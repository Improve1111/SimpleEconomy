package dev.improve.simpleeconomy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class SimpleEconomy extends JavaPlugin {

    private static SimpleEconomy instance;

    private MessageUtil messageUtil;
    private EconomyManager economyManager;
    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        messageUtil = new MessageUtil(getConfig());

        databaseManager = new DatabaseManager(this);
        databaseManager.setup();

        economyManager = new EconomyManager(this, databaseManager);

        // Register commands
        registerCommands();

        // Vault economy hook registration
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
    public void onDisable() {
        // Nothing specific to close here for SQLite connection since connection lifecycle handled by DatabaseManager
        // (Add closing connection if you implement it there)
    }

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

        // Register reload command under /simpleeconomy or /secon reload
        getCommand("simpleeconomy").setExecutor(new ReloadCommand());
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
