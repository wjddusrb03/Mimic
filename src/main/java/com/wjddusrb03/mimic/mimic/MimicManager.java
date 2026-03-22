package com.wjddusrb03.mimic.mimic;

import com.wjddusrb03.mimic.Mimic;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 모든 미믹 엔티티를 관리한다.
 * 틱 루프로 힌트 파티클, 재위장, 사운드 등을 처리한다.
 */
public class MimicManager {

    private final Mimic plugin;
    private final Map<Integer, MimicEntity> mimics = new ConcurrentHashMap<>();
    private BukkitTask tickTask;
    private BukkitTask hintTask;

    public MimicManager(Mimic plugin) {
        this.plugin = plugin;
    }

    /**
     * 매니저를 시작한다.
     */
    public void start() {
        // 메인 틱 루프 (1초마다 - 재위장, 상태 관리)
        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                tickMimics();
            }
        }.runTaskTimer(plugin, 20L, 20L);

        // 힌트 파티클 루프 (2초마다)
        hintTask = new BukkitRunnable() {
            @Override
            public void run() {
                tickHints();
            }
        }.runTaskTimer(plugin, 40L, 40L);
    }

    /**
     * 미믹을 생성하고 블록을 배치한다.
     */
    public MimicEntity spawnMimic(MimicType type, int level, World world, int x, int y, int z) {
        // 청크당 제한 체크
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        int maxPerChunk = plugin.getConfig().getInt("spawn.max-per-chunk", 2);
        long countInChunk = mimics.values().stream()
                .filter(m -> m.getWorld().equals(world)
                        && (m.getBlockX() >> 4) == chunkX
                        && (m.getBlockZ() >> 4) == chunkZ
                        && m.getState() != MimicState.DEAD)
                .count();

        if (countInChunk >= maxPerChunk) return null;

        // 월드당 제한 체크
        int maxPerWorld = plugin.getConfig().getInt("spawn.max-per-world", 200);
        long countInWorld = mimics.values().stream()
                .filter(m -> m.getWorld().equals(world) && m.getState() != MimicState.DEAD)
                .count();
        if (countInWorld >= maxPerWorld) return null;

        MimicEntity mimic = new MimicEntity(type, level, world, x, y, z);
        mimic.placeBlock();
        mimics.put(mimic.getId(), mimic);
        return mimic;
    }

    /**
     * 위치로 미믹을 찾는다.
     */
    public MimicEntity getMimicAt(World world, int x, int y, int z) {
        for (MimicEntity mimic : mimics.values()) {
            if (mimic.isAt(world, x, y, z) &&
                    (mimic.getState() == MimicState.DORMANT || mimic.getState() == MimicState.ALERTING)) {
                return mimic;
            }
        }
        return null;
    }

    /**
     * 엔티티 ID로 미믹을 찾는다.
     */
    public MimicEntity getMimicByEntityId(int mimicId) {
        return mimics.get(mimicId);
    }

    /**
     * 활성 몹 UUID로 미믹을 찾는다.
     */
    public MimicEntity getMimicByMobUuid(UUID mobUuid) {
        for (MimicEntity mimic : mimics.values()) {
            if (mimic.getMobEntity() != null && mimic.getMobEntity().getUniqueId().equals(mobUuid)) {
                return mimic;
            }
        }
        return null;
    }

    /**
     * 미믹을 제거한다.
     */
    public void removeMimic(int id) {
        MimicEntity mimic = mimics.remove(id);
        if (mimic != null) {
            mimic.remove();
        }
    }

    /**
     * 반경 내 모든 미믹을 제거한다.
     */
    public int removeInRadius(Location center, double radius) {
        int count = 0;
        Iterator<Map.Entry<Integer, MimicEntity>> it = mimics.entrySet().iterator();
        while (it.hasNext()) {
            MimicEntity mimic = it.next().getValue();
            if (mimic.getWorld().equals(center.getWorld())) {
                double dist = mimic.getBlockCenterLocation().distance(center);
                if (dist <= radius) {
                    mimic.remove();
                    it.remove();
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * 반경 내 미믹 목록을 반환한다.
     */
    public List<MimicEntity> getMimicsInRadius(Location center, double radius) {
        List<MimicEntity> result = new ArrayList<>();
        for (MimicEntity mimic : mimics.values()) {
            if (mimic.getWorld().equals(center.getWorld()) && mimic.getState() != MimicState.DEAD) {
                double dist = mimic.getBlockCenterLocation().distance(center);
                if (dist <= radius) {
                    result.add(mimic);
                }
            }
        }
        return result;
    }

    /**
     * 메인 틱 - 재위장, 상태 정리 등.
     */
    private void tickMimics() {
        long now = System.currentTimeMillis();
        int reDisguiseMs = plugin.getConfig().getInt("combat.re-disguise-seconds", 8) * 1000;

        Iterator<Map.Entry<Integer, MimicEntity>> it = mimics.entrySet().iterator();
        while (it.hasNext()) {
            MimicEntity mimic = it.next().getValue();

            // DEAD 상태 제거
            if (mimic.getState() == MimicState.DEAD) {
                it.remove();
                continue;
            }

            // ACTIVE 몹이 죽었으면 DEAD로
            if (mimic.getState() == MimicState.ACTIVE && mimic.getMobEntity() != null
                    && mimic.getMobEntity().isDead()) {
                mimic.setState(MimicState.DEAD);
                it.remove();
                continue;
            }

            // Lv.4+ 재위장 체크
            if (mimic.getState() == MimicState.ACTIVE && mimic.getLevel() >= 4) {
                if (now - mimic.getLastDamagedTime() > reDisguiseMs) {
                    // 근처에 플레이어가 없으면 재위장
                    boolean playerNearby = false;
                    if (mimic.getMobEntity() != null) {
                        for (Player p : mimic.getWorld().getPlayers()) {
                            if (p.getLocation().distance(mimic.getMobEntity().getLocation()) < 16) {
                                playerNearby = true;
                                break;
                            }
                        }
                    }
                    if (!playerNearby) {
                        mimic.reDisguise(plugin);
                    }
                }
            }
        }
    }

    /**
     * 힌트 파티클/사운드를 표시한다.
     */
    private void tickHints() {
        int hintRange = plugin.getConfig().getInt("detection.hint-range", 8);
        int soundRange = plugin.getConfig().getInt("detection.sound-hint-range", 3);

        for (MimicEntity mimic : mimics.values()) {
            if (mimic.getState() != MimicState.DORMANT && mimic.getState() != MimicState.ALERTING) continue;

            Location mimicLoc = mimic.getBlockCenterLocation();
            boolean anyoneNearby = false;

            for (Player player : mimic.getWorld().getPlayers()) {
                double dist = player.getLocation().distance(mimicLoc);

                if (dist <= hintRange) {
                    anyoneNearby = true;

                    // 레벨에 따른 힌트 표시
                    showHintForLevel(mimic, player, dist, soundRange);
                }
            }

            // 상태 전환
            if (anyoneNearby && mimic.getState() == MimicState.DORMANT) {
                mimic.setState(MimicState.ALERTING);
            } else if (!anyoneNearby && mimic.getState() == MimicState.ALERTING) {
                mimic.setState(MimicState.DORMANT);
            }
        }
    }

    /**
     * 미믹 레벨에 따른 힌트를 표시한다.
     */
    private void showHintForLevel(MimicEntity mimic, Player player, double distance, int soundRange) {
        Location loc = mimic.getBlockCenterLocation();
        Random random = new Random();

        switch (mimic.getLevel()) {
            case 1 -> {
                // Lv.1: 주기적으로 보라색 파티클
                if (random.nextFloat() < 0.6f) {
                    player.spawnParticle(Particle.DUST, loc.clone().add(
                                    random.nextDouble() - 0.5, random.nextDouble(), random.nextDouble() - 0.5),
                            2, 0, 0, 0, 0,
                            new Particle.DustOptions(Color.fromRGB(128, 0, 200), 0.7f));
                }
            }
            case 2 -> {
                // Lv.2: 가끔 파티클 + 가까우면 진동
                if (random.nextFloat() < 0.3f) {
                    player.spawnParticle(Particle.DUST, loc.clone().add(
                                    random.nextDouble() * 0.6 - 0.3, random.nextDouble() * 0.5, random.nextDouble() * 0.6 - 0.3),
                            1, 0, 0, 0, 0,
                            new Particle.DustOptions(Color.fromRGB(80, 0, 120), 0.5f));
                }
                if (distance <= soundRange && random.nextFloat() < 0.15f) {
                    player.playSound(loc, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.15f, 0.5f);
                }
            }
            case 3 -> {
                // Lv.3: 아주 드물게 눈 번쩍임
                if (random.nextFloat() < 0.08f) {
                    player.spawnParticle(Particle.DUST, loc.clone().add(0.3, 0.6, 0.3),
                            1, 0, 0, 0, 0,
                            new Particle.DustOptions(Color.RED, 0.4f));
                }
                if (distance <= soundRange && random.nextFloat() < 0.05f) {
                    player.playSound(loc, Sound.ENTITY_WARDEN_HEARTBEAT, 0.1f, 1.5f);
                }
            }
            // Lv.4+: 힌트 없음 (감지기 아이템 필요)
        }
    }

    /**
     * 미믹 데이터를 파일에 저장한다.
     */
    public void saveData() {
        File file = new File(plugin.getDataFolder(), "data/mimics.yml");
        file.getParentFile().mkdirs();

        FileConfiguration data = new YamlConfiguration();

        int index = 0;
        for (MimicEntity mimic : mimics.values()) {
            if (mimic.getState() == MimicState.DEAD) continue;
            // ACTIVE 몹은 저장하지 않음 (재시작 시 사라짐)
            if (mimic.getState() == MimicState.ACTIVE) continue;

            String path = "mimics." + index;
            data.set(path + ".type", mimic.getType().name());
            data.set(path + ".level", mimic.getLevel());
            data.set(path + ".world", mimic.getWorld().getName());
            data.set(path + ".x", mimic.getBlockX());
            data.set(path + ".y", mimic.getBlockY());
            data.set(path + ".z", mimic.getBlockZ());
            index++;
        }
        data.set("count", index);

        try {
            data.save(file);
            plugin.getLogger().info("미믹 " + index + "개 데이터 저장 완료.");
        } catch (IOException e) {
            plugin.getLogger().severe("미믹 데이터 저장 실패: " + e.getMessage());
        }
    }

    /**
     * 미믹 데이터를 파일에서 로드한다.
     */
    public void loadData() {
        File file = new File(plugin.getDataFolder(), "data/mimics.yml");
        if (!file.exists()) return;

        FileConfiguration data = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = data.getConfigurationSection("mimics");
        if (section == null) return;

        int loaded = 0;
        for (String key : section.getKeys(false)) {
            String path = "mimics." + key;
            String typeName = data.getString(path + ".type");
            int level = data.getInt(path + ".level", 1);
            String worldName = data.getString(path + ".world");
            int x = data.getInt(path + ".x");
            int y = data.getInt(path + ".y");
            int z = data.getInt(path + ".z");

            MimicType type;
            try {
                type = MimicType.valueOf(typeName);
            } catch (Exception e) {
                continue;
            }

            World world = Bukkit.getWorld(worldName);
            if (world == null) continue;

            // 블록이 여전히 미믹 타입과 일치하는지 확인
            Block block = world.getBlockAt(x, y, z);
            if (block.getType() != type.getDisguiseBlock() && block.getType() != Material.AIR) {
                continue; // 다른 블록으로 바뀌었으면 스킵
            }

            MimicEntity mimic = new MimicEntity(type, level, world, x, y, z);
            if (block.getType() != type.getDisguiseBlock()) {
                mimic.placeBlock();
            }
            mimics.put(mimic.getId(), mimic);
            loaded++;
        }

        plugin.getLogger().info("미믹 " + loaded + "개 데이터 로드 완료.");
    }

    /**
     * 모든 미믹을 정리한다.
     */
    public void cleanup() {
        for (MimicEntity mimic : mimics.values()) {
            if (mimic.getMobEntity() != null && !mimic.getMobEntity().isDead()) {
                mimic.getMobEntity().remove();
            }
        }
        mimics.clear();

        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        if (hintTask != null) {
            hintTask.cancel();
            hintTask = null;
        }
    }

    public int getMimicCount() {
        return (int) mimics.values().stream()
                .filter(m -> m.getState() != MimicState.DEAD)
                .count();
    }

    public Collection<MimicEntity> getAllMimics() {
        return Collections.unmodifiableCollection(mimics.values());
    }
}
