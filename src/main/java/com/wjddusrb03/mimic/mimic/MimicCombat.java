package com.wjddusrb03.mimic.mimic;

import com.wjddusrb03.mimic.Mimic;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Random;

/**
 * 미믹 전투 AI와 특수 능력을 처리한다.
 */
public class MimicCombat {

    private final Mimic plugin;
    private final Random random = new Random();

    public MimicCombat(Mimic plugin) {
        this.plugin = plugin;
    }

    /**
     * 미믹이 플레이어를 공격할 때 호출된다.
     * 특수 효과와 능력을 적용한다.
     */
    public void onMimicAttack(MimicEntity mimic, Player victim, double damage) {
        if (mimic.getMobEntity() == null || mimic.getMobEntity().isDead()) return;

        Location mobLoc = mimic.getMobEntity().getLocation();

        // 공격 사운드
        victim.getWorld().playSound(mobLoc, mimic.getType().getAttackSound(), 0.8f, 1.0f);

        // === 유형별 특수 공격 ===
        switch (mimic.getType()) {
            case CHEST, BARREL -> {
                // 물어뜯기 파티클
                victim.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR,
                        victim.getLocation().add(0, 1, 0), 5, 0.3, 0.3, 0.3, 0);
            }
            case CRAFTING_TABLE -> {
                // 톱날 투사체 효과 (파티클)
                if (random.nextFloat() < 0.3f) {
                    spawnToolProjectile(mimic, victim);
                }
            }
            case FURNACE -> {
                // 화염 브레스
                if (random.nextFloat() < 0.25f) {
                    fireBreath(mimic, victim);
                }
            }
            case ANVIL -> {
                // 내리찍기 (넉백 + 추가 데미지)
                if (random.nextFloat() < 0.2f) {
                    anvilSlam(mimic, victim);
                }
            }
            case ENCHANTING_TABLE -> {
                // 마법 구체 + 레비테이션
                if (random.nextFloat() < 0.3f) {
                    magicBlast(mimic, victim);
                }
            }
            case SHULKER_BOX -> {
                // 셜커 총알 효과
                if (random.nextFloat() < 0.25f) {
                    shulkerShot(mimic, victim);
                }
            }
            case ENDER_CHEST -> {
                // 텔레포트 + 엔더 파티클
                if (random.nextFloat() < 0.35f) {
                    enderBlink(mimic, victim);
                }
            }
        }

        // === 레벨별 특수 능력 ===

