package com.wjddusrb03.mimic.listener;

import com.wjddusrb03.mimic.Mimic;
import com.wjddusrb03.mimic.mimic.MimicEntity;
import com.wjddusrb03.mimic.mimic.MimicState;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Husk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * 미믹 전투 이벤트를 처리한다.
 */
public class CombatListener implements Listener {

    private final Mimic plugin;

    public CombatListener(Mimic plugin) {
        this.plugin = plugin;
    }

    /**
     * 미믹 Husk가 플레이어를 공격하거나, 플레이어가 미믹 Husk를 공격할 때 처리한다.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        // 미믹이 플레이어를 공격한 경우
        if (e.getDamager() instanceof Husk husk && e.getEntity() instanceof Player victim) {
            handleMimicAttackPlayer(e, husk, victim);
            return;
        }

        // 플레이어가 미믹을 공격한 경우
        if (e.getDamager() instanceof Player player && e.getEntity() instanceof Husk husk) {
            handlePlayerAttackMimic(e, player, husk);
        }
    }

    /**
     * 미믹 Husk가 플레이어를 공격할 때.
     */
    private void handleMimicAttackPlayer(EntityDamageByEntityEvent e, Husk husk, Player victim) {
        PersistentDataContainer pdc = husk.getPersistentDataContainer();
        if (!pdc.has(plugin.getMimicKey())) return;

        int mimicId = pdc.getOrDefault(plugin.getMimicKey(), PersistentDataType.INTEGER, -1);
        MimicEntity mimic = plugin.getMimicManager().getMimicByMobUuid(husk.getUniqueId());
        if (mimic == null) return;
        if (mimic.getState() != MimicState.ACTIVE) return;

        double damage = e.getDamage();

        // 기습 배율 적용 (미믹의 첫 번째 공격, 선제 공격이 아닌 경우)
        if (!mimic.isPreemptiveStrike() && mimic.getLastDamagedTime() == 0) {
            // 아직 한 번도 피해를 입지 않았다면 첫 공격
            double ambushMultiplier = plugin.getConfig().getDouble("combat.ambush-multiplier", 1.5);
            damage *= ambushMultiplier;
            e.setDamage(damage);
        }

        // MimicCombat 특수 능력 처리
        plugin.getMimicCombat().onMimicAttack(mimic, victim, damage);
    }

    /**
     * 플레이어가 미믹 Husk를 공격할 때.
     */
    private void handlePlayerAttackMimic(EntityDamageByEntityEvent e, Player player, Husk husk) {
        PersistentDataContainer pdc = husk.getPersistentDataContainer();

        // 미니 미믹은 별도 처리 없음 (일반 데미지 적용)
        if (pdc.has(plugin.getMimicMiniKey())) return;

        if (!pdc.has(plugin.getMimicKey())) return;

        MimicEntity mimic = plugin.getMimicManager().getMimicByMobUuid(husk.getUniqueId());
        if (mimic == null) return;
        if (mimic.getState() != MimicState.ACTIVE) return;

        double damage = e.getDamage();

        // MimicCombat 피격 처리 (분열 등)
        plugin.getMimicCombat().onMimicDamaged(mimic, damage);
    }

    /**
     * 미믹 Husk 사망 시 처리.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent e) {
        if (!(e.getEntity() instanceof Husk husk)) return;

        PersistentDataContainer pdc = husk.getPersistentDataContainer();

        // 미니 미믹 사망 처리
        if (pdc.has(plugin.getMimicMiniKey())) {
            handleMiniMimicDeath(e, husk);
            return;
        }

        // 일반 미믹 사망 처리
        if (!pdc.has(plugin.getMimicKey())) return;

        MimicEntity mimic = plugin.getMimicManager().getMimicByMobUuid(husk.getUniqueId());
        if (mimic == null) return;

        // 기본 드롭 취소
        e.getDrops().clear();
        e.setDroppedExp(0);

        // 킬러 확인
        Player killer = husk.getKiller();

        // MimicCombat 사망 처리 (파티클, 메시지)
        plugin.getMimicCombat().onMimicDeath(mimic, killer);

        // 전리품 드롭
        Location deathLoc = husk.getLocation();
        plugin.getLootManager().dropLoot(mimic, deathLoc, killer);

        // 경험치 지급
        if (killer != null) {
            int exp = plugin.getConfig().getInt("experience.lv" + mimic.getLevel(), 15);
            killer.giveExp(exp);

            // 통계 기록
            plugin.getStatsManager().addKill(killer.getUniqueId(), mimic);
        }
    }

    /**
     * 미니 미믹 사망 시: 작은 파티클만, 전리품 없음.
     */
    private void handleMiniMimicDeath(EntityDeathEvent e, Husk husk) {
        // 기본 드롭 취소
        e.getDrops().clear();
        e.setDroppedExp(0);

        // 작은 파티클
        Location loc = husk.getLocation();
        loc.getWorld().spawnParticle(Particle.SMOKE, loc.clone().add(0, 0.3, 0),
                10, 0.2, 0.2, 0.2, 0.02);
        loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 0.3, 0),
                5, 0.2, 0.2, 0.2, 0,
                new Particle.DustOptions(Color.fromRGB(80, 0, 80), 0.8f));
        loc.getWorld().playSound(loc, Sound.ENTITY_SLIME_DEATH, 0.6f, 1.5f);
    }
}
