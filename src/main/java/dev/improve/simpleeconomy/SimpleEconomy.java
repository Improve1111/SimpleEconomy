package dev.improve.simpleeconomy;

import dev.improve.simpleeconomy.commands.*;
import dev.improve.simpleeconomy.hooks.PAPIHook;
import dev.improve.simpleeconomy.hooks.VaultEconomyHook;
import dev.improve.simpleeconomy.listeners.PlayerListener;
import dev.improve.simpleeconomy.managers.DatabaseManager;
import dev.improve.simpleeconomy.utils.Config;
import dev.improve.simpleeconomy.utils.MessageUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public class SimpleEconomy extends JavaPlugin {

    private static SimpleEconomy instance;

    private MessageUtil messageUtil;
    private DatabaseManager databaseManager;
    private Config config;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        config = new Config(this);

        databaseManager = new DatabaseManager(this);
        databaseManager.setup();

        messageUtil = new MessageUtil(getConfig());

        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            Bukkit.getServicesManager().register(
                    Economy.class,
                    new VaultEconomyHook(this),
                    this,
                    ServicePriority.Normal
            );
            getLogger().info("Vault hook registered.");
        } else {
            getLogger().warning("Vault plugin not found! Vault integration disabled.");
        }

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        registerCommands();

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PAPIHook(this).register();
            getLogger().info("PlaceholderAPI hook registered.");
        }
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
    }

    private void registerCommands() {
        BalanceCommand balanceCommand = new BalanceCommand(this);
        PayCommand payCommand = new PayCommand(this);
        EcoCommand ecoCommand = new EcoCommand(this);

        registerCommand("balance", balanceCommand, balanceCommand);
        registerCommand("baltop", new BalTopCommand(this), null);
        registerCommand("pay", payCommand, payCommand);
        registerCommand("eco", ecoCommand, ecoCommand);
        registerCommand("simpleeconomy", new ReloadCommand(this), null);
    }

    private void registerCommand(String name, CommandExecutor executor, TabCompleter tabCompleter) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().severe("Command " + name + " is not defined in plugin.yml");
            return;
        }
        command.setExecutor(executor);
        if (tabCompleter != null) {
            command.setTabCompleter(tabCompleter);
        }
    }

    public void refreshConfiguration() {
        reloadConfig();
        config.reload();
        messageUtil = new MessageUtil(getConfig());
        if (databaseManager != null) {
            databaseManager.reloadSettings();
        }
    }

    public static SimpleEconomy getInstance() {
        return instance;
    }

    public MessageUtil getMessageUtil() {
        return messageUtil;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public Config getEconomyConfig() {
        return config;
    }
}
