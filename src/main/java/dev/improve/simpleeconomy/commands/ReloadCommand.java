package dev.improve.simpleeconomy.commands;

import dev.improve.simpleeconomy.SimpleEconomy;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandExecutor {

    private final SimpleEconomy plugin;

    public ReloadCommand(SimpleEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("simpleeconomy.se")) {
            sender.sendMessage(plugin.getMessageUtil().colorize("&cYou do not have permission to do that."));
            return true;
        }

        if (args.length != 1 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(plugin.getMessageUtil().colorize("&7Usage: &3/simpleeconomy reload"));
            return true;
        }

        plugin.refreshConfiguration();
        sender.sendMessage(plugin.getMessageUtil().getMessage("reload.success", "&7[&e&lSimpleEconomy&7] &aConfiguration reloaded."));
        return true;
    }
}
