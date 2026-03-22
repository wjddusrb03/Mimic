package com.wjddusrb03.mimic.listener;

import com.wjddusrb03.mimic.Mimic;
import com.wjddusrb03.mimic.mimic.MimicType;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 청크 로드 시 자연 미믹 생성을 처리한다.
 */
public class ChunkListener implements Listener {

    private final Mimic plugin;
    private final Random random = new Random();

    public ChunkListener(Mimic plugin) {
        this.plugin = plugin;
    }

    /**
     * 새 청크가 처음 생성될 때 미믹 배치를 시도한다.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent e) {
        if (!e.isNewChunk()) return;

        Chunk chunk = e.getChunk();
        World world = chunk.getWorld();

        // 비활성화된 월드 체크
        List<String> disabledWorlds = plugin.getConfig().getStringList("disabled-worlds");
        if (disabledWorlds.contains(world.getName())) return;

        // 확률 체크
        double chunkChance = plugin.getConfig().getDouble("spawn.chunk-chance", 0.05);
        if (random.nextDouble() >= chunkChance) return;

        // 비동기 청크 데이터 접근 방지 - 1틱 후 메인 스레드에서 실행
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            tryPlaceMimic(chunk, world);
        }, 1L);
    }

    /**
     * 청크 내 적절한 위치에 미믹을 배치한다.
     */
    private void tryPlaceMimic(Chunk chunk, World world) {
        int maxLightLevel = plugin.getConfig().getInt("spawn.max-light-level", 7);

        // 청크 내 랜덤 위치 탐색 (최대 10회 시도)
        for (int attempt = 0; attempt < 10; attempt++) {
            int localX = random.nextInt(16);
            int localZ = random.nextInt(16);
            int worldX = chunk.getX() * 16 + localX;
            int worldZ = chunk.getZ() * 16 + localZ;

            // 가장 높은 블록 찾기
            int highY = world.getHighestBlockYAt(worldX, worldZ);
            if (highY < world.getMinHeight() + 5) continue;

            // 지하 위치 탐색 (표면이 아닌 동굴 등)
            int targetY = findSuitableY(world, worldX, highY, worldZ, maxLightLevel);
            if (targetY < 0) continue;

            Block targetBlock = world.getBlockAt(worldX, targetY, worldZ);

            // 바닥이 고체 블록인지 확인
            Block below = world.getBlockAt(worldX, targetY - 1, worldZ);
            if (!below.getType().isSolid()) continue;

            // 위에 공간이 있는지 확인
            Block above = world.getBlockAt(worldX, targetY + 1, worldZ);
            if (above.getType().isSolid()) continue;

            // 레벨 결정 (스폰 지점 거리 기반)
            int level = determineLevel(world, worldX, worldZ);

            // 레벨에 맞는 미믹 타입 선택
            MimicType type = selectMimicType(level);
            if (type == null) continue;

            // 미믹 생성
            plugin.getMimicManager().spawnMimic(type, level, world, worldX, targetY, worldZ);
            return; // 한 청크에 하나만
        }
    }

    /**
     * 미믹을 배치하기 적절한 Y좌표를 찾는다.
     */
    private int findSuitableY(World world, int x, int highY, int z, int maxLightLevel) {
        // 지하에서 위로 탐색
        for (int y = world.getMinHeight() + 5; y < highY; y++) {
            Block block = world.getBlockAt(x, y, z);

            // 빈 공간이면서 빛 레벨이 낮은 곳
            if (block.getType() == Material.AIR || block.getType() == Material.CAVE_AIR) {
                if (block.getLightLevel() <= maxLightLevel) {
                    Block below = world.getBlockAt(x, y - 1, z);
                    if (below.getType().isSolid()) {
                        return y;
                    }
                }
            }
        }
        return -1;
    }

    /**
     * 스폰 지점 거리 기반으로 미믹 레벨을 결정한다.
     */
    private int determineLevel(World world, int x, int z) {
        // 스폰 지점에서의 거리 계산
        double distance = Math.sqrt(x * x + z * z);

        int lv4Dist = plugin.getConfig().getInt("level.distance-thresholds.lv4", 3000);
        int lv3Dist = plugin.getConfig().getInt("level.distance-thresholds.lv3", 1500);
        int lv2Dist = plugin.getConfig().getInt("level.distance-thresholds.lv2", 500);

        if (distance >= lv4Dist) {
            // Lv.4~5 확률: 70% lv4, 30% lv5
            return random.nextFloat() < 0.7f ? 4 : 5;
        } else if (distance >= lv3Dist) {
            return 3;
        } else if (distance >= lv2Dist) {
            return 2;
        }
        return 1;
    }

    /**
     * 레벨에 적합한 미믹 타입을 랜덤 선택한다.
     */
    private MimicType selectMimicType(int level) {
        List<MimicType> candidates = new ArrayList<>();
        for (MimicType type : MimicType.values()) {
            if (type.getMinLevel() <= level) {
                candidates.add(type);
            }
        }
        if (candidates.isEmpty()) return null;
        return candidates.get(random.nextInt(candidates.size()));
    }
}
