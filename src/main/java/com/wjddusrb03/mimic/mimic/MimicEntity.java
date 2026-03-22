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
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

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
    private boolean preemptiveStrike; // 선제 공격 여부

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
     * 변신 시퀀스를 시작한다.
     */
    public void trigger(Player triggerPlayer, Mimic plugin, boolean isPreemptive) {
        if (state != MimicState.DORMANT && state != MimicState.ALERTING) return;

        this.state = MimicState.TRIGGERING;
        this.triggerPlayerUuid = triggerPlayer.getUniqueId();
        this.preemptiveStrike = isPreemptive;

        Location blockLoc = new Location(world, blockX + 0.5, blockY, blockZ + 0.5);

        // === 변신 시퀀스 ===

        // [0.0초] 블록 흔들림 파티클
        world.spawnParticle(Particle.BLOCK, blockLoc.clone().add(0, 0.5, 0),
                15, 0.3, 0.3, 0.3, 0,
                type.getDisguiseBlock().createBlockData());

        // [0.2초] 경고 사운드 + 더 많은 파티클
        new BukkitRunnable() {
            @Override
            public void run() {
                if (state != MimicState.TRIGGERING) return;
                world.playSound(blockLoc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.7f, 1.5f);
                world.spawnParticle(Particle.DUST, blockLoc.clone().add(0, 0.5, 0),
                        20, 0.4, 0.4, 0.4, 0,
                        new Particle.DustOptions(Color.fromRGB(120, 0, 0), 1.2f));

                // 눈이 번쩍이는 효과 (빨간 불빛)
                world.spawnParticle(Particle.DUST, blockLoc.clone().add(0.3, 0.7, 0.3),
                        3, 0, 0, 0, 0,
                        new Particle.DustOptions(Color.RED, 1.5f));
                world.spawnParticle(Particle.DUST, blockLoc.clone().add(-0.3, 0.7, 0.3),
                        3, 0, 0, 0, 0,
                        new Particle.DustOptions(Color.RED, 1.5f));
            }
        }.runTaskLater(plugin, 4L);

        // [0.5초] 더 격렬한 흔들림
        new BukkitRunnable() {
            @Override
            public void run() {
                if (state != MimicState.TRIGGERING) return;
                world.playSound(blockLoc, Sound.BLOCK_STONE_BREAK, 1.2f, 0.5f);
                world.spawnParticle(Particle.BLOCK, blockLoc.clone().add(0, 0.5, 0),
                        30, 0.5, 0.5, 0.5, 0.1,
                        type.getDisguiseBlock().createBlockData());
            }
        }.runTaskLater(plugin, 10L);

        // [0.8초] 블록 파괴 + 몹 스폰
        new BukkitRunnable() {
            @Override
            public void run() {
                if (state != MimicState.TRIGGERING) return;

                // 블록 파괴
                Block block = world.getBlockAt(blockX, blockY, blockZ);
                block.setType(Material.AIR);

                // 트리거 사운드
                world.playSound(blockLoc, type.getTriggerSound(), 1.5f, 0.7f);
                world.playSound(blockLoc, Sound.ENTITY_WARDEN_EMERGE, 0.8f, 1.3f);

                // 파괴 파티클 폭발
                world.spawnParticle(Particle.EXPLOSION, blockLoc.clone().add(0, 0.5, 0),
                        3, 0.3, 0.3, 0.3, 0);
                world.spawnParticle(Particle.BLOCK, blockLoc.clone().add(0, 0.5, 0),
                        50, 0.5, 0.5, 0.5, 0.2,
                        type.getDisguiseBlock().createBlockData());
                world.spawnParticle(Particle.SMOKE, blockLoc.clone().add(0, 0.5, 0),
                        25, 0.4, 0.4, 0.4, 0.05);

                // 미믹 몹 스폰
                spawnMob(plugin, blockLoc);

                // 주변 플레이어에게 경고
                NamedTextColor levelColor = getLevelColor();
                for (Player p : world.getPlayers()) {
                    if (p.getLocation().distance(blockLoc) < 32) {
                        p.sendMessage(Component.text("[미믹] ", NamedTextColor.DARK_RED)
                                .append(Component.text("Lv." + level + " " + type.getDisplayName(),
                                        levelColor).decoration(TextDecoration.BOLD, true))
                                .append(Component.text("이(가) 정체를 드러냈다!", NamedTextColor.RED)));
                    }
                }
            }
        }.runTaskLater(plugin, 16L);
    }

    /**
     * 미믹 전투 몹을 스폰한다.
     */
    private void spawnMob(Mimic plugin, Location loc) {
        mobEntity = world.spawn(loc, Husk.class, husk -> {
            // 이름 설정
            NamedTextColor levelColor = getLevelColor();
            husk.customName(Component.text("Lv." + level + " ", NamedTextColor.GRAY)
                    .append(Component.text(type.getDisplayName(), levelColor)
                            .decoration(TextDecoration.BOLD, true)));
            husk.setCustomNameVisible(true);

            // 속성 설정
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

            // 플레이어 헤드 (위장 블록 텍스처)
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                // 블록 이름으로 스킨 설정 시도
                head.setItemMeta(meta);
            }
            husk.getEquipment().setHelmet(new ItemStack(type.getDisguiseBlock()));
            husk.getEquipment().setHelmetDropChance(0);

            // 기타 설정
            husk.setPersistent(false);
            husk.setRemoveWhenFarAway(false);
            husk.setSilent(false);
            husk.setCanPickupItems(false);

            // PDC에 미믹 ID 저장
            husk.getPersistentDataContainer().set(
                    plugin.getMimicKey(), PersistentDataType.INTEGER, id);
            husk.getPersistentDataContainer().set(
                    plugin.getMimicLevelKey(), PersistentDataType.INTEGER, level);

            // 선제 공격 시 스턴 효과
            if (preemptiveStrike) {
                husk.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 4, false, true));
                husk.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 2, false, true));
                husk.setGlowing(true);
                // 3초 후 글로우 해제
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (mobEntity != null && !mobEntity.isDead()) {
                            mobEntity.setGlowing(false);
                        }
                    }
                }.runTaskLater(plugin, 60L);
            }

            // 타겟 설정
            Player target = Bukkit.getPlayer(triggerPlayerUuid);
            if (target != null && target.isOnline()) {
                husk.setTarget(target);
            }
        });

        state = MimicState.ACTIVE;
        lastDamagedTime = System.currentTimeMillis();
    }

    /**
     * 레벨에 따른 색상을 반환한다.
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
     * Lv.4+ 재위장: 블록으로 되돌아간다.
     */
    public void reDisguise(Mimic plugin) {
        if (mobEntity != null && !mobEntity.isDead()) {
            Location loc = mobEntity.getLocation();
            // 재위장 파티클
            world.spawnParticle(Particle.SMOKE, loc.clone().add(0, 0.5, 0),
                    20, 0.3, 0.5, 0.3, 0.05);
            world.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.5f);

            mobEntity.remove();
            mobEntity = null;
        }

        // 다시 블록 배치
        placeBlock();
        state = MimicState.DORMANT;
    }

    // ─── Getters ───

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

    public Location getBlockLocation() {
        return new Location(world, blockX, blockY, blockZ);
    }

    public Location getBlockCenterLocation() {
        return new Location(world, blockX + 0.5, blockY + 0.5, blockZ + 0.5);
    }

    /**
     * 이 미믹이 해당 블록 위치에 있는지 확인한다.
     */
    public boolean isAt(World w, int x, int y, int z) {
        return world.equals(w) && blockX == x && blockY == y && blockZ == z;
    }

    /**
     * 미믹을 완전히 제거한다.
     */
    public void remove() {
        if (mobEntity != null && !mobEntity.isDead()) {
            mobEntity.remove();
        }
        // DORMANT 상태였으면 블록도 제거
        if (state == MimicState.DORMANT || state == MimicState.ALERTING) {
            Block block = world.getBlockAt(blockX, blockY, blockZ);
            if (block.getType() == type.getDisguiseBlock()) {
                block.setType(Material.AIR);
            }
        }
        state = MimicState.DEAD;
    }
}
