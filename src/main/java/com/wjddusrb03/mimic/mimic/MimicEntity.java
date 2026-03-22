package com.wjddusrb03.mimic.mimic;

import com.wjddusrb03.mimic.Mimic;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Husk;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 개별 미믹 엔티티를 표현한다.
 * DORMANT 상태에서는 블록만 존재하고, ACTIVE 상태에서는 Husk 몹이 스폰된다.
 */
public class MimicEntity {

    private static final AtomicInteger ID_COUNTER = new AtomicInteger(0);

    private final int id;
    private final MimicType type;
    private final int level;
    private final World world;
    private final int blockX, blockY, blockZ;

    private MimicState state;
    private Husk mobEntity;
    private UUID triggerPlayerUuid;
    private long lastDamagedTime;
    private boolean preemptiveStrike;
    private boolean rageMode = false;

    public MimicEntity(MimicType type, int level, World world, int x, int y, int z) {
        this.id = ID_COUNTER.incrementAndGet();
        this.type = type;
        this.level = Math.max(1, Math.min(5, level));
        this.world = world;
        this.blockX = x;
        this.blockY = y;
        this.blockZ = z;
        this.state = MimicState.DORMANT;
        this.preemptiveStrike = false;
    }

    /**
     * 미믹 블록을 월드에 설치한다 (DORMANT 상태로 진입).
     */
    public void placeBlock() {
        Block block = world.getBlockAt(blockX, blockY, blockZ);
        block.setType(type.getDisguiseBlock());
        state = MimicState.DORMANT;
    }

