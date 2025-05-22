package dev.improve.simpleeconomy.commands;

import dev.improve.simpleeconomy.SimpleEconomy;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandExecutor {

    private final SimpleEconomy plugin = SimpleEconomy.getInstance();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(SimpleEconomy.getInstance().getMessageUtil().colorize("&7Usage: &3/simpleeconomy reload"));
            return true;
        }

        plugin.reloadConfig();

        sender.sendMessage(SimpleEconomy.getInstance().getMessageUtil().colorize("&7[&e&lSimpleEconomy&7] &7Configuration reloaded."));
        return true;
    }
}
