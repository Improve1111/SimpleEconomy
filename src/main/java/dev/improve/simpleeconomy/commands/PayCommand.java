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
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.bukkit.Bukkit.getScheduler;

public class PayCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return false;
        MessageUtil msg = SimpleEconomy.getInstance().getMessageUtil();
        DatabaseManager db = SimpleEconomy.getInstance().getDatabaseManager();

        if (args.length != 2) {
            player.sendMessage(msg.getMessage("error.usage-pay", "&cUsage: /pay <player> <amount>"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            player.sendMessage(msg.getMessage("error.player-not-found", "&cThat player could not be found."));
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

        getScheduler().runTaskAsynchronously(SimpleEconomy.getInstance(), () -> {
            if (db.getBalance(player.getUniqueId()) < amount) {
                player.sendMessage(msg.getMessage("error.insufficient-funds", "&cYou do not have enough funds."));
                return;
            }

            db.setBalance(player.getUniqueId(), db.getBalance(player.getUniqueId()) - amount);
            db.setBalance(target.getUniqueId(), db.getBalance(target.getUniqueId()) + amount);

            player.sendMessage(msg.getMessage("pay.sent", "&7You sent &#54daf4{amount} &7to &#54daf4{player}&7.")
                    .replace("{player}", target.getName())
                    .replace("{amount}", String.format("%.2f", amount)));

            if (target.isOnline() && target.getPlayer() != null) {
                target.getPlayer().sendMessage(msg.getMessage("pay.received", "&7You received &#54daf4{amount} &7from &#54daf4{player}&7.")
                        .replace("{player}", player.getName())
                        .replace("{amount}", String.format("%.2f", amount)));
            }
        });

        return true;
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
