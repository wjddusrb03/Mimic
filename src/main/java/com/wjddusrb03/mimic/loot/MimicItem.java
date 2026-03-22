package com.wjddusrb03.mimic.loot;

import com.wjddusrb03.mimic.Mimic;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * 미믹 관련 커스텀 아이템과 제작법을 관리한다.
 */
public class MimicItem {

    private static NamespacedKey detectorKey;
    private static NamespacedKey greedSwordKey;
    private static NamespacedKey mimicArmorKey;
    private static NamespacedKey mimicBaitKey;

    /**
     * 미믹 감지기를 생성한다.
     */
    public static ItemStack createDetector(Mimic plugin) {
        ensureKeys(plugin);
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("미믹 감지기")
                .color(NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("8블록 내 미믹을 감지합니다")
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.getPersistentDataContainer().set(detectorKey, PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 탐욕의 검을 생성한다.
     */
    public static ItemStack createGreedSword(Mimic plugin) {
        ensureKeys(plugin);
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("탐욕의 검")
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("미믹에게 2배 데미지")
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("미믹 드롭률 50% 증가")
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.getPersistentDataContainer().set(greedSwordKey, PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 미믹 갑옷을 생성한다.
     */
    public static ItemStack createMimicArmor(Mimic plugin) {
        ensureKeys(plugin);
        ItemStack item = new ItemStack(Material.DIAMOND_CHESTPLATE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("미믹 갑옷")
                .color(NamedTextColor.DARK_PURPLE)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("기습 공격 면역")
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("미믹 근처에서 경고음")
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.getPersistentDataContainer().set(mimicArmorKey, PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 미믹 미끼를 생성한다.
     */
    public static ItemStack createMimicBait(Mimic plugin) {
        ensureKeys(plugin);
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("미믹 미끼")
                .color(NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("설치 시 주변 미믹을 유인합니다")
                        .color(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.getPersistentDataContainer().set(mimicBaitKey, PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * 제작법을 등록한다.
     */
    public static void registerRecipes(Mimic plugin) {
        ensureKeys(plugin);

        // 미믹 감지기: 미믹 눈알 + 나침반 + 철괴
        NamespacedKey detectorRecipeKey = new NamespacedKey(plugin, "mimic_detector_recipe");
        ShapedRecipe detectorRecipe = new ShapedRecipe(detectorRecipeKey, createDetector(plugin));
        detectorRecipe.shape(" E ", " C ", " I ");
        detectorRecipe.setIngredient('E', Material.ENDER_EYE);
        detectorRecipe.setIngredient('C', Material.COMPASS);
        detectorRecipe.setIngredient('I', Material.IRON_INGOT);
        plugin.getServer().addRecipe(detectorRecipe);

        // 탐욕의 검: 미믹 이빨 + 다이아몬드 검 + 미믹 정수
        NamespacedKey swordRecipeKey = new NamespacedKey(plugin, "mimic_greed_sword_recipe");
        ShapedRecipe swordRecipe = new ShapedRecipe(swordRecipeKey, createGreedSword(plugin));
        swordRecipe.shape(" T ", " S ", " E ");
        swordRecipe.setIngredient('T', Material.PRISMARINE_SHARD);
        swordRecipe.setIngredient('S', Material.DIAMOND_SWORD);
        swordRecipe.setIngredient('E', Material.AMETHYST_SHARD);
        plugin.getServer().addRecipe(swordRecipe);

        // 미믹 갑옷: 미믹 정수 + 다이아몬드 흉갑 + 미믹 눈알
        NamespacedKey armorRecipeKey = new NamespacedKey(plugin, "mimic_armor_recipe");
        ShapedRecipe armorRecipe = new ShapedRecipe(armorRecipeKey, createMimicArmor(plugin));
        armorRecipe.shape("E E", "ACA", " A ");
        armorRecipe.setIngredient('E', Material.ENDER_EYE);
        armorRecipe.setIngredient('A', Material.AMETHYST_SHARD);
        armorRecipe.setIngredient('C', Material.DIAMOND_CHESTPLATE);
        plugin.getServer().addRecipe(armorRecipe);

        // 미믹 미끼: 미믹 이빨 + 상자 + 미믹 눈알
        NamespacedKey baitRecipeKey = new NamespacedKey(plugin, "mimic_bait_recipe");
        ShapedRecipe baitRecipe = new ShapedRecipe(baitRecipeKey, createMimicBait(plugin));
        baitRecipe.shape("TTT", "TCT", "TET");
        baitRecipe.setIngredient('T', Material.PRISMARINE_SHARD);
        baitRecipe.setIngredient('C', Material.CHEST);
        baitRecipe.setIngredient('E', Material.ENDER_EYE);
        plugin.getServer().addRecipe(baitRecipe);
    }

    /**
     * 미믹 감지기인지 확인한다.
     */
    public static boolean isDetector(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        if (detectorKey == null) return false;
        return item.getItemMeta().getPersistentDataContainer().has(detectorKey, PersistentDataType.BOOLEAN);
    }

    /**
     * 탐욕의 검인지 확인한다.
     */
    public static boolean isGreedSword(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        if (greedSwordKey == null) return false;
        return item.getItemMeta().getPersistentDataContainer().has(greedSwordKey, PersistentDataType.BOOLEAN);
    }

    /**
     * 미믹 갑옷인지 확인한다.
     */
    public static boolean isMimicArmor(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        if (mimicArmorKey == null) return false;
        return item.getItemMeta().getPersistentDataContainer().has(mimicArmorKey, PersistentDataType.BOOLEAN);
    }

    /**
     * 미믹 미끼인지 확인한다.
     */
    public static boolean isMimicBait(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        if (mimicBaitKey == null) return false;
        return item.getItemMeta().getPersistentDataContainer().has(mimicBaitKey, PersistentDataType.BOOLEAN);
    }

    private static void ensureKeys(Mimic plugin) {
        if (detectorKey == null) {
            detectorKey = new NamespacedKey(plugin, "mimic_detector");
            greedSwordKey = new NamespacedKey(plugin, "mimic_greed_sword");
            mimicArmorKey = new NamespacedKey(plugin, "mimic_armor");
            mimicBaitKey = new NamespacedKey(plugin, "mimic_bait");
        }
    }
}