    /**
     * 변신 시퀀스를 시작한다 (강화판).
     */
    public void trigger(Player triggerPlayer, Mimic plugin, boolean isPreemptive) {
        if (state != MimicState.DORMANT && state != MimicState.ALERTING) return;

        this.state = MimicState.TRIGGERING;
        this.triggerPlayerUuid = triggerPlayer.getUniqueId();
        this.preemptiveStrike = isPreemptive;

        Location blockLoc = new Location(world, blockX + 0.5, blockY, blockZ + 0.5);

        // [0.0초] 미세 진동 파티클
        world.spawnParticle(Particle.DUST, blockLoc.clone().add(0, 0.5, 0),
                10, 0.4, 0.4, 0.4, 0,
                new Particle.DustOptions(Color.fromRGB(80, 0, 0), 0.8f));

        // [0.15초] 균열음 + 십자 균열 파티클
        new BukkitRunnable() {
            @Override public void run() {
                if (state != MimicState.TRIGGERING) return;
                world.playSound(blockLoc, Sound.BLOCK_STONE_BREAK, 0.5f, 0.3f);
                for (int i = 0; i < 4; i++) {
                    double angle = (Math.PI / 2.0) * i;
                    Location crack = blockLoc.clone().add(Math.cos(angle) * 0.5, 0.05, Math.sin(angle) * 0.5);
                    world.spawnParticle(Particle.BLOCK, crack, 5, 0.08, 0.05, 0.08, 0,
                            type.getDisguiseBlock().createBlockData());
                }
            }
        }.runTaskLater(plugin, 3L);

        // [0.3초] 빨간 눈 번쩍 + 저주 사운드
        new BukkitRunnable() {
            @Override public void run() {
                if (state != MimicState.TRIGGERING) return;
                world.playSound(blockLoc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.7f, 1.5f);
                world.spawnParticle(Particle.DUST, blockLoc.clone().add(0, 0.5, 0),
                        25, 0.4, 0.4, 0.4, 0,
                        new Particle.DustOptions(Color.fromRGB(140, 0, 0), 1.3f));
                // 양쪽 눈 파티클
                for (int i = 0; i < 4; i++) {
                    world.spawnParticle(Particle.DUST, blockLoc.clone().add(0.22, 0.72, 0.48),
                            4, 0.03, 0.03, 0.03, 0,
                            new Particle.DustOptions(Color.RED, 2.0f));
                    world.spawnParticle(Particle.DUST, blockLoc.clone().add(-0.22, 0.72, 0.48),
                            4, 0.03, 0.03, 0.03, 0,
                            new Particle.DustOptions(Color.RED, 2.0f));
                }
            }
        }.runTaskLater(plugin, 6L);

        // [0.5초] 원형 파티클 링 + 강한 진동음
        new BukkitRunnable() {
            @Override public void run() {
                if (state != MimicState.TRIGGERING) return;
                world.playSound(blockLoc, Sound.BLOCK_STONE_BREAK, 1.2f, 0.5f);
                // 원형 링
                for (int i = 0; i < 20; i++) {
                    double angle = (Math.PI * 2.0 / 20) * i;
                    Location ringLoc = blockLoc.clone().add(Math.cos(angle) * 0.9, 0.05, Math.sin(angle) * 0.9);
                    world.spawnParticle(Particle.DUST, ringLoc, 2, 0.03, 0.03, 0.03, 0,
                            new Particle.DustOptions(Color.fromRGB(170, 0, 0), 1.0f));
                }
                world.spawnParticle(Particle.BLOCK, blockLoc.clone().add(0, 0.5, 0),
                        35, 0.5, 0.5, 0.5, 0.1,
                        type.getDisguiseBlock().createBlockData());
            }
        }.runTaskLater(plugin, 10L);

        // [0.8초] 폭발 변신 + 몹 스폰 + 화면 흔들림
        new BukkitRunnable() {
            @Override public void run() {
                if (state != MimicState.TRIGGERING) return;

                world.getBlockAt(blockX, blockY, blockZ).setType(Material.AIR);
                world.playSound(blockLoc, type.getTriggerSound(), 1.5f, 0.7f);
                world.playSound(blockLoc, Sound.ENTITY_WARDEN_EMERGE, 0.8f, 1.3f);

                world.spawnParticle(Particle.EXPLOSION, blockLoc.clone().add(0, 0.5, 0),
                        3, 0.3, 0.3, 0.3, 0);
                world.spawnParticle(Particle.BLOCK, blockLoc.clone().add(0, 0.5, 0),
                        70, 0.6, 0.6, 0.6, 0.25,
                        type.getDisguiseBlock().createBlockData());
                world.spawnParticle(Particle.SMOKE, blockLoc.clone().add(0, 0.5, 0),
                        35, 0.4, 0.4, 0.4, 0.06);
                world.spawnParticle(Particle.DUST, blockLoc.clone().add(0, 1, 0),
                        25, 0.5, 0.6, 0.5, 0,
                        new Particle.DustOptions(Color.fromRGB(200, 0, 0), 1.8f));

                spawnMob(plugin, blockLoc);

                NamedTextColor levelColor = getLevelColor();
                for (Player p : world.getPlayers()) {
                    double dist = p.getLocation().distance(blockLoc);
                    if (dist < 32) {
                        p.sendMessage(Component.text("[미믹] ", NamedTextColor.DARK_RED)
                                .append(Component.text("Lv." + level + " " + type.getDisplayName(),
                                        levelColor).decoration(TextDecoration.BOLD, true))
                                .append(Component.text("이(가) 정체를 드러냈다!", NamedTextColor.RED)));
                    }
                    // 근거리 화면 흔들림
                    if (dist < 8) {
                        p.setVelocity(p.getVelocity().add(new Vector(
                                (Math.random() - 0.5) * 0.3, 0.18, (Math.random() - 0.5) * 0.3)));
                    }
                }
            }
        }.runTaskLater(plugin, 16L);

        // [1.1초] 포효 파티클 링 + 수직 기둥
        new BukkitRunnable() {
            @Override public void run() {
                if (mobEntity == null || mobEntity.isDead()) return;
                Location mobLoc = mobEntity.getLocation();
                world.playSound(mobLoc, Sound.ENTITY_RAVAGER_ROAR, 0.9f, 0.6f + level * 0.1f);
                Color ringColor = getLevelColorAsColor();
                // 방사형 링
                for (int i = 0; i < 28; i++) {
                    double angle = (Math.PI * 2.0 / 28) * i;
                    Location ringLoc = mobLoc.clone().add(Math.cos(angle) * 2.5, 0.1, Math.sin(angle) * 2.5);
                    world.spawnParticle(Particle.DUST, ringLoc, 2, 0.05, 0.05, 0.05, 0,
                            new Particle.DustOptions(ringColor, 1.5f));
                }
                // 수직 기둥
                for (int i = 0; i < 10; i++) {
                    world.spawnParticle(Particle.DUST, mobLoc.clone().add(0, i * 0.25, 0),
                            3, 0.15, 0.05, 0.15, 0,
                            new Particle.DustOptions(ringColor, 1.2f));
                }
            }
        }.runTaskLater(plugin, 22L);
    }

