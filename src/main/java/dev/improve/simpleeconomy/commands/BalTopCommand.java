package dev.improve.simpleeconomy.commands;

import dev.improve.simpleeconomy.managers.DatabaseManager;
import dev.improve.simpleeconomy.utils.MessageUtil;
import dev.improve.simpleeconomy.utils.Config;
import dev.improve.simpleeconomy.SimpleEconomy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Map;
import java.util.UUID;

public class BalTopCommand implements CommandExecutor {

    private final SimpleEconomy plugin;

    public BalTopCommand(SimpleEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MessageUtil msg = plugin.getMessageUtil();
        DatabaseManager db = plugin.getDatabaseManager();

        sender.sendMessage(msg.getMessage("baltop.loading", "&7Loading baltop data..."));

        db.getTopBalancesAsync(10).thenAccept(topBalances -> {
            // Return to main thread for sending messages
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                sender.sendMessage(msg.getMessage("baltop.header", "&7Top &#54daf410 &7Richest Players:"));

                if (topBalances.isEmpty()) {
                    sender.sendMessage(msg.getMessage("baltop.empty", "&7No player data found."));
                    return;
                }

                int position = 1;
                for (Map.Entry<UUID, Double> entry : topBalances.entrySet()) {
                    OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
                    String playerName = player.getName() == null ? "Unknown" : player.getName();
                    String template = msg.getMessage("baltop.entry", "&7{position}. &#54daf4{player} &7- &#54daf4{balance}");
                    String balance = msg.formatCurrency(entry.getValue());

                    if (template.contains("${balance}")) {
                        balance = balance.startsWith(Config.CURRENCY_SYMBOL)
                                ? balance.substring(Config.CURRENCY_SYMBOL.length())
                                : balance;
                    }

                    sender.sendMessage(template
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
