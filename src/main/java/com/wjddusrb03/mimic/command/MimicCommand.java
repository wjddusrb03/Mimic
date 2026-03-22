package com.wjddusrb03.mimic.command;

import com.wjddusrb03.mimic.Mimic;
import com.wjddusrb03.mimic.loot.MimicItem;
import com.wjddusrb03.mimic.mimic.MimicEntity;
import com.wjddusrb03.mimic.mimic.MimicManager;
import com.wjddusrb03.mimic.mimic.MimicType;
import com.wjddusrb03.mimic.stats.StatsManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class MimicCommand implements CommandExecutor {

    private final Mimic plugin;

    public MimicCommand(Mimic plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help" -> sendHelp(sender);
            case "stats" -> handleStats(sender);
            case "top" -> handleTop(sender);
            case "spawn" -> handleSpawn(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "list" -> handleList(sender, args);
            case "give" -> handleGive(sender, args);
            case "reload" -> handleReload(sender);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleStats(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용 가능합니다.");
            return;
        }

        StatsManager stats = plugin.getStatsManager();
        int total = stats.getKills(player.getUniqueId());

        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("  === ", NamedTextColor.DARK_GRAY)
                .append(Component.text("미믹 헌터 통계", NamedTextColor.RED)
                        .decoration(TextDecoration.BOLD, true))
                .append(Component.text(" ===", NamedTextColor.DARK_GRAY)));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("  총 처치 수: ", NamedTextColor.GRAY)
                .append(Component.text(total + "마리", NamedTextColor.YELLOW)));

        for (int lv = 1; lv <= 5; lv++) {
            int lvKills = stats.getKillsByLevel(player.getUniqueId(), lv);
            if (lvKills > 0) {
                sender.sendMessage(Component.text("  Lv." + lv + ": ", NamedTextColor.GRAY)
                        .append(Component.text(lvKills + "마리", NamedTextColor.WHITE)));
            }
        }
        sender.sendMessage(Component.empty());
    }

    private void handleTop(CommandSender sender) {
        List<Map.Entry<UUID, Integer>> topList = plugin.getStatsManager().getTopKillers(10);

        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("  === ", NamedTextColor.DARK_GRAY)
                .append(Component.text("미믹 헌터 랭킹", NamedTextColor.GOLD)
                        .decoration(TextDecoration.BOLD, true))
                .append(Component.text(" ===", NamedTextColor.DARK_GRAY)));
        sender.sendMessage(Component.empty());

        if (topList.isEmpty()) {
            sender.sendMessage(Component.text("  아직 기록이 없습니다.", NamedTextColor.GRAY));
        } else {
            int rank = 1;
            for (Map.Entry<UUID, Integer> entry : topList) {
                String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                if (name == null) name = "???";

                NamedTextColor rankColor = switch (rank) {
                    case 1 -> NamedTextColor.GOLD;
                    case 2 -> NamedTextColor.WHITE;
                    case 3 -> NamedTextColor.AQUA;
                    default -> NamedTextColor.GRAY;
                };

                sender.sendMessage(Component.text("  #" + rank + " ", rankColor)
                        .append(Component.text(name, NamedTextColor.YELLOW))
                        .append(Component.text(" - " + entry.getValue() + "마리", NamedTextColor.GRAY)));
                rank++;
            }
        }
        sender.sendMessage(Component.empty());
    }

    private void handleSpawn(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mimic.admin.spawn")) {
            msg(sender, Component.text("권한이 없습니다.", NamedTextColor.RED));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용 가능합니다.");
            return;
        }
        if (args.length < 2) {
            msg(sender, Component.text("사용법: /mimic spawn <유형> [레벨]", NamedTextColor.RED));
            msg(sender, Component.text("유형: chest, barrel, crafting_table, furnace, anvil, enchanting_table, shulker_box, ender_chest", NamedTextColor.GRAY));
            return;
        }

        MimicType type;
        try {
            type = MimicType.valueOf(args[1].toUpperCase());
        } catch (Exception e) {
            msg(sender, Component.text("알 수 없는 유형: " + args[1], NamedTextColor.RED));
            return;
        }

        int level = 1;
        if (args.length >= 3) {
            try {
                level = Integer.parseInt(args[2]);
                level = Math.max(1, Math.min(5, level));
            } catch (NumberFormatException e) {
                msg(sender, Component.text("레벨은 1~5 사이의 숫자여야 합니다.", NamedTextColor.RED));
                return;
            }
        }

        Block target = player.getTargetBlockExact(5);
        if (target == null) {
            msg(sender, Component.text("블록을 바라보고 사용하세요.", NamedTextColor.RED));
            return;
        }

        // 블록 위에 미믹 배치
        Location loc = target.getLocation().add(0, 1, 0);
        MimicEntity mimic = plugin.getMimicManager().spawnMimic(
                type, level, loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

        if (mimic != null) {
            msg(sender, Component.text("Lv." + level + " " + type.getDisplayName(), mimic.getLevelColor())
                    .append(Component.text("을(를) 배치했습니다.", NamedTextColor.GREEN)));
        } else {
            msg(sender, Component.text("미믹 한도 초과로 배치할 수 없습니다.", NamedTextColor.RED));
        }
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mimic.admin.remove")) {
            msg(sender, Component.text("권한이 없습니다.", NamedTextColor.RED));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용 가능합니다.");
            return;
        }

        double radius = 10;
        if (args.length >= 2) {
            try {
                radius = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                msg(sender, Component.text("사용법: /mimic remove <반경>", NamedTextColor.RED));
                return;
            }
        }

        int removed = plugin.getMimicManager().removeInRadius(player.getLocation(), radius);
        msg(sender, Component.text("반경 " + (int) radius + "블록 내 미믹 " + removed + "개를 제거했습니다.", NamedTextColor.YELLOW));
    }

    private void handleList(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mimic.admin.list")) {
            msg(sender, Component.text("권한이 없습니다.", NamedTextColor.RED));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용 가능합니다.");
            return;
        }

        double radius = 50;
        if (args.length >= 2) {
            try {
                radius = Double.parseDouble(args[1]);
            } catch (NumberFormatException ignored) {}
        }

        List<MimicEntity> nearby = plugin.getMimicManager().getMimicsInRadius(player.getLocation(), radius);

        sender.sendMessage(Component.text("[미믹] ", NamedTextColor.DARK_RED)
                .append(Component.text("반경 " + (int) radius + "블록 내 미믹: " + nearby.size() + "개", NamedTextColor.YELLOW)));

        for (MimicEntity mimic : nearby) {
            double dist = player.getLocation().distance(mimic.getBlockCenterLocation());
            sender.sendMessage(Component.text("  • ", NamedTextColor.GRAY)
                    .append(Component.text("Lv." + mimic.getLevel() + " " + mimic.getType().getDisplayName(),
                            mimic.getLevelColor()))
                    .append(Component.text(" [" + mimic.getState() + "]", NamedTextColor.DARK_GRAY))
                    .append(Component.text(" (" + (int) dist + "m)", NamedTextColor.GRAY)));
        }
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mimic.admin.give")) {
            msg(sender, Component.text("권한이 없습니다.", NamedTextColor.RED));
            return;
        }
        if (args.length < 3) {
            msg(sender, Component.text("사용법: /mimic give <플레이어> <아이템>", NamedTextColor.RED));
            msg(sender, Component.text("아이템: tooth, eye, essence, heart, detector, sword, armor, bait", NamedTextColor.GRAY));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            msg(sender, Component.text("플레이어를 찾을 수 없습니다: " + args[1], NamedTextColor.RED));
            return;
        }

        ItemStack item = switch (args[2].toLowerCase()) {
            case "tooth" -> plugin.getLootManager().createTooth(1);
            case "eye" -> plugin.getLootManager().createEye();
            case "essence" -> plugin.getLootManager().createEssence();
            case "heart" -> plugin.getLootManager().createHeart();
            case "detector" -> MimicItem.createDetector(plugin);
            case "sword" -> MimicItem.createGreedSword(plugin);
            case "armor" -> MimicItem.createMimicArmor(plugin);
            case "bait" -> MimicItem.createMimicBait(plugin);
            default -> null;
        };

        if (item == null) {
            msg(sender, Component.text("알 수 없는 아이템: " + args[2], NamedTextColor.RED));
            return;
        }

        target.getInventory().addItem(item);
        msg(sender, Component.text(target.getName() + "에게 아이템을 지급했습니다.", NamedTextColor.GREEN));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("mimic.admin.reload")) {
            msg(sender, Component.text("권한이 없습니다.", NamedTextColor.RED));
            return;
        }
        plugin.reloadConfig();
        msg(sender, Component.text("설정을 다시 불러왔습니다.", NamedTextColor.GREEN));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("  === ", NamedTextColor.DARK_GRAY)
                .append(Component.text("미믹", NamedTextColor.RED).decoration(TextDecoration.BOLD, true))
                .append(Component.text(" 도움말 ===", NamedTextColor.DARK_GRAY)));
        sender.sendMessage(Component.empty());

        sendCmd(sender, "/mimic stats", "내 미믹 처치 통계");
        sendCmd(sender, "/mimic top", "미믹 헌터 랭킹");

        if (sender.hasPermission("mimic.admin.spawn")) {
            sender.sendMessage(Component.empty());
            sender.sendMessage(Component.text("  관리자:", NamedTextColor.RED)
                    .decoration(TextDecoration.BOLD, true));
            sendCmd(sender, "/mimic spawn <유형> [레벨]", "미믹 배치");
            sendCmd(sender, "/mimic remove <반경>", "미믹 제거");
            sendCmd(sender, "/mimic list [반경]", "주변 미믹 목록");
            sendCmd(sender, "/mimic give <플레이어> <아이템>", "아이템 지급");
            sendCmd(sender, "/mimic reload", "설정 재로드");
        }
        sender.sendMessage(Component.empty());
    }

    private void sendCmd(CommandSender sender, String cmd, String desc) {
        sender.sendMessage(Component.text("  " + cmd, NamedTextColor.YELLOW)
                .clickEvent(ClickEvent.suggestCommand(cmd))
                .hoverEvent(HoverEvent.showText(Component.text("클릭하여 입력")))
                .append(Component.text(" - " + desc, NamedTextColor.GRAY)));
    }

    private void msg(CommandSender sender, Component message) {
        sender.sendMessage(Component.text("[미믹] ", NamedTextColor.DARK_RED).append(message));
    }
}
