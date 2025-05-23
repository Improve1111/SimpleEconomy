package dev.improve.simpleeconomy.commands;

import dev.improve.simpleeconomy.managers.DatabaseManager;
import dev.improve.simpleeconomy.utils.MessageUtil;
import dev.improve.simpleeconomy.SimpleEconomy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Map;
import java.util.UUID;

import static org.bukkit.Bukkit.getScheduler;

public class BalTopCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MessageUtil msg = SimpleEconomy.getInstance().getMessageUtil();
        DatabaseManager db = SimpleEconomy.getInstance().getDatabaseManager();

        sender.sendMessage(msg.getMessage("baltop.loading", "&7Loading baltop data..."));

        getScheduler().runTaskAsynchronously(SimpleEconomy.getInstance(), () -> {
            Map<UUID, Double> topBalances = db.getTopBalances(10);

            SimpleEconomy.getInstance().getServer().getScheduler().runTask(SimpleEconomy.getInstance(), () -> {
                sender.sendMessage(msg.getMessage("baltop.header", "&7Top &#54daf410 &7Richest Players:"));

                if (topBalances.isEmpty()) {
                    sender.sendMessage(msg.getMessage("baltop.empty", "&7No player data found."));
                    return;
                }

                int position = 1;
                for (Map.Entry<UUID, Double> entry : topBalances.entrySet()) {
                    OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
                    String playerName = player.getName() == null ? "Unknown" : player.getName();
                    String balance = String.format("%.2f", entry.getValue());

                    sender.sendMessage(msg.getMessage("baltop.entry", "&7{position}. &#54daf4{player} &7- &#54daf4${balance}")
                            .replace("{position}", String.valueOf(position))
                            .replace("{player}", playerName)
                            .replace("{balance}", balance));
                    position++;
                }
            });
        });
        return true;
    }
}