package dev.improve.simpleeconomy;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static org.bukkit.Bukkit.getScheduler;

public class BalanceCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MessageUtil msg = SimpleEconomy.getInstance().getMessageUtil();
        DatabaseManager db = SimpleEconomy.getInstance().getDatabaseManager();

        if (args.length == 0 && sender instanceof Player player) {
            getScheduler().runTaskAsynchronously(SimpleEconomy.getInstance(), () -> {
                double balance = db.getBalance(player.getUniqueId());
                player.sendMessage(msg.getMessage("balance.self", "&7Your balance is: &#54daf4{balance}")
                        .replace("{balance}", String.format("%.2f", balance)));
            });
            return true;
        } else if (args.length == 1) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            if (target.hasPlayedBefore() || target.isOnline()) {
                getScheduler().runTaskAsynchronously(SimpleEconomy.getInstance(), () -> {
                    double balance = db.getBalance(target.getUniqueId());
                    sender.sendMessage(msg.getMessage("balance.other", "&7{player}'s balance is: &#54daf4{balance}")
                            .replace("{player}", target.getName())
                            .replace("{balance}", String.format("%.2f", balance)));
                });
            } else {
                sender.sendMessage(msg.getMessage("error.player-not-found", "&cThat player could not be found."));
            }
            return true;
        }
        sender.sendMessage(msg.getMessage("error.usage-balance", "&cUsage: /bal [player]"));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (args.length == 1) {
            return Arrays.stream(Bukkit.getOfflinePlayers())
                    .map(OfflinePlayer::getName)
                    .filter(Objects::nonNull)
                    .filter(name -> name.startsWith(args[0]))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}