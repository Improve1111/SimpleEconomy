package dev.improve.simpleeconomy.commands;

import dev.improve.simpleeconomy.managers.DatabaseManager;
import dev.improve.simpleeconomy.utils.MessageUtil;
import dev.improve.simpleeconomy.SimpleEconomy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.bukkit.Bukkit.getScheduler;

public class EcoCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MessageUtil msg = SimpleEconomy.getInstance().getMessageUtil();
        DatabaseManager db = SimpleEconomy.getInstance().getDatabaseManager();

        if (args.length != 3) {
            sender.sendMessage(msg.getMessage("error.usage-eco", "&cUsage: /eco <give|take|set> <player> <amount>"));
            return true;
        }

        String sub = args[0].toLowerCase();
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(msg.getMessage("error.player-not-found", "&cThat player could not be found."));
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(msg.getMessage("error.invalid-amount", "&cPlease enter a valid, positive number."));
            return true;
        }

        getScheduler().runTaskAsynchronously(SimpleEconomy.getInstance(), () -> {
            switch (sub) {
                case "give":
                    double currentGive = db.getBalance(target.getUniqueId());
                    db.setBalance(target.getUniqueId(), currentGive + amount);

                    Bukkit.getScheduler().runTask(SimpleEconomy.getInstance(), () -> {
                        sender.sendMessage(msg.getMessage("eco.given", "&7Gave &#54daf4{amount} &7to &#54daf4{player}&7.")
                                .replace("{player}", target.getName())
                                .replace("{amount}", String.format("%.2f", amount)));
                    });
                    break;
                case "take":
                    double currentTake = db.getBalance(target.getUniqueId());
                    db.setBalance(target.getUniqueId(), Math.max(0, currentTake - amount));

                    Bukkit.getScheduler().runTask(SimpleEconomy.getInstance(), () -> {
                        sender.sendMessage(msg.getMessage("eco.taken", "&7Took &#54daf4{amount} &7from &#54daf4{player}&7.")
                                .replace("{player}", target.getName())
                                .replace("{amount}", String.format("%.2f", amount)));
                    });
                    break;
                case "set":
                    db.setBalance(target.getUniqueId(), amount);

                    Bukkit.getScheduler().runTask(SimpleEconomy.getInstance(), () -> {
                        sender.sendMessage(msg.getMessage("eco.set", "&7Set &#54daf4{player}&7's balance to &#54daf4{amount}&7.")
                                .replace("{player}", target.getName())
                                .replace("{amount}", String.format("%.2f", amount)));
                    });
                    break;
                default:
                    Bukkit.getScheduler().runTask(SimpleEconomy.getInstance(), () -> {
                        sender.sendMessage(msg.getMessage("error.usage-eco", "&cUsage: /eco <give|take|set> <player> <amount>"));
                    });
                    break;
            }
        });

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (args.length == 1) {
            return List.of("give", "take", "set");
        }

        if (args.length == 2) {
            return Arrays.stream(Bukkit.getOfflinePlayers())
                    .map(OfflinePlayer::getName).filter(Objects::nonNull) // intellij was screaming at me :(
                    .filter(name -> name.startsWith(args[1]))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
