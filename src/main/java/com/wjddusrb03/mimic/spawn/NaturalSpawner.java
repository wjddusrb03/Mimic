package com.wjddusrb03.mimic.spawn;

import com.wjddusrb03.mimic.Mimic;
import com.wjddusrb03.mimic.mimic.MimicManager;
import com.wjddusrb03.mimic.mimic.MimicType;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 새로 생성된 청크에서 자연 미믹 스폰을 처리한다.
 */
public class NaturalSpawner {

    private final Mimic plugin;
    private final Random random = new Random();

    public NaturalSpawner(Mimic plugin) {
        this.plugin = plugin;
    }

    /**
     * 청크 내에서 미믹 스폰을 시도한다.
     *
     * @param chunk 새로 로드/생성된 청크
     */
    public void trySpawnInChunk(Chunk chunk) {
        double chunkChance = plugin.getConfig().getDouble("spawn.chunk-chance", 0.05);
        if (random.nextDouble() >= chunkChance) return;

        int maxPerChunk = plugin.getConfig().getInt("spawn.max-per-chunk", 2);
        int maxLightLevel = plugin.getConfig().getInt("spawn.max-light-level", 7);

        MimicManager mimicManager = plugin.getMimicManager();

        // 청크 내 적합한 블록 스캔
        List<BlockCandidate> candidates = new ArrayList<>();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = chunk.getWorld().getMinHeight(); y < chunk.getWorld().getMaxHeight(); y++) {
                    Block block = chunk.getBlock(x, y, z);
                    Material mat = block.getType();

                    // MimicType에 해당하는 블록인지 확인
                    MimicType type = MimicType.fromMaterial(mat);
                    if (type == null) continue;

                    // 빛 레벨 확인
                    if (block.getLightLevel() > maxLightLevel) continue;

                    candidates.add(new BlockCandidate(block, type));
                }
            }
        }

        if (candidates.isEmpty()) return;

        // 최대 max-per-chunk개까지 랜덤 선택
        int spawnCount = Math.min(candidates.size(), maxPerChunk);
        java.util.Collections.shuffle(candidates, random);

        int spawned = 0;
        for (int i = 0; i < candidates.size() && spawned < spawnCount; i++) {
            BlockCandidate candidate = candidates.get(i);
            Block block = candidate.block;
            MimicType type = candidate.type;

            Location loc = block.getLocation();
            int level = calculateLevel(loc);

            // 해당 타입의 최소 레벨 확인
            if (level < type.getMinLevel()) continue;

            // 미믹 생성 (MimicManager가 청크/월드 제한을 처리)
            var mimic = mimicManager.spawnMimic(type, level,
                    block.getWorld(), block.getX(), block.getY(), block.getZ());
            if (mimic != null) {
                spawned++;
            }
        }
    }

    /**
     * 위치에서 월드 스폰으로부터의 거리를 기반으로 레벨을 결정한다.
     */
    public int calculateLevel(Location loc) {
        World world = loc.getWorld();
        Location spawn = world.getSpawnLocation();

        double distance = loc.distance(spawn);

        int lv4Threshold = plugin.getConfig().getInt("level.distance-thresholds.lv4", 3000);
        int lv3Threshold = plugin.getConfig().getInt("level.distance-thresholds.lv3", 1500);
        int lv2Threshold = plugin.getConfig().getInt("level.distance-thresholds.lv2", 500);

        if (distance >= lv4Threshold) {
            // Lv.4~5 (먼 거리에서 낮은 확률로 Lv.5)
            return random.nextDouble() < 0.15 ? 5 : 4;
        } else if (distance >= lv3Threshold) {
            return 3;
        } else if (distance >= lv2Threshold) {
            return 2;
        } else {
            return 1;
        }
    }

    /**
     * 청크 스캔 후보 블록 정보.
     */
    private static class BlockCandidate {
        final Block block;
        final MimicType type;

        BlockCandidate(Block block, MimicType type) {
            this.block = block;
            this.type = type;
        }
    }
}
