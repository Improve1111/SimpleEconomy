package dev.improve.simpleeconomy;

import dev.improve.simpleeconomy.commands.*;
import dev.improve.simpleeconomy.hooks.VaultEconomyHook;
import dev.improve.simpleeconomy.listeners.PlayerJoinListener;
import dev.improve.simpleeconomy.managers.*;
import dev.improve.simpleeconomy.utils.Config;
import dev.improve.simpleeconomy.utils.MessageUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class SimpleEconomy extends JavaPlugin {

    private static SimpleEconomy instance;

    private MessageUtil messageUtil;
    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        instance = this;

        databaseManager = new DatabaseManager(this);
        databaseManager.setup();

        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            Bukkit.getServicesManager().register(
                    Economy.class,
                    new VaultEconomyHook(this),
                    this,
                    org.bukkit.plugin.ServicePriority.Normal
            );
            getLogger().info("Vault hook registered.");
        } else {
            getLogger().warning("Vault plugin not found! Vault integration disabled.");
        }

        saveDefaultConfig();
        Config config = new Config(this);
        messageUtil = new MessageUtil(getConfig());
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        registerCommands();
    }

    @Override
    public void onDisable() {}

    @SuppressWarnings("DataFlowIssue")
    private void registerCommands() {
        BalanceCommand balanceCmd = new BalanceCommand();
        BalTopCommand baltopCmd = new BalTopCommand();
        PayCommand payCmd = new PayCommand();
        EcoCommand ecoCmd = new EcoCommand();

        getCommand("balance").setExecutor(balanceCmd);
        getCommand("balance").setTabCompleter(balanceCmd);

        getCommand("baltop").setExecutor(baltopCmd);

        getCommand("pay").setExecutor(payCmd);
        getCommand("pay").setTabCompleter(payCmd);

        getCommand("eco").setExecutor(ecoCmd);
        getCommand("eco").setTabCompleter(ecoCmd);

        getCommand("simpleeconomy").setExecutor(new ReloadCommand());
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
}
