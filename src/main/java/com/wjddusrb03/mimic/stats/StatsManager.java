package com.wjddusrb03.mimic.stats;

import com.wjddusrb03.mimic.Mimic;
import com.wjddusrb03.mimic.mimic.MimicEntity;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 플레이어별 미믹 처치 통계를 관리한다.
 */
public class StatsManager {

    private final Mimic plugin;
    private final Map<UUID, PlayerStats> statsMap = new ConcurrentHashMap<>();

    public StatsManager(Mimic plugin) {
        this.plugin = plugin;
    }

    /**
     * 킬을 기록한다.
     */
    public void addKill(UUID playerUuid, MimicEntity mimic) {
        PlayerStats stats = statsMap.computeIfAbsent(playerUuid, k -> new PlayerStats());
        stats.addKill(mimic.getLevel(), mimic.getType().name());
    }

    /**
     * 총 킬 수를 반환한다.
     */
    public int getKills(UUID playerUuid) {
        PlayerStats stats = statsMap.get(playerUuid);
        return stats != null ? stats.getTotalKills() : 0;
    }

    /**
     * 특정 레벨 미믹 킬 수를 반환한다.
     */
    public int getKillsByLevel(UUID playerUuid, int level) {
        PlayerStats stats = statsMap.get(playerUuid);
        return stats != null ? stats.getKillsByLevel(level) : 0;
    }

    /**
     * 상위 킬러 목록을 반환한다.
     */
    public List<Map.Entry<UUID, Integer>> getTopKillers(int limit) {
        return statsMap.entrySet().stream()
                .map(e -> Map.entry(e.getKey(), e.getValue().getTotalKills()))
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 통계를 파일에 저장한다.
     */
    public void saveData() {
        File file = new File(plugin.getDataFolder(), "data/stats.yml");
        file.getParentFile().mkdirs();

        FileConfiguration data = new YamlConfiguration();

        for (Map.Entry<UUID, PlayerStats> entry : statsMap.entrySet()) {
            String uuid = entry.getKey().toString();
            PlayerStats stats = entry.getValue();

            data.set("players." + uuid + ".total", stats.getTotalKills());
            for (int lv = 1; lv <= 5; lv++) {
                int kills = stats.getKillsByLevel(lv);
                if (kills > 0) {
                    data.set("players." + uuid + ".levels.lv" + lv, kills);
                }
            }
            for (Map.Entry<String, Integer> typeEntry : stats.getKillsByType().entrySet()) {
                if (typeEntry.getValue() > 0) {
                    data.set("players." + uuid + ".types." + typeEntry.getKey(), typeEntry.getValue());
                }
            }
        }

        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("통계 데이터 저장 실패: " + e.getMessage());
        }
    }

    /**
     * 통계를 파일에서 로드한다.
     */
    public void loadData() {
        File file = new File(plugin.getDataFolder(), "data/stats.yml");
        if (!file.exists()) return;

        FileConfiguration data = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection playersSection = data.getConfigurationSection("players");
        if (playersSection == null) return;

        for (String uuidStr : playersSection.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                continue;
            }

            PlayerStats stats = new PlayerStats();
            String basePath = "players." + uuidStr;

            // 레벨별 킬 로드
            ConfigurationSection levelsSection = data.getConfigurationSection(basePath + ".levels");
            if (levelsSection != null) {
                for (String lvKey : levelsSection.getKeys(false)) {
                    try {
                        int lv = Integer.parseInt(lvKey.replace("lv", ""));
                        int kills = levelsSection.getInt(lvKey, 0);
                        stats.setKillsByLevel(lv, kills);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            // 타입별 킬 로드
            ConfigurationSection typesSection = data.getConfigurationSection(basePath + ".types");
            if (typesSection != null) {
                for (String typeKey : typesSection.getKeys(false)) {
                    int kills = typesSection.getInt(typeKey, 0);
                    stats.setKillsByType(typeKey, kills);
                }
            }

            // 총 킬은 레벨별 합산으로 재계산
            stats.recalculateTotal();

            statsMap.put(uuid, stats);
        }

        plugin.getLogger().info("플레이어 " + statsMap.size() + "명의 통계 데이터 로드 완료.");
    }

    /**
     * 플레이어별 통계 데이터.
     */
    public static class PlayerStats {

        private int totalKills;
        private final int[] killsByLevel = new int[6]; // index 1~5
        private final Map<String, Integer> killsByType = new HashMap<>();

        public void addKill(int level, String typeName) {
            totalKills++;
            if (level >= 1 && level <= 5) {
                killsByLevel[level]++;
            }
            killsByType.merge(typeName, 1, Integer::sum);
        }

        public int getTotalKills() { return totalKills; }

        public int getKillsByLevel(int level) {
            if (level < 1 || level > 5) return 0;
            return killsByLevel[level];
        }

        public Map<String, Integer> getKillsByType() { return killsByType; }

        public void setKillsByLevel(int level, int kills) {
            if (level >= 1 && level <= 5) {
                killsByLevel[level] = kills;
            }
        }

        public void setKillsByType(String typeName, int kills) {
            killsByType.put(typeName, kills);
        }

        public void recalculateTotal() {
            totalKills = 0;
            for (int lv = 1; lv <= 5; lv++) {
                totalKills += killsByLevel[lv];
            }
        }
    }
}
