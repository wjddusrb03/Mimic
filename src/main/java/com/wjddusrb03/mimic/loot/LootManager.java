package com.wjddusrb03.mimic.loot;

import com.wjddusrb03.mimic.Mimic;
import com.wjddusrb03.mimic.mimic.MimicEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Random;

/**
 * 미믹 처치 시 드롭 아이템을 관리한다.
 */
public class LootManager {

    private final Mimic plugin;
    private final Random random = new Random();

    private final NamespacedKey toothKey;
    private final NamespacedKey eyeKey;
    private final NamespacedKey essenceKey;
    private final NamespacedKey heartKey;

    public LootManager(Mimic plugin) {
        this.plugin = plugin;
        this.toothKey = new NamespacedKey(plugin, "mimic_tooth");
        this.eyeKey = new NamespacedKey(plugin, "mimic_eye");
        this.essenceKey = new NamespacedKey(plugin, "mimic_essence");
        this.heartKey = new NamespacedKey(plugin, "mimic_heart");
    }

    /**
     * 미믹 처치 시 전리품을 드롭한다.
     *
     * @param mimic  처치된 미믹
     * @param loc    드롭 위치
     * @param killer 처치한 플레이어
     */
    public void dropLoot(MimicEntity mimic, Location loc, Player killer) {
        int level = mimic.getLevel();
        double levelBonus = 1.0 + level * plugin.getConfig().getDouble("loot.level-bonus", 0.1);
        double preemptiveMultiplier = mimic.isPreemptiveStrike() ? 1.5 : 1.0;

        // 미믹 이빨 드롭
        int toothMin = plugin.getConfig().getInt("loot.tooth-min", 1);
        int toothMax = plugin.getConfig().getInt("loot.tooth-max", 3);
        int toothCount = toothMin + random.nextInt(Math.max(1, toothMax - toothMin + 1));
        toothCount = (int) Math.ceil(toothCount * preemptiveMultiplier);
        if (toothCount > 0) {
            loc.getWorld().dropItemNaturally(loc, createTooth(toothCount));
        }

        // 미믹 눈알 드롭
        double eyeChance = plugin.getConfig().getDouble("loot.eye-chance", 0.4) * levelBonus * preemptiveMultiplier;
        if (random.nextDouble() < eyeChance) {
            loc.getWorld().dropItemNaturally(loc, createEye());
        }

        // 미믹 정수 드롭
        double essenceChance = plugin.getConfig().getDouble("loot.essence-chance", 0.1) * levelBonus * preemptiveMultiplier;
        if (random.nextDouble() < essenceChance) {
            loc.getWorld().dropItemNaturally(loc, createEssence());
        }

        // 미믹 심장 드롭 (Lv.4+)
        if (level >= 4) {
            double heartChance = plugin.getConfig().getDouble("loot.heart-chance", 0.03) * levelBonus * preemptiveMultiplier;
            if (random.nextDouble() < heartChance) {
                loc.getWorld().dropItemNaturally(loc, createHeart());
            }
        }
    }

    /**
     * 미믹 이빨 아이템을 생성한다.
     */
    public ItemStack createTooth(int amount) {
        ItemStack item = new ItemStack(Material.PRISMARINE_SHARD, amount);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("미믹 이빨")
                .color(NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("미믹의 날카로운 이빨")
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.getPersistentDataContainer().set(toothKey, PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 미믹 눈알 아이템을 생성한다.
     */
    public ItemStack createEye() {
        ItemStack item = new ItemStack(Material.ENDER_EYE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("미믹 눈알")
                .color(NamedTextColor.DARK_PURPLE)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("섬뜩하게 빛나는 눈")
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.getPersistentDataContainer().set(eyeKey, PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 미믹 정수 아이템을 생성한다.
     */
    public ItemStack createEssence() {
        ItemStack item = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("미믹 정수")
                .color(NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("순수한 미믹의 에너지")
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.getPersistentDataContainer().set(essenceKey, PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 미믹 심장 아이템을 생성한다.
     */
    public ItemStack createHeart() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("미믹 심장")
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("전설적 미믹의 심장이 뛰고 있다")
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.getPersistentDataContainer().set(heartKey, PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
        return item;
    }

    public NamespacedKey getToothKey() { return toothKey; }
    public NamespacedKey getEyeKey() { return eyeKey; }
    public NamespacedKey getEssenceKey() { return essenceKey; }
    public NamespacedKey getHeartKey() { return heartKey; }
}
