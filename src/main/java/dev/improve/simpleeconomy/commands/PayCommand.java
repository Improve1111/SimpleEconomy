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
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PayCommand implements CommandExecutor, TabCompleter {

    private final SimpleEconomy plugin;

    public PayCommand(SimpleEconomy plugin) {
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return false;
        MessageUtil msg = plugin.getMessageUtil();
        DatabaseManager db = plugin.getDatabaseManager();

        if (!player.hasPermission("simpleeconomy.pay")) {
            player.sendMessage(msg.getMessage("error.no-permission", "&cYou do not have permission to do that."));
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(msg.getMessage("error.usage-pay", "&cUsage: /pay <player> <amount>"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            player.sendMessage(msg.getMessage("error.player-not-found", "&cThat player could not be found."));
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(msg.getMessage("error.same-account", "&cYou cannot send money to yourself."));
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(msg.getMessage("error.invalid-amount", "&cPlease enter a valid, positive number."));
            return true;
        }

        if (amount <= 0) {
            player.sendMessage(msg.getMessage("error.invalid-amount", "&cPlease enter a valid, positive number."));
            return true;
        }

        db.transferAsync(player.getUniqueId(), target.getUniqueId(), amount).thenAccept(result -> {
            // Return to main thread for sending messages
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!result.success()) {
                    handleTransferFailure(player, result.status(), msg);
                    return;
                }

                String targetName = target.getName() == null ? "Unknown" : target.getName();
                player.sendMessage(msg.getMessage("pay.sent", "&7You sent &#54daf4{amount} &7to &#54daf4{player}&7.")
                        .replace("{player}", targetName)
                        .replace("{amount}", msg.formatCurrency(amount)));

                if (target.isOnline() && target.getPlayer() != null) {
                    target.getPlayer().sendMessage(msg.getMessage("pay.received", "&7You received &#54daf4{amount} &7from &#54daf4{player}&7.")
                            .replace("{player}", player.getName())
                            .replace("{amount}", msg.formatCurrency(amount)));
                }
            });
        });

        return true;
    }

    private void handleTransferFailure(Player player, EconomyStatus status, MessageUtil msg) {
        String responseKey;
        String defaultMessage;

        switch (status) {
            case INSUFFICIENT_FUNDS -> {
                responseKey = "error.insufficient-funds";
                defaultMessage = "&cYou do not have enough funds.";
            }
            case EXCEEDS_MAX_BALANCE -> {
                responseKey = "error.max-balance";
                defaultMessage = "&cThat transaction would exceed the maximum balance.";
            }
            case SAME_ACCOUNT -> {
                responseKey = "error.same-account";
                defaultMessage = "&cYou cannot send money to yourself.";
            }
            case INVALID_AMOUNT, NEGATIVE_AMOUNT -> {
                responseKey = "error.invalid-amount";
                defaultMessage = "&cPlease enter a valid, positive number.";
            }
            case DATABASE_ERROR -> {
                responseKey = "error.database";
                defaultMessage = "&cSomething went wrong while processing your transaction. Please try again.";
            }
            default -> {
                responseKey = "error.database";
                defaultMessage = "&cSomething went wrong while processing your transaction. Please try again.";
            }
        }

        player.sendMessage(msg.getMessage(responseKey, defaultMessage));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (args.length == 1){
            return Arrays.stream(Bukkit.getOfflinePlayers())
                    .map(OfflinePlayer::getName)
                    .filter(Objects::nonNull)
                    .filter(name -> name.startsWith(args[0]))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
