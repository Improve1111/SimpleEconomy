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

        sender.sendMessage(msg.getMessage("baltop.header", "&eTop 10 Richest Players:"));
        getScheduler().runTaskAsynchronously(SimpleEconomy.getInstance(), () -> {
            Map<UUID, Double> topBalances = db.getTopBalances(10);
            for (Map.Entry<UUID, Double> entry : topBalances.entrySet()) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
                sender.sendMessage(msg.getMessage("baltop.entry", "&6{player} &7- &#54daf4{balance}")
                        .replace("{player}", player.getName() == null ? "Unknown" : player.getName())
                        .replace("{balance}", String.format("%.2f", entry.getValue())));
            }
        });
        return true;
    }
}