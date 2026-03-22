package com.wjddusrb03.mimic;

import com.wjddusrb03.mimic.command.MimicCommand;
import com.wjddusrb03.mimic.command.MimicTabCompleter;
import com.wjddusrb03.mimic.detection.DetectorManager;
import com.wjddusrb03.mimic.listener.ChunkListener;
import com.wjddusrb03.mimic.listener.CombatListener;
import com.wjddusrb03.mimic.listener.InteractListener;
import com.wjddusrb03.mimic.loot.LootManager;
import com.wjddusrb03.mimic.loot.MimicItem;
import com.wjddusrb03.mimic.mimic.MimicCombat;
import com.wjddusrb03.mimic.mimic.MimicManager;
import com.wjddusrb03.mimic.spawn.NaturalSpawner;
import com.wjddusrb03.mimic.stats.StatsManager;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class Mimic extends JavaPlugin {

    private static Mimic instance;

    private MimicManager mimicManager;
    private MimicCombat mimicCombat;
    private LootManager lootManager;
    private StatsManager statsManager;
    private NaturalSpawner naturalSpawner;
    private DetectorManager detectorManager;

    // PDC 키들
    private NamespacedKey mimicKey;
    private NamespacedKey mimicLevelKey;
    private NamespacedKey mimicMiniKey;
    private NamespacedKey mimicSplitKey;

    @Override
    public void onEnable() {
        instance = this;

        // 1. 설정 파일
        saveDefaultConfig();

        // 2. PDC 키 초기화
        mimicKey = new NamespacedKey(this, "mimic_id");
        mimicLevelKey = new NamespacedKey(this, "mimic_level");
        mimicMiniKey = new NamespacedKey(this, "mimic_mini");
        mimicSplitKey = new NamespacedKey(this, "mimic_split");

        // 3. 매니저 초기화
        mimicManager = new MimicManager(this);
        mimicCombat = new MimicCombat(this);
        lootManager = new LootManager(this);
        statsManager = new StatsManager(this);
        naturalSpawner = new NaturalSpawner(this);
        detectorManager = new DetectorManager(this);

        // 4. 데이터 로드
        mimicManager.loadData();
        statsManager.loadData();

        // 5. 매니저 시작
        mimicManager.start();
        detectorManager.start();

        // 6. 커스텀 레시피 등록
        MimicItem.registerRecipes(this);

        // 7. 이벤트 리스너 등록
        getServer().getPluginManager().registerEvents(new InteractListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new ChunkListener(this), this);

        // 8. 커맨드 등록
        MimicCommand cmd = new MimicCommand(this);
        MimicTabCompleter tab = new MimicTabCompleter(this);
        if (getCommand("mimic") != null) {
            getCommand("mimic").setExecutor(cmd);
            getCommand("mimic").setTabCompleter(tab);
        }

        getLogger().info("Mimic v" + getDescription().getVersion() + " 활성화 완료.");
    }

    @Override
    public void onDisable() {
        // 데이터 저장
        if (mimicManager != null) {
            mimicManager.saveData();
            mimicManager.cleanup();
        }
        if (statsManager != null) {
            statsManager.saveData();
        }
        if (detectorManager != null) {
            detectorManager.stop();
        }

        getLogger().info("Mimic 비활성화 완료.");
    }

    // ─── Getters ───

    public static Mimic getInstance() { return instance; }
    public MimicManager getMimicManager() { return mimicManager; }
    public MimicCombat getMimicCombat() { return mimicCombat; }
    public LootManager getLootManager() { return lootManager; }
    public StatsManager getStatsManager() { return statsManager; }
    public NaturalSpawner getNaturalSpawner() { return naturalSpawner; }
    public DetectorManager getDetectorManager() { return detectorManager; }

    public NamespacedKey getMimicKey() { return mimicKey; }
    public NamespacedKey getMimicLevelKey() { return mimicLevelKey; }
    public NamespacedKey getMimicMiniKey() { return mimicMiniKey; }
    public NamespacedKey getMimicSplitKey() { return mimicSplitKey; }
}
