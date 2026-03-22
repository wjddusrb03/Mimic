package com.wjddusrb03.mimic.detection;

import com.wjddusrb03.mimic.Mimic;
import com.wjddusrb03.mimic.loot.MimicItem;
import com.wjddusrb03.mimic.mimic.MimicEntity;
import com.wjddusrb03.mimic.mimic.MimicState;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

/**
 * 미믹 감지기와 미믹 갑옷의 효과를 처리한다.
 */
public class DetectorManager {

    private final Mimic plugin;
    private BukkitTask detectorTask;

    public DetectorManager(Mimic plugin) {
        this.plugin = plugin;
    }

    /**
     * 감지 태스크를 시작한다.
     */
    public void start() {
        detectorTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    checkDetector(player);
                    checkArmor(player);
                }
            }
        }.runTaskTimer(plugin, 10L, 10L);
    }

    /**
     * 감지 태스크를 중지한다.
     */
    public void stop() {
        if (detectorTask != null) {
            detectorTask.cancel();
            detectorTask = null;
        }
    }

    /**
     * 미믹 감지기를 들고 있는 플레이어 주변 DORMANT 미믹에 파티클 표시.
     */
    private void checkDetector(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        if (!MimicItem.isDetector(mainHand) && !MimicItem.isDetector(offHand)) return;

        double range = plugin.getConfig().getDouble("detection.detector-range", 8);
        List<MimicEntity> nearbyMimics = plugin.getMimicManager()
                .getMimicsInRadius(player.getLocation(), range);

        for (MimicEntity mimic : nearbyMimics) {
            if (mimic.getState() != MimicState.DORMANT && mimic.getState() != MimicState.ALERTING) continue;

            Location mimicLoc = mimic.getBlockCenterLocation();

            // 빨간 DUST 파티클을 미믹 위치에 표시 (해당 플레이어에게만)
            player.spawnParticle(Particle.DUST, mimicLoc,
                    5, 0.3, 0.3, 0.3, 0,
                    new Particle.DustOptions(Color.RED, 1.0f));
        }
    }

    /**
     * 미믹 갑옷을 착용한 플레이어에게 5블록 내 미믹 접근 시 경고음 재생.
     */
    private void checkArmor(Player player) {
        ItemStack chestplate = player.getInventory().getChestplate();
        if (!MimicItem.isMimicArmor(chestplate)) return;

        double warningRange = 5.0;
        List<MimicEntity> nearbyMimics = plugin.getMimicManager()
                .getMimicsInRadius(player.getLocation(), warningRange);

        boolean mimicNearby = false;
        for (MimicEntity mimic : nearbyMimics) {
            if (mimic.getState() == MimicState.DORMANT || mimic.getState() == MimicState.ALERTING) {
                mimicNearby = true;
                break;
            }
        }

        if (mimicNearby) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.3f, 0.5f);
        }
    }
}