    /**
     * 미믹 전투 몹을 스폰한다.
     */
    private void spawnMob(Mimic plugin, Location loc) {
        mobEntity = world.spawn(loc, Husk.class, husk -> {
            NamedTextColor levelColor = getLevelColor();
            husk.customName(Component.text("Lv." + level + " ", NamedTextColor.GRAY)
                    .append(Component.text(type.getDisplayName(), levelColor)
                            .decoration(TextDecoration.BOLD, true)));
            husk.setCustomNameVisible(true);

            double health = type.getHealthForLevel(level);
            var maxHp = husk.getAttribute(Attribute.MAX_HEALTH);
            if (maxHp != null) maxHp.setBaseValue(health);
            husk.setHealth(health);

            var speed = husk.getAttribute(Attribute.MOVEMENT_SPEED);
            if (speed != null) speed.setBaseValue(type.getBaseSpeed() + (level * 0.01));

            var damage = husk.getAttribute(Attribute.ATTACK_DAMAGE);
            if (damage != null) damage.setBaseValue(type.getDamageForLevel(level));

            var kbResist = husk.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
            if (kbResist != null) kbResist.setBaseValue(
                    Math.min(1.0, type.getBaseKnockbackResist() + (level * 0.1)));

            husk.getEquipment().setHelmet(new ItemStack(type.getDisguiseBlock()));
            husk.getEquipment().setHelmetDropChance(0);

            husk.setPersistent(false);
            husk.setRemoveWhenFarAway(false);
            husk.setSilent(false);
            husk.setCanPickupItems(false);

            husk.getPersistentDataContainer().set(
                    plugin.getMimicKey(), PersistentDataType.INTEGER, id);
            husk.getPersistentDataContainer().set(
                    plugin.getMimicLevelKey(), PersistentDataType.INTEGER, level);

            if (preemptiveStrike) {
                husk.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 4, false, true));
                husk.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 2, false, true));
                husk.setGlowing(true);
                new BukkitRunnable() {
                    @Override public void run() {
                        if (mobEntity != null && !mobEntity.isDead()) mobEntity.setGlowing(false);
                    }
                }.runTaskLater(plugin, 60L);
            }

            Player target = Bukkit.getPlayer(triggerPlayerUuid);
            if (target != null && target.isOnline()) husk.setTarget(target);
        });

        state = MimicState.ACTIVE;
        lastDamagedTime = System.currentTimeMillis();
    }

    /**
     * 레벨에 따른 NamedTextColor를 반환한다.
     */
    public NamedTextColor getLevelColor() {
        return switch (level) {
            case 1 -> NamedTextColor.WHITE;
            case 2 -> NamedTextColor.GREEN;
            case 3 -> NamedTextColor.AQUA;
            case 4 -> NamedTextColor.LIGHT_PURPLE;
            case 5 -> NamedTextColor.GOLD;
            default -> NamedTextColor.GRAY;
        };
    }

    /**
     * 레벨에 따른 Bukkit Color를 반환한다 (파티클용).
     */
    public Color getLevelColorAsColor() {
        return switch (level) {
            case 1 -> Color.WHITE;
            case 2 -> Color.fromRGB(0, 220, 0);
            case 3 -> Color.fromRGB(0, 220, 220);
            case 4 -> Color.fromRGB(180, 0, 255);
            case 5 -> Color.fromRGB(255, 160, 0);
            default -> Color.GRAY;
        };
    }

    /**
     * Lv.4+ 재위장 (강화 이펙트).
     */
    public void reDisguise(Mimic plugin) {
        if (mobEntity != null && !mobEntity.isDead()) {
            Location loc = mobEntity.getLocation();
            world.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
            // 소용돌이 파티클
            for (int i = 0; i < 24; i++) {
                double angle = (Math.PI * 2.0 / 24) * i;
                double r = 1.0 - (i / 24.0) * 0.8;
                double y = (i / 24.0) * 1.8;
                Location spiralLoc = loc.clone().add(Math.cos(angle) * r, y, Math.sin(angle) * r);
                world.spawnParticle(Particle.PORTAL, spiralLoc, 3, 0.05, 0.05, 0.05, 0.2);
            }
            world.spawnParticle(Particle.SMOKE, loc.clone().add(0, 0.5, 0),
                    30, 0.4, 0.6, 0.4, 0.07);
            world.spawnParticle(Particle.DUST, loc.clone().add(0, 1, 0),
                    15, 0.3, 0.5, 0.3, 0,
                    new Particle.DustOptions(Color.fromRGB(80, 0, 120), 1.5f));
            mobEntity.remove();
            mobEntity = null;
        }
        rageMode = false;
        placeBlock();
        state = MimicState.DORMANT;
    }

    // ─── Getters / Setters ───

    public int getId() { return id; }
    public MimicType getType() { return type; }
    public int getLevel() { return level; }
    public World getWorld() { return world; }
    public int getBlockX() { return blockX; }
    public int getBlockY() { return blockY; }
    public int getBlockZ() { return blockZ; }
    public MimicState getState() { return state; }
    public void setState(MimicState state) { this.state = state; }
    public Husk getMobEntity() { return mobEntity; }
    public UUID getTriggerPlayerUuid() { return triggerPlayerUuid; }
    public long getLastDamagedTime() { return lastDamagedTime; }
    public void setLastDamagedTime(long time) { this.lastDamagedTime = time; }
    public boolean isPreemptiveStrike() { return preemptiveStrike; }
    public boolean isRageMode() { return rageMode; }
    public void setRageMode(boolean rageMode) { this.rageMode = rageMode; }

    public Location getBlockLocation() { return new Location(world, blockX, blockY, blockZ); }
    public Location getBlockCenterLocation() { return new Location(world, blockX + 0.5, blockY + 0.5, blockZ + 0.5); }

    public boolean isAt(World w, int x, int y, int z) {
        return world.equals(w) && blockX == x && blockY == y && blockZ == z;
    }

    public void remove() {
        if (mobEntity != null && !mobEntity.isDead()) mobEntity.remove();
        if (state == MimicState.DORMANT || state == MimicState.ALERTING) {
            Block block = world.getBlockAt(blockX, blockY, blockZ);
            if (block.getType() == type.getDisguiseBlock()) block.setType(Material.AIR);
        }
        state = MimicState.DEAD;
    }
}
