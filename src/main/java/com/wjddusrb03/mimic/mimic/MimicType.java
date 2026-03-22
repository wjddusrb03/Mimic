package com.wjddusrb03.mimic.mimic;

import org.bukkit.Material;
import org.bukkit.Sound;

/**
 * 미믹 유형. 위장 블록, 전투 특성, 사운드 등을 정의한다.
 */
public enum MimicType {

    CHEST(Material.CHEST, "상자 미믹", 1,
            20, 4, 0.28, 0.0,
            Sound.BLOCK_CHEST_OPEN, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR),

    BARREL(Material.BARREL, "배럴 미믹", 1,
            20, 4, 0.32, 0.0,
            Sound.BLOCK_BARREL_OPEN, Sound.ENTITY_RAVAGER_ATTACK),

    CRAFTING_TABLE(Material.CRAFTING_TABLE, "작업대 미믹", 2,
            35, 6, 0.26, 0.2,
            Sound.BLOCK_WOOD_BREAK, Sound.ENTITY_VINDICATOR_CELEBRATE),

    FURNACE(Material.FURNACE, "화로 미믹", 2,
            40, 7, 0.24, 0.3,
            Sound.BLOCK_FURNACE_FIRE_CRACKLE, Sound.ENTITY_BLAZE_SHOOT),

    ANVIL(Material.ANVIL, "모루 미믹", 3,
            60, 10, 0.22, 0.5,
            Sound.BLOCK_ANVIL_LAND, Sound.ENTITY_IRON_GOLEM_ATTACK),

    ENCHANTING_TABLE(Material.ENCHANTING_TABLE, "인챈트 미믹", 3,
            55, 9, 0.25, 0.4,
            Sound.BLOCK_ENCHANTMENT_TABLE_USE, Sound.ENTITY_EVOKER_CAST_SPELL),

    SHULKER_BOX(Material.SHULKER_BOX, "셜커 미믹", 3,
            50, 8, 0.20, 0.6,
            Sound.BLOCK_SHULKER_BOX_OPEN, Sound.ENTITY_SHULKER_SHOOT),

    ENDER_CHEST(Material.ENDER_CHEST, "엔더 미믹", 4,
            80, 12, 0.30, 0.7,
            Sound.BLOCK_ENDER_CHEST_OPEN, Sound.ENTITY_ENDERMAN_SCREAM);

    private final Material disguiseBlock;
    private final String displayName;
    private final int minLevel;
    private final double baseHealth;
    private final double baseDamage;
    private final double baseSpeed;
    private final double baseKnockbackResist;
    private final Sound triggerSound;
    private final Sound attackSound;

    MimicType(Material disguiseBlock, String displayName, int minLevel,
              double baseHealth, double baseDamage, double baseSpeed,
              double baseKnockbackResist,
              Sound triggerSound, Sound attackSound) {
        this.disguiseBlock = disguiseBlock;
        this.displayName = displayName;
        this.minLevel = minLevel;
        this.baseHealth = baseHealth;
        this.baseDamage = baseDamage;
        this.baseSpeed = baseSpeed;
        this.baseKnockbackResist = baseKnockbackResist;
        this.triggerSound = triggerSound;
        this.attackSound = attackSound;
    }

    public Material getDisguiseBlock() { return disguiseBlock; }
    public String getDisplayName() { return displayName; }
    public int getMinLevel() { return minLevel; }
    public double getBaseHealth() { return baseHealth; }
    public double getBaseDamage() { return baseDamage; }
    public double getBaseSpeed() { return baseSpeed; }
    public double getBaseKnockbackResist() { return baseKnockbackResist; }
    public Sound getTriggerSound() { return triggerSound; }
    public Sound getAttackSound() { return attackSound; }

    /**
     * 레벨에 따른 체력을 반환한다.
     */
    public double getHealthForLevel(int level) {
        return baseHealth * (1.0 + (level - 1) * 0.5);
    }

    /**
     * 레벨에 따른 공격력을 반환한다.
     */
    public double getDamageForLevel(int level) {
        return baseDamage * (1.0 + (level - 1) * 0.3);
    }

    /**
     * Material로 미믹 타입을 찾는다.
     */
    public static MimicType fromMaterial(Material material) {
        for (MimicType type : values()) {
            if (type.disguiseBlock == material) return type;
        }
        return null;
    }
}