        // Lv.2+ : 점착 (이동속도 감소)
        if (mimic.getLevel() >= 2 && random.nextFloat() < 0.4f) {
            victim.addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOWNESS, 60, 0, false, true, true));
            victim.getWorld().spawnParticle(Particle.ITEM_SLIME,
                    victim.getLocation().add(0, 0.5, 0), 8, 0.3, 0.2, 0.3, 0);
        }
    }

    /**
     * 미믹이 데미지를 받을 때 호출된다.
     * 분열 등 반응형 능력을 처리한다.
     */
    public void onMimicDamaged(MimicEntity mimic, double damage) {
        if (mimic.getMobEntity() == null || mimic.getMobEntity().isDead()) return;

        Husk mob = mimic.getMobEntity();
        mimic.setLastDamagedTime(System.currentTimeMillis());

        // 피격 파티클
        mob.getWorld().spawnParticle(Particle.DUST,
                mob.getLocation().add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0,
                new Particle.DustOptions(Color.fromRGB(100, 0, 0), 1.0f));

        // Lv.3+ : 분열 (체력 50% 이하)
        if (mimic.getLevel() >= 3) {
            var maxHp = mob.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
            if (maxHp != null && mob.getHealth() <= maxHp.getBaseValue() * 0.5) {
                // 한 번만 분열
                if (!mob.getPersistentDataContainer().has(plugin.getMimicSplitKey())) {
                    mob.getPersistentDataContainer().set(plugin.getMimicSplitKey(),
                            org.bukkit.persistence.PersistentDataType.BOOLEAN, true);
                    spawnMiniMimics(mimic);
                }
            }
        }
    }

    /**
     * 미믹 사망 시 호출된다.
     */
    public void onMimicDeath(MimicEntity mimic, Player killer) {
        if (mimic.getMobEntity() == null) return;

        Location deathLoc = mimic.getMobEntity().getLocation();
        World world = deathLoc.getWorld();

        // 사망 파티클 연출
        world.spawnParticle(Particle.EXPLOSION, deathLoc.clone().add(0, 0.5, 0),
                2, 0.3, 0.3, 0.3, 0);
        world.spawnParticle(Particle.SMOKE, deathLoc.clone().add(0, 0.5, 0),
                30, 0.5, 0.5, 0.5, 0.1);
        world.spawnParticle(Particle.DUST, deathLoc.clone().add(0, 1, 0),
                20, 0.5, 0.5, 0.5, 0,
                new Particle.DustOptions(Color.fromRGB(80, 0, 100), 1.5f));

        // 사망 사운드
        world.playSound(deathLoc, Sound.ENTITY_WARDEN_DEATH, 0.6f, 1.5f);
        world.playSound(deathLoc, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 0.8f, 0.5f);

        // 처치 메시지
        if (killer != null) {
            String bonusText = mimic.isPreemptiveStrike() ? " §a(선제 공격!)" : "";
            killer.sendMessage(Component.text("[미믹] ", NamedTextColor.DARK_RED)
                    .append(Component.text("Lv." + mimic.getLevel() + " " + mimic.getType().getDisplayName(),
                            mimic.getLevelColor()))
                    .append(Component.text("을(를) 처치했다!" + bonusText, NamedTextColor.YELLOW)));
        }

        mimic.setState(MimicState.DEAD);
    }

    // === 유형별 특수 공격 구현 ===

    /**
     * 작업대 미믹 - 톱날 투사체 파티클
     */
    private void spawnToolProjectile(MimicEntity mimic, Player target) {
        Husk mob = mimic.getMobEntity();
        Location start = mob.getLocation().add(0, 1.2, 0);
        Vector dir = target.getLocation().add(0, 1, 0).subtract(start).toVector().normalize();

        new BukkitRunnable() {
            Location current = start.clone();
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 15) { cancel(); return; }
                current.add(dir.clone().multiply(0.8));

                current.getWorld().spawnParticle(Particle.CRIT, current, 3, 0.1, 0.1, 0.1, 0);
                current.getWorld().spawnParticle(Particle.DUST, current, 2, 0.05, 0.05, 0.05, 0,
                        new Particle.DustOptions(Color.fromRGB(139, 69, 19), 0.8f));

                // 히트 체크
                if (current.distance(target.getLocation().add(0, 1, 0)) < 1.5) {
                    target.damage(2.0, mob);
                    cancel();
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * 화로 미믹 - 화염 브레스
     */
    private void fireBreath(MimicEntity mimic, Player target) {
        Husk mob = mimic.getMobEntity();
        Location start = mob.getLocation().add(0, 1.2, 0);
        Vector dir = target.getLocation().add(0, 1, 0).subtract(start).toVector().normalize();

        mob.getWorld().playSound(start, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.8f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 8) { cancel(); return; }
                Location particleLoc = start.clone().add(dir.clone().multiply(ticks * 0.6));
                double spread = ticks * 0.15;

                particleLoc.getWorld().spawnParticle(Particle.FLAME, particleLoc,
                        5, spread, spread, spread, 0.02);
                particleLoc.getWorld().spawnParticle(Particle.SMOKE, particleLoc,
                        3, spread, spread, spread, 0.01);

                // 범위 내 플레이어 불붙이기
                for (Player p : particleLoc.getWorld().getPlayers()) {
                    if (p.getLocation().distance(particleLoc) < 1.5 + spread) {
                        p.setFireTicks(60);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    /**
     * 모루 미믹 - 내리찍기
     */
    private void anvilSlam(MimicEntity mimic, Player target) {
        Husk mob = mimic.getMobEntity();
        Location mobLoc = mob.getLocation();

        // 점프
        mob.setVelocity(new Vector(0, 0.8, 0));
        mob.getWorld().playSound(mobLoc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.2f, 0.5f);

        // 1초 후 내려찍기 효과
        new BukkitRunnable() {
            @Override
            public void run() {
                if (mob.isDead()) return;
                Location landLoc = mob.getLocation();
                landLoc.getWorld().playSound(landLoc, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.7f);
                landLoc.getWorld().spawnParticle(Particle.BLOCK, landLoc,
                        40, 1.5, 0.3, 1.5, 0.1,
                        Material.STONE.createBlockData());

                // 주변 플레이어 넉백
                for (Player p : landLoc.getWorld().getPlayers()) {
                    if (p.getLocation().distance(landLoc) < 3.0) {
                        Vector knockback = p.getLocation().subtract(landLoc).toVector()
                                .normalize().multiply(1.2).setY(0.5);
                        p.setVelocity(knockback);
                        p.damage(4.0, mob);
                    }
                }
            }
        }.runTaskLater(plugin, 15L);
    }

    /**
     * 인챈트 테이블 미믹 - 마법 구체
     */
    private void magicBlast(MimicEntity mimic, Player target) {
        Husk mob = mimic.getMobEntity();
        Location start = mob.getLocation().add(0, 1.5, 0);

        mob.getWorld().playSound(start, Sound.ENTITY_EVOKER_CAST_SPELL, 1.0f, 1.2f);

        // 마법 구체 3발 발사
        for (int i = 0; i < 3; i++) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (mob.isDead()) return;
                    Location s = mob.getLocation().add(0, 1.5, 0);
                    Vector dir = target.getLocation().add(0, 1, 0).subtract(s).toVector().normalize();

                    new BukkitRunnable() {
                        Location current = s.clone();
                        int t = 0;

                        @Override
                        public void run() {
                            if (t >= 20) { cancel(); return; }
                            current.add(dir.clone().multiply(0.7));
                            current.getWorld().spawnParticle(Particle.ENCHANTED_HIT, current,
                                    3, 0.1, 0.1, 0.1, 0);
                            current.getWorld().spawnParticle(Particle.DUST, current,
                                    2, 0.05, 0.05, 0.05, 0,
                                    new Particle.DustOptions(Color.fromRGB(100, 0, 255), 0.8f));

                            if (current.distance(target.getLocation().add(0, 1, 0)) < 1.5) {
                                target.damage(3.0, mob);
                                target.addPotionEffect(new PotionEffect(
                                        PotionEffectType.LEVITATION, 30, 0, false, true));
                                cancel();
                            }
                            t++;
                        }
                    }.runTaskTimer(plugin, 0L, 1L);
                }
            }.runTaskLater(plugin, i * 5L);
        }
    }

    /**
     * 셜커 미믹 - 셜커 총알 효과
     */
    private void shulkerShot(MimicEntity mimic, Player target) {
        Husk mob = mimic.getMobEntity();
        Location start = mob.getLocation().add(0, 1.5, 0);

        mob.getWorld().playSound(start, Sound.ENTITY_SHULKER_SHOOT, 1.0f, 1.0f);

        // 호밍 파티클 발사
        new BukkitRunnable() {
            Location current = start.clone();
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 40 || mob.isDead()) { cancel(); return; }

                Vector dir = target.getLocation().add(0, 1, 0).subtract(current).toVector().normalize();
                current.add(dir.multiply(0.5));

                current.getWorld().spawnParticle(Particle.END_ROD, current, 2, 0.05, 0.05, 0.05, 0);
                current.getWorld().spawnParticle(Particle.DUST, current, 1, 0, 0, 0, 0,
                        new Particle.DustOptions(Color.fromRGB(200, 100, 255), 1.0f));

                if (current.distance(target.getLocation().add(0, 1, 0)) < 1.5) {
                    target.damage(3.0, mob);
                    target.addPotionEffect(new PotionEffect(
                            PotionEffectType.LEVITATION, 60, 1, false, true));
                    current.getWorld().spawnParticle(Particle.EXPLOSION, current, 1, 0, 0, 0, 0);
                    cancel();
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * 엔더 미믹 - 텔레포트 기습
     */
    private void enderBlink(MimicEntity mimic, Player target) {
        Husk mob = mimic.getMobEntity();
        Location fromLoc = mob.getLocation();

        // 텔레포트 이펙트 (출발)
        fromLoc.getWorld().spawnParticle(Particle.PORTAL, fromLoc.clone().add(0, 1, 0),
                30, 0.3, 0.5, 0.3, 0.5);
        fromLoc.getWorld().playSound(fromLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);

        // 대상 뒤로 텔레포트
        Location behindTarget = target.getLocation().clone()
                .add(target.getLocation().getDirection().multiply(-2));
        behindTarget.setY(target.getLocation().getY());

        mob.teleport(behindTarget);

        // 텔레포트 이펙트 (도착)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (mob.isDead()) return;
                Location arrivedLoc = mob.getLocation();
                arrivedLoc.getWorld().spawnParticle(Particle.PORTAL, arrivedLoc.clone().add(0, 1, 0),
                        30, 0.3, 0.5, 0.3, 0.5);
                arrivedLoc.getWorld().spawnParticle(Particle.DUST, arrivedLoc.clone().add(0, 1, 0),
                        10, 0.4, 0.6, 0.4, 0,
                        new Particle.DustOptions(Color.fromRGB(100, 0, 150), 1.2f));
                arrivedLoc.getWorld().playSound(arrivedLoc, Sound.ENTITY_ENDERMAN_SCREAM, 0.6f, 1.2f);
            }
        }.runTaskLater(plugin, 2L);
    }

    /**
     * 미니 미믹을 소환한다 (Lv.3+ 분열).
     */
    private void spawnMiniMimics(MimicEntity mimic) {
        Husk parentMob = mimic.getMobEntity();
        if (parentMob == null || parentMob.isDead()) return;

        Location loc = parentMob.getLocation();
        int count = plugin.getConfig().getInt("combat.split-count", 2);

        loc.getWorld().playSound(loc, Sound.ENTITY_SLIME_SQUISH, 1.0f, 1.5f);
        loc.getWorld().spawnParticle(Particle.ITEM_SLIME, loc.clone().add(0, 0.5, 0),
                15, 0.5, 0.3, 0.5, 0.1);

        // 주변 플레이어에게 알림
        for (Player p : loc.getWorld().getPlayers()) {
            if (p.getLocation().distance(loc) < 32) {
                p.sendMessage(Component.text("[미믹] ", NamedTextColor.DARK_RED)
                        .append(Component.text("미믹이 분열했다!", NamedTextColor.LIGHT_PURPLE)));
            }
        }

        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI / count) * i;
            Location spawnLoc = loc.clone().add(Math.cos(angle) * 1.5, 0, Math.sin(angle) * 1.5);

            Husk mini = loc.getWorld().spawn(spawnLoc, Husk.class, husk -> {
                husk.customName(Component.text("미니 " + mimic.getType().getDisplayName(), NamedTextColor.GRAY));
                husk.setCustomNameVisible(true);
                husk.setBaby();

                var maxHp = husk.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                if (maxHp != null) maxHp.setBaseValue(10);
                husk.setHealth(10);

                var speed = husk.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED);
                if (speed != null) speed.setBaseValue(0.35);

                var dmg = husk.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE);
                if (dmg != null) dmg.setBaseValue(3);

                husk.getEquipment().setHelmet(new ItemStack(mimic.getType().getDisguiseBlock()));
                husk.getEquipment().setHelmetDropChance(0);
                husk.setPersistent(false);
                husk.setCanPickupItems(false);

                // 미니 미믹 태그 (드롭 없음)
                husk.getPersistentDataContainer().set(plugin.getMimicMiniKey(),
                        org.bukkit.persistence.PersistentDataType.BOOLEAN, true);

                if (parentMob.getTarget() != null) {
                    husk.setTarget(parentMob.getTarget());
                }
            });
        }
    }
}
