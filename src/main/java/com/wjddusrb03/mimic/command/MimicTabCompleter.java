package com.wjddusrb03.mimic.command;

import com.wjddusrb03.mimic.Mimic;
import com.wjddusrb03.mimic.mimic.MimicType;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MimicTabCompleter implements TabCompleter {

    private final Mimic plugin;

    public MimicTabCompleter(Mimic plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("help", "stats", "top"));
            if (sender.hasPermission("mimic.admin.spawn")) {
                subs.addAll(List.of("spawn", "remove", "list", "give", "reload"));
            }
            return filter(subs, args[0]);
        }

        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "spawn" -> filter(
                        Arrays.stream(MimicType.values())
                                .map(t -> t.name().toLowerCase())
                                .collect(Collectors.toList()),
                        args[1]);
                case "give" -> filter(
                        Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName)
                                .collect(Collectors.toList()),
                        args[1]);
                case "remove", "list" -> filter(List.of("10", "25", "50", "100"), args[1]);
                default -> new ArrayList<>();
            };
        }

        if (args.length == 3) {
            return switch (args[0].toLowerCase()) {
                case "spawn" -> filter(List.of("1", "2", "3", "4", "5"), args[2]);
                case "give" -> filter(List.of("tooth", "eye", "essence", "heart",
                        "detector", "sword", "armor", "bait"), args[2]);
                default -> new ArrayList<>();
            };
        }

        return new ArrayList<>();
    }

    private List<String> filter(List<String> options, String input) {
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }
}
