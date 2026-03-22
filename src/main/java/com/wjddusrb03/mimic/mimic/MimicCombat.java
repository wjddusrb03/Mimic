package com.wjddusrb03.mimic.mimic;

import com.wjddusrb03.mimic.Mimic;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Random;

/**
 * 미믹 전투 AI — 유형별 특수 공격, 분노 모드, 사망 연출.
 */
public class MimicCombat {

    private final Mimic plugin;
    private final Random random = new Random();

    public MimicCombat(Mimic plugin) {
        this.plugin = plugin;
    }

    /**
     * 미믹이 플레이어를 공격할 때 호출된다.
     */
    public void onMimicAttack(MimicEntity mimic, Player victim, double damage) {
        if (mimic.getMobEntity() == null || mimic.getMobEntity().isDead()) return;
        Husk mob = mimic.getMobEntity();
        Location mobLoc = mob.getLocation();
        boolean rage = mimic.isRageMode();
        float chanceBoost = rage ? 2.0f : 1.0f;

        victim.getWorld().playSound(mobLoc, mimic.getType().getAttackSound(), 0.8f, rage ? 1.25f : 1.0f);

        // 분노 오라 파티클
        if (rage) {
            mob.getWorld().spawnParticle(Particle.FLAME, mobLoc.clone().add(0, 1, 0),
                    10, 0.35, 0.55, 0.35, 0.06);
        }

        switch (mimic.getType()) {
            case CHEST -> {
                victim.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR,
                        victim.getLocation().add(0, 1, 0), 6, 0.3, 0.3, 0.3, 0);
                if (random.nextFloat() < 0.22f * chanceBoost) {
                    tongueLash(mimic, victim);
                }
            }
            case BARREL -> {
                victim.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR,
                        victim.getLocation().add(0, 1, 0), 6, 0.3, 0.3, 0.3, 0);
                if (random.nextFloat() < 0.28f * chanceBoost) {
                    barrelRoll(mimic, victim);
                }
            }
            case CRAFTING_TABLE -> {
                if (random.nextFloat() < 0.30f * chanceBoost) {
                    tripleToolProjectile(mimic, victim);
                }
            }
            case FURNACE -> {
                if (random.nextFloat() < 0.28f * chanceBoost) {
                    if (random.nextBoolean()) firePool(mimic, victim);
                    else fireBreath(mimic, victim);
                }
            }
            case ANVIL -> {
                if (random.nextFloat() < 0.22f * chanceBoost) {
                    anvilSlam(mimic, victim);
                }
            }
            case ENCHANTING_TABLE -> {
                if (random.nextFloat() < 0.30f * chanceBoost) {
                    orbitBlast(mimic, victim);
                }
            }
            case SHULKER_BOX -> {
                if (random.nextFloat() < 0.28f * chanceBoost) {
                    shulkerBarrage(mimic, victim);
                }
            }
            case ENDER_CHEST -> {
                if (random.nextFloat() < 0.35f * chanceBoost) {
                    tripleEnderBlink(mimic, victim);
                }
            }
        }

        // Lv.2+ 점착 (이동속도 감소)
        if (mimic.getLevel() >= 2 && random.nextFloat() < 0.40f) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, false, true, true));
            victim.getWorld().spawnParticle(Particle.ITEM_SLIME,
                    victim.getLocation().add(0, 0.5, 0), 10, 0.3, 0.2, 0.3, 0.05);
        }
    }

    /**
     * 미믹이 데미지를 받을 때 호출된다.
     */
    public void onMimicDamaged(MimicEntity mimic, double damage) {
        if (mimic.getMobEntity() == null || mimic.getMobEntity().isDead()) return;
        Husk mob = mimic.getMobEntity();
        mimic.setLastDamagedTime(System.currentTimeMillis());

        // 피격 파티클 (레벨 색상)
        Color hitColor = mimic.getLevelColorAsColor();
        mob.getWorld().spawnParticle(Particle.DUST,
                mob.getLocation().add(0, 1, 0), 8, 0.35, 0.5, 0.35, 0,
                new Particle.DustOptions(hitColor, 1.2f));
        mob.getWorld().spawnParticle(Particle.CRIT,
                mob.getLocation().add(0, 1, 0), 5, 0.3, 0.4, 0.3, 0.1);

        // Lv.3+ 분열 (HP 50% 이하, 최초 1회)
        if (mimic.getLevel() >= 3) {
            var maxHp = mob.getAttribute(Attribute.MAX_HEALTH);
            if (maxHp != null && mob.getHealth() <= maxHp.getBaseValue() * 0.5
                    && !mob.getPersistentDataContainer().has(plugin.getMimicSplitKey())) {
                mob.getPersistentDataContainer().set(plugin.getMimicSplitKey(),
                        org.bukkit.persistence.PersistentDataType.BOOLEAN, true);
                spawnMiniMimics(mimic);
            }
        }

        // 분노 모드 (HP 25% 이하, 최초 1회)
        if (!mimic.isRageMode()) {
            var maxHp = mob.getAttribute(Attribute.MAX_HEALTH);
            if (maxHp != null && mob.getHealth() <= maxHp.getBaseValue() * 0.25) {
                activateRageMode(mimic);
            }
        }
    }

    /**
     * 미믹 사망 시 호출된다 (레벨별 연출).
     */
    public void onMimicDeath(MimicEntity mimic, Player killer) {
        if (mimic.getMobEntity() == null) return;
        Location deathLoc = mimic.getMobEntity().getLocation();
        World world = deathLoc.getWorld();
        int level = mimic.getLevel();
        Color levelColor = mimic.getLevelColorAsColor();

        if (level <= 2) {
            // 기본 사망
            world.spawnParticle(Particle.EXPLOSION, deathLoc.clone().add(0, 0.5, 0), 2, 0.3, 0.3, 0.3, 0);
            world.spawnParticle(Particle.SMOKE, deathLoc.clone().add(0, 0.5, 0), 30, 0.5, 0.5, 0.5, 0.1);
            world.playSound(deathLoc, Sound.ENTITY_WARDEN_DEATH, 0.6f, 1.5f);

        } else if (level <= 4) {
            // 중급: 큰 폭발 + 나선 파티클
            world.spawnParticle(Particle.EXPLOSION, deathLoc.clone().add(0, 0.5, 0), 4, 0.5, 0.5, 0.5, 0);
            world.spawnParticle(Particle.SMOKE, deathLoc.clone().add(0, 0.5, 0), 55, 0.6, 0.6, 0.6, 0.12);
            world.playSound(deathLoc, Sound.ENTITY_WARDEN_DEATH, 0.7f, 1.2f);

            new BukkitRunnable() {
                int t = 0;
                @Override public void run() {
                    if (t >= 35) { cancel(); return; }
                    double angle = t * 0.5;
                    double r = 2.0 - (t / 35.0) * 2.0;
                    double y = t * 0.07;
                    world.spawnParticle(Particle.DUST,
                            deathLoc.clone().add(Math.cos(angle) * r, y, Math.sin(angle) * r),
                            3, 0.08, 0.08, 0.08, 0,
                            new Particle.DustOptions(levelColor, 1.5f));
                    t++;
                }
            }.runTaskTimer(plugin, 0L, 1L);

        } else {
            // Lv.5: 대폭발 + 충격파 + 넉백
            world.spawnParticle(Particle.EXPLOSION, deathLoc.clone().add(0, 0.5, 0), 10, 1.0, 1.0, 1.0, 0);
            world.spawnParticle(Particle.SMOKE, deathLoc.clone().add(0, 0.5, 0), 90, 1.0, 0.8, 1.0, 0.18);
            world.playSound(deathLoc, Sound.ENTITY_ENDER_DRAGON_DEATH, 0.5f, 1.5f);
            world.playSound(deathLoc, Sound.ENTITY_WARDEN_DEATH, 0.8f, 0.8f);

            // 충격파 링
            for (int i = 0; i < 36; i++) {
                double angle = (Math.PI * 2.0 / 36) * i;
                world.spawnParticle(Particle.DUST,
                        deathLoc.clone().add(Math.cos(angle) * 3, 0.5, Math.sin(angle) * 3),
                        5, 0.08, 0.08, 0.08, 0,
                        new Particle.DustOptions(Color.fromRGB(255, 160, 0), 2.0f));
            }
            // 주변 플레이어 날려보내기
            for (Player p : world.getPlayers()) {
                double dist = p.getLocation().distance(deathLoc);
                if (dist < 10) {
                    Vector dir = p.getLocation().subtract(deathLoc).toVector().normalize()
                            .multiply(1.6).setY(0.6);
                    p.setVelocity(p.getVelocity().add(dir));
                }
            }
        }

        world.spawnParticle(Particle.DUST, deathLoc.clone().add(0, 1, 0),
                25, 0.5, 0.5, 0.5, 0,
                new Particle.DustOptions(Color.fromRGB(80, 0, 100), 1.5f));
        world.playSound(deathLoc, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 0.8f, 0.5f);

        if (killer != null) {
            String bonusText = mimic.isPreemptiveStrike() ? " §a(선제 공격!)" : "";
            killer.sendMessage(Component.text("[미믹] ", NamedTextColor.DARK_RED)
                    .append(Component.text("Lv." + level + " " + mimic.getType().getDisplayName(),
                            mimic.getLevelColor()))
                    .append(Component.text("을(를) 처치했다!" + bonusText, NamedTextColor.YELLOW)));
        }
        mimic.setState(MimicState.DEAD);
    }

    // ─── 분노 모드 ───

    private void activateRageMode(MimicEntity mimic) {
        mimic.setRageMode(true);
        Husk mob = mimic.getMobEntity();
        Location loc = mob.getLocation();

        // 속도 +50%, 공격 +30%
        var speed = mob.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(speed.getBaseValue() * 1.5);
        var atk = mob.getAttribute(Attribute.ATTACK_DAMAGE);
        if (atk != null) atk.setBaseValue(atk.getBaseValue() * 1.3);

        mob.setGlowing(true);
        mob.getWorld().playSound(loc, Sound.ENTITY_RAVAGER_ROAR, 1.2f, 0.5f);
        mob.getWorld().playSound(loc, Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 1.5f);

        // 분노 폭발 파티클
        for (int i = 0; i < 36; i++) {
            double angle = (Math.PI * 2.0 / 36) * i;
            loc.getWorld().spawnParticle(Particle.DUST,
                    loc.clone().add(Math.cos(angle) * 2, 0.5, Math.sin(angle) * 2),
                    3, 0.05, 0.1, 0.05, 0,
                    new Particle.DustOptions(Color.RED, 1.8f));
        }
        loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(0, 1, 0),
                35, 0.4, 0.6, 0.4, 0.12);

        for (Player p : loc.getWorld().getPlayers()) {
            if (p.getLocation().distance(loc) < 32) {
                p.sendMessage(Component.text("[미믹] ", NamedTextColor.DARK_RED)
                        .append(Component.text("Lv." + mimic.getLevel() + " " + mimic.getType().getDisplayName(),
                                mimic.getLevelColor()))
                        .append(Component.text("이(가) 분노했다!", NamedTextColor.RED)
                                .decoration(TextDecoration.BOLD, true)));
            }
        }
    }

    // ─── 특수 공격 ───

    /** 상자 미믹 — 혀 공격 */
    private void tongueLash(MimicEntity mimic, Player target) {
        Husk mob = mimic.getMobEntity();
        Location start = mob.getLocation().add(0, 0.8, 0);
        Location end = target.getLocation().add(0, 0.8, 0);
        Vector dir = end.clone().subtract(start).toVector().normalize();
        double dist = start.distance(end);
        int steps = Math.max(1, (int)(dist / 0.4));

        mob.getWorld().playSound(start, Sound.ENTITY_FROG_TONGUE, 0.8f, 1.2f);

        // 혀 파티클 라인 (빨간색)
        for (int i = 0; i <= steps; i++) {
            final int fi = i;
            new BukkitRunnable() {
                @Override public void run() {
                    Location pt = start.clone().add(dir.clone().multiply(fi * 0.4));
                    pt.getWorld().spawnParticle(Particle.DUST, pt, 3, 0.04, 0.04, 0.04, 0,
                            new Particle.DustOptions(Color.fromRGB(200, 30, 30), 0.9f));
                }
            }.runTaskLater(plugin, i);
        }

        // 히트 체크
        new BukkitRunnable() {
            @Override public void run() {
                if (target.getLocation().distance(mob.getLocation()) < dist + 1.0) {
                    target.damage(3.0, mob);
                    target.getWorld().spawnParticle(Particle.CRIT,
                            target.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0.1);
                }
            }
        }.runTaskLater(plugin, steps + 2L);
    }

    /** 배럴 미믹 — 배럴 롤 돌진 */
    private void barrelRoll(MimicEntity mimic, Player target) {
        Husk mob = mimic.getMobEntity();
        Location start = mob.getLocation();
        Vector dir = target.getLocation().subtract(start).toVector().normalize();

        mob.getWorld().playSound(start, Sound.BLOCK_BARREL_OPEN, 1.2f, 0.5f);
        mob.getWorld().playSound(start, Sound.ENTITY_RAVAGER_ATTACK, 0.8f, 0.8f);
        mob.setVelocity(dir.clone().multiply(1.5).setY(0.2));

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= 12 || mob.isDead()) { cancel(); return; }
                Location mobLoc = mob.getLocation();
                // 회전 링 파티클 (트레일)
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2.0 / 8) * i + t * 0.6;
                    mobLoc.getWorld().spawnParticle(Particle.DUST,
                            mobLoc.clone().add(Math.cos(angle) * 0.5, 0.5, Math.sin(angle) * 0.5),
                            1, 0, 0, 0, 0,
                            new Particle.DustOptions(Color.fromRGB(120, 60, 0), 1.2f));
                }
                // 충격 체크
                if (mobLoc.distance(target.getLocation()) < 2.5) {
                    target.damage(5.0, mob);
                    target.setVelocity(dir.clone().multiply(1.8).setY(0.7));
                    mobLoc.getWorld().spawnParticle(Particle.EXPLOSION,
                            mobLoc.clone().add(0, 0.5, 0), 1, 0.2, 0.2, 0.2, 0);
                    cancel();
                }
                t++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /** 작업대 미믹 — 3방향 회전 톱날 */
    private void tripleToolProjectile(MimicEntity mimic, Player target) {
        Husk mob = mimic.getMobEntity();
        Location start = mob.getLocation().add(0, 1.2, 0);
        Vector centerDir = target.getLocation().add(0, 1, 0).subtract(start).toVector().normalize();

        mob.getWorld().playSound(start, Sound.BLOCK_WOOD_BREAK, 1.0f, 1.5f);

        double[] yawOffsets = {0, -0.35, 0.35};
        for (double yaw : yawOffsets) {
            Vector dir = rotateAroundY(centerDir.clone(), yaw);
            new BukkitRunnable() {
                Location current = start.clone();
                int ticks = 0;
                @Override public void run() {
                    if (ticks >= 16 || mob.isDead()) { cancel(); return; }
                    current.add(dir.clone().multiply(0.85));
                    // 회전 CRIT 파티클
                    for (int i = 0; i < 3; i++) {
                        double a = (Math.PI * 2.0 / 3) * i + ticks * 0.9;
                        Location pLoc = current.clone().add(Math.cos(a) * 0.2, Math.sin(a) * 0.2, 0);
                        current.getWorld().spawnParticle(Particle.CRIT, pLoc, 1, 0, 0, 0, 0);
                    }
                    current.getWorld().spawnParticle(Particle.DUST, current, 2, 0.04, 0.04, 0.04, 0,
                            new Particle.DustOptions(Color.fromRGB(139, 69, 19), 0.8f));
                    if (current.distance(target.getLocation().add(0, 1, 0)) < 1.5) {
                        target.damage(2.5, mob);
                        cancel();
                    }
                    ticks++;
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }
    }

    /** 화로 미믹 — 화염 브레스 */
    private void fireBreath(MimicEntity mimic, Player target) {
        Husk mob = mimic.getMobEntity();
        Location start = mob.getLocation().add(0, 1.2, 0);
        Vector dir = target.getLocation().add(0, 1, 0).subtract(start).toVector().normalize();

        mob.getWorld().playSound(start, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.8f);

        new BukkitRunnable() {
            int ticks = 0;
            @Override public void run() {
                if (ticks >= 10) { cancel(); return; }
                Location pLoc = start.clone().add(dir.clone().multiply(ticks * 0.65));
                double spread = ticks * 0.18;
                pLoc.getWorld().spawnParticle(Particle.FLAME, pLoc, 8, spread, spread, spread, 0.03);
                pLoc.getWorld().spawnParticle(Particle.SMOKE, pLoc, 4, spread, spread, spread, 0.01);
                for (Player p : pLoc.getWorld().getPlayers()) {
                    if (p.getLocation().distance(pLoc) < 1.5 + spread) {
                        p.setFireTicks(80);
                    }
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    /** 화로 미믹 — 화염 웅덩이 */
    private void firePool(MimicEntity mimic, Player target) {
        Husk mob = mimic.getMobEntity();
        Location poolCenter = target.getLocation().clone();

        mob.getWorld().playSound(poolCenter, Sound.BLOCK_FURNACE_FIRE_CRACKLE, 1.5f, 0.6f);
        mob.getWorld().playSound(poolCenter, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.5f);

        // 경고 원형 링
        for (int i = 0; i < 24; i++) {
            double angle = (Math.PI * 2.0 / 24) * i;
            poolCenter.getWorld().spawnParticle(Particle.DUST,
                    poolCenter.clone().add(Math.cos(angle) * 2.5, 0.1, Math.sin(angle) * 2.5),
                    2, 0.05, 0.05, 0.05, 0,
                    new Particle.DustOptions(Color.fromRGB(255, 100, 0), 1.2f));
        }

        // 3초간 화염 웅덩이
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= 60) { cancel(); return; }
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        if (Math.abs(dx) + Math.abs(dz) > 3) continue;
                        Location fireLoc = poolCenter.clone().add(dx, 0.1, dz);
                        if (t % 3 == 0) {
                            poolCenter.getWorld().spawnParticle(Particle.FLAME, fireLoc,
                                    2, 0.2, 0.1, 0.2, 0.02);
                            poolCenter.getWorld().spawnParticle(Particle.LAVA, fireLoc,
                                    1, 0.2, 0.05, 0.2, 0);
                        }
                    }
                }
                if (t % 10 == 0) {
                    for (Player p : poolCenter.getWorld().getPlayers()) {
                        if (p.getLocation().distance(poolCenter) < 2.5) {
                            p.setFireTicks(40);
                            p.damage(1.5, mob);
                        }
                    }
                }
                t++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /** 모루 미믹 — 모루 슬램 (지진 충격파) */
    private void anvilSlam(MimicEntity mimic, Player target) {
        Husk mob = mimic.getMobEntity();
        Location mobLoc = mob.getLocation();

        mob.setVelocity(new Vector(0, 0.95, 0));
        mob.getWorld().playSound(mobLoc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.2f, 0.5f);

        new BukkitRunnable() {
            @Override public void run() {
                if (mob.isDead()) return;
                Location landLoc = mob.getLocation();

                landLoc.getWorld().playSound(landLoc, Sound.BLOCK_ANVIL_LAND, 1.8f, 0.6f);
                landLoc.getWorld().playSound(landLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 0.8f);

                // 돌/심층암 파편
                landLoc.getWorld().spawnParticle(Particle.BLOCK, landLoc,
                        60, 2.0, 0.4, 2.0, 0.15, Material.STONE.createBlockData());
                landLoc.getWorld().spawnParticle(Particle.BLOCK, landLoc,
                        40, 2.0, 0.4, 2.0, 0.12, Material.DEEPSLATE.createBlockData());

                // 방사형 충격파 링
                for (int i = 0; i < 32; i++) {
                    double angle = (Math.PI * 2.0 / 32) * i;
                    landLoc.getWorld().spawnParticle(Particle.DUST,
                            landLoc.clone().add(Math.cos(angle) * 3, 0.1, Math.sin(angle) * 3),
                            3, 0.05, 0.05, 0.05, 0,
                            new Particle.DustOptions(Color.fromRGB(100, 100, 100), 1.5f));
                }

                // 거리 비례 넉백 + 데미지
                for (Player p : landLoc.getWorld().getPlayers()) {
                    double dist = p.getLocation().distance(landLoc);
                    if (dist < 4.5) {
                        Vector knockback = p.getLocation().subtract(landLoc).toVector()
                                .normalize().multiply(1.6).setY(0.65);
                        p.setVelocity(knockback);
                        p.damage(5.0 * (1.0 - dist / 5.0), mob);
                    }
                }
            }
        }.runTaskLater(plugin, 15L);
    }

    /** 인챈트 미믹 — 오비트 블라스트 (3개 구체 공전 후 발사) */
    private void orbitBlast(MimicEntity mimic, Player target) {
        Husk mob = mimic.getMobEntity();
        Location center = mob.getLocation().add(0, 1.5, 0);

        mob.getWorld().playSound(center, Sound.ENTITY_EVOKER_CAST_SPELL, 1.0f, 1.2f);

        // 3개 구체가 공전하다 순차 발사
        for (int orbIdx = 0; orbIdx < 3; orbIdx++) {
            final double baseAngle = (Math.PI * 2.0 / 3) * orbIdx;
            final int oIdx = orbIdx;
            new BukkitRunnable() {
                int t = 0;
                @Override public void run() {
                    if (t >= 30 || mob.isDead()) {
                        if (!mob.isDead()) launchOrb(mob, center.clone(), target, oIdx);
                        cancel(); return;
                    }
                    double angle = baseAngle + t * 0.25;
                    Location orbLoc = center.clone().add(
                            Math.cos(angle) * 1.2, Math.sin(t * 0.1) * 0.3, Math.sin(angle) * 1.2);
                    center.getWorld().spawnParticle(Particle.ENCHANTED_HIT, orbLoc, 3, 0.05, 0.05, 0.05, 0);
                    // 각 구체별 색상
                    Color orbColor = oIdx == 0 ? Color.fromRGB(200, 50, 200)
                            : oIdx == 1 ? Color.fromRGB(50, 200, 50) : Color.fromRGB(50, 50, 255);
                    center.getWorld().spawnParticle(Particle.DUST, orbLoc, 2, 0.05, 0.05, 0.05, 0,
                            new Particle.DustOptions(orbColor, 1.0f));
                    t++;
                }
            }.runTaskTimer(plugin, orbIdx * 4L, 1L);
        }
    }

    private void launchOrb(Husk mob, Location start, Player target, int orbIdx) {
        if (mob.isDead()) return;
        Vector dir = target.getLocation().add(0, 1, 0).subtract(start).toVector().normalize();
        Color orbColor = orbIdx == 0 ? Color.fromRGB(200, 50, 200)
                : orbIdx == 1 ? Color.fromRGB(50, 200, 50) : Color.fromRGB(50, 50, 255);

        new BukkitRunnable() {
            Location current = start.clone();
            int t = 0;
            @Override public void run() {
                if (t >= 28 || mob.isDead()) { cancel(); return; }
                current.add(dir.clone().multiply(0.7));
                current.getWorld().spawnParticle(Particle.ENCHANTED_HIT, current, 4, 0.1, 0.1, 0.1, 0);
                current.getWorld().spawnParticle(Particle.DUST, current, 2, 0.05, 0.05, 0.05, 0,
                        new Particle.DustOptions(orbColor, 0.9f));
                if (current.distance(target.getLocation().add(0, 1, 0)) < 1.5) {
                    target.damage(3.0, mob);
                    target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 35, 0, false, true));
                    target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 0, false, true));
                    current.getWorld().spawnParticle(Particle.EXPLOSION, current, 1, 0, 0, 0, 0);
                    cancel();
                }
                t++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /** 셜커 미믹 — 3연발 호밍 + 방어막 파티클 */
    private void shulkerBarrage(MimicEntity mimic, Player target) {
        Husk mob = mimic.getMobEntity();
        Location start = mob.getLocation().add(0, 1.5, 0);

        mob.getWorld().playSound(start, Sound.ENTITY_SHULKER_SHOOT, 1.0f, 0.9f);

        // 방어막 파티클 (발사 시)
        for (int i = 0; i < 24; i++) {
            double angle = (Math.PI * 2.0 / 24) * i;
            mob.getWorld().spawnParticle(Particle.END_ROD,
                    mob.getLocation().add(0, 1, 0).add(Math.cos(angle) * 0.9, 0, Math.sin(angle) * 0.9),
                    1, 0.05, 0.05, 0.05, 0);
        }

        // 3발 순차 발사
        for (int shot = 0; shot < 3; shot++) {
            new BukkitRunnable() {
                @Override public void run() {
                    if (mob.isDead()) return;
                    Location s = mob.getLocation().add(0, 1.5, 0);
                    new BukkitRunnable() {
                        Location current = s.clone();
                        int t = 0;
                        @Override public void run() {
                            if (t >= 50 || mob.isDead()) { cancel(); return; }
                            Vector dir = target.getLocation().add(0, 1, 0).subtract(current).toVector().normalize();
                            current.add(dir.multiply(0.55));
                            current.getWorld().spawnParticle(Particle.END_ROD, current, 2, 0.04, 0.04, 0.04, 0);
                            current.getWorld().spawnParticle(Particle.DUST, current, 1, 0, 0, 0, 0,
                                    new Particle.DustOptions(Color.fromRGB(200, 100, 255), 1.0f));
                            if (current.distance(target.getLocation().add(0, 1, 0)) < 1.5) {
                                target.damage(2.5, mob);
                                target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 55, 1, false, true));
                                current.getWorld().spawnParticle(Particle.EXPLOSION, current, 1, 0, 0, 0, 0);
                                cancel();
                            }
                            t++;
                        }
                    }.runTaskTimer(plugin, 0L, 1L);
                }
            }.runTaskLater(plugin, shot * 8L);
        }
    }

    /** 엔더 미믹 — 3연속 텔레포트 + 어둠 */
    private void tripleEnderBlink(MimicEntity mimic, Player target) {
        Husk mob = mimic.getMobEntity();

        // 어둠 효과 먼저
        target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 70, 0, false, true));

        new BukkitRunnable() {
            int blinks = 0;
            @Override public void run() {
                if (blinks >= 3 || mob.isDead()) { cancel(); return; }
                Location fromLoc = mob.getLocation();

                // 출발 이펙트
                fromLoc.getWorld().spawnParticle(Particle.PORTAL, fromLoc.clone().add(0, 1, 0),
                        28, 0.3, 0.5, 0.3, 0.5);
                fromLoc.getWorld().playSound(fromLoc, Sound.ENTITY_ENDERMAN_TELEPORT,
                        0.8f, 1.0f + blinks * 0.25f);

                // 매번 다른 각도로 순간이동
                double angle = (blinks * Math.PI * 2.0 / 3) + Math.PI;
                Location dest = target.getLocation().clone().add(
                        Math.cos(angle) * (1.5 + blinks * 0.5), 0, Math.sin(angle) * (1.5 + blinks * 0.5));
                mob.teleport(dest);

                final int b = blinks;
                // 도착 이펙트
                new BukkitRunnable() {
                    @Override public void run() {
                        if (mob.isDead()) return;
                        Location arr = mob.getLocation();
                        arr.getWorld().spawnParticle(Particle.PORTAL, arr.clone().add(0, 1, 0),
                                28, 0.3, 0.5, 0.3, 0.5);
                        arr.getWorld().spawnParticle(Particle.DUST, arr.clone().add(0, 1, 0),
                                14, 0.4, 0.6, 0.4, 0,
                                new Particle.DustOptions(Color.fromRGB(100, 0, 150), 1.3f));
                        arr.getWorld().playSound(arr, Sound.ENTITY_ENDERMAN_SCREAM,
                                0.55f, 1.2f + b * 0.15f);
                    }
                }.runTaskLater(plugin, 1L);

                blinks++;
            }
        }.runTaskTimer(plugin, 0L, 7L);
    }

    // ─── 유틸 ───

    private Vector rotateAroundY(Vector v, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vector(v.getX() * cos - v.getZ() * sin, v.getY(), v.getX() * sin + v.getZ() * cos);
    }

    private void spawnMiniMimics(MimicEntity mimic) {
        Husk parentMob = mimic.getMobEntity();
        if (parentMob == null || parentMob.isDead()) return;

        Location loc = parentMob.getLocation();
        int count = plugin.getConfig().getInt("combat.split-count", 2);

        loc.getWorld().playSound(loc, Sound.ENTITY_SLIME_SQUISH, 1.0f, 1.5f);
        loc.getWorld().spawnParticle(Particle.ITEM_SLIME, loc.clone().add(0, 0.5, 0),
                20, 0.6, 0.4, 0.6, 0.15);

        for (Player p : loc.getWorld().getPlayers()) {
            if (p.getLocation().distance(loc) < 32) {
                p.sendMessage(Component.text("[미믹] ", NamedTextColor.DARK_RED)
                        .append(Component.text("미믹이 분열했다!", NamedTextColor.LIGHT_PURPLE)));
            }
        }

        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI / count) * i;
            Location spawnLoc = loc.clone().add(Math.cos(angle) * 1.5, 0, Math.sin(angle) * 1.5);

            loc.getWorld().spawn(spawnLoc, Husk.class, husk -> {
                husk.customName(Component.text("미니 " + mimic.getType().getDisplayName(), NamedTextColor.GRAY));
                husk.setCustomNameVisible(true);
                husk.setBaby();

                var maxHp = husk.getAttribute(Attribute.MAX_HEALTH);
                if (maxHp != null) maxHp.setBaseValue(12);
                husk.setHealth(12);

                var speed = husk.getAttribute(Attribute.MOVEMENT_SPEED);
                if (speed != null) speed.setBaseValue(0.38);

                var dmg = husk.getAttribute(Attribute.ATTACK_DAMAGE);
                if (dmg != null) dmg.setBaseValue(3.0);

                husk.getEquipment().setHelmet(new ItemStack(mimic.getType().getDisguiseBlock()));
                husk.getEquipment().setHelmetDropChance(0);
                husk.setPersistent(false);
                husk.setCanPickupItems(false);

                husk.getPersistentDataContainer().set(plugin.getMimicMiniKey(),
                        org.bukkit.persistence.PersistentDataType.BOOLEAN, true);

                if (parentMob.getTarget() != null) husk.setTarget(parentMob.getTarget());
            });
        }
    }
}
