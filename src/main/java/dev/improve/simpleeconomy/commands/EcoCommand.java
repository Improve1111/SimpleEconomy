package dev.improve.simpleeconomy.commands;

import dev.improve.simpleeconomy.managers.DatabaseManager;
import dev.improve.simpleeconomy.managers.EconomyResult;
import dev.improve.simpleeconomy.managers.EconomyStatus;
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

    private final SimpleEconomy plugin;

    public EcoCommand(SimpleEconomy plugin) {
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MessageUtil msg = plugin.getMessageUtil();
        DatabaseManager db = plugin.getDatabaseManager();

        if (!sender.hasPermission("simpleeconomy.eco")) {
            sender.sendMessage(msg.getMessage("error.no-permission", "&cYou do not have permission to do that."));
            return true;
        }

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

        getScheduler().runTaskAsynchronously(plugin, () -> {
            EconomyResult result;
            switch (sub) {
                case "give" -> result = db.deposit(target.getUniqueId(), amount);
                case "take" -> result = db.withdraw(target.getUniqueId(), amount);
                case "set" -> result = db.setBalance(target.getUniqueId(), amount);
                default -> {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            sender.sendMessage(msg.getMessage("error.usage-eco", "&cUsage: /eco <give|take|set> <player> <amount>")));
                    return;
                }
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!result.success()) {
                    sender.sendMessage(msg.getMessage(mapStatusToMessage(result.status()),
                            "&cUnable to update that balance."));
                    return;
                }

                String amountFormatted = msg.formatCurrency(
                        sub.equalsIgnoreCase("set") ? result.resultingBalance() : amount);

                String targetName = target.getName() == null ? "Unknown" : target.getName();

                switch (sub) {
                    case "give" -> sender.sendMessage(msg.getMessage("eco.given", "&7Gave &#54daf4{amount} &7to &#54daf4{player}&7.")
                            .replace("{player}", targetName)
                            .replace("{amount}", amountFormatted));
                    case "take" -> sender.sendMessage(msg.getMessage("eco.taken", "&7Took &#54daf4{amount} &7from &#54daf4{player}&7.")
                            .replace("{player}", targetName)
                            .replace("{amount}", amountFormatted));
                    case "set" -> sender.sendMessage(msg.getMessage("eco.set", "&7Set &#54daf4{player}&7's balance to &#54daf4{amount}&7.")
                            .replace("{player}", targetName)
                            .replace("{amount}", amountFormatted));
                }
            });
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

    private String mapStatusToMessage(EconomyStatus status) {
        return switch (status) {
            case INSUFFICIENT_FUNDS -> "error.insufficient-funds";
            case EXCEEDS_MAX_BALANCE -> "error.max-balance";
            case BELOW_MIN_BALANCE -> "error.invalid-amount";
            case NEGATIVE_AMOUNT, INVALID_AMOUNT -> "error.invalid-amount";
            case SAME_ACCOUNT -> "error.same-account";
            case DATABASE_ERROR -> "error.database";
            default -> "error.database";
        };
    }
}
