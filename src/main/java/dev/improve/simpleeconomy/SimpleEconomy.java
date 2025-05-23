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
import org.jetbrains.annotations.NotNull;

public class SimpleEconomy extends JavaPlugin {

    private static SimpleEconomy instance;

    private MessageUtil messageUtil;
    private EconomyManager economyManager;
    private DatabaseManager databaseManager;
    private Config config;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        this.config = new Config(this);
        messageUtil = new MessageUtil(getConfig());

        databaseManager = new DatabaseManager(this);
        databaseManager.setup();

        economyManager = new EconomyManager(this, databaseManager);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        registerCommands();

        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            Bukkit.getServicesManager().register(
                    Economy.class,
                    new VaultEconomyHook(economyManager),
                    this,
                    org.bukkit.plugin.ServicePriority.Normal
            );
            getLogger().info("Vault hook registered.");
        } else {
            getLogger().warning("Vault plugin not found! Vault integration disabled.");
        }
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

    public @NotNull Config getPluginConfig() {
        return config;
    }

    public static SimpleEconomy getInstance() {
        return instance;
    }

    public MessageUtil getMessageUtil() {
        return messageUtil;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}
