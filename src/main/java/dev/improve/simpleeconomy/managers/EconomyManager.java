package dev.improve.simpleeconomy.managers;

import dev.improve.simpleeconomy.SimpleEconomy;
import org.bukkit.Bukkit;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class EconomyManager {

    private final SimpleEconomy plugin;
    private final DatabaseManager databaseManager;

    public EconomyManager(SimpleEconomy plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    /**
     * Get a player's balance asynchronously.
     * Result is provided in the callback on the main thread.
     *
     * @param uuid Player UUID
     * @param callback Consumer to receive the balance value asynchronously on main thread
     */
    public void getBalanceAsync(UUID uuid, Consumer<Double> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            double balance = databaseManager.getBalance(uuid);
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(balance));
        });
    }

    /**
     * Set a player's balance asynchronously.
     *
     * @param uuid Player UUID
     * @param amount New balance
     */
    public void setBalanceAsync(UUID uuid, double amount) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            databaseManager.setBalance(uuid, amount);
        });
    }

    /**
     * Add amount to player's balance asynchronously.
     *
     * @param uuid Player UUID
     * @param amount Amount to add
     */
    public void addBalanceAsync(UUID uuid, double amount) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            double current = databaseManager.getBalance(uuid);
            databaseManager.setBalance(uuid, current + amount);
        });
    }

    /**
     * Remove amount from player's balance asynchronously.
     *
     * @param uuid Player UUID
     * @param amount Amount to remove
     */
    public void removeBalanceAsync(UUID uuid, double amount) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            double current = databaseManager.getBalance(uuid);
            double newBalance = Math.max(0, current - amount); // prevent negative balance
            databaseManager.setBalance(uuid, newBalance);
        });
    }

    /**
     * Get top balances synchronously.
     * Because this returns a map, this should run async or only used in async tasks.
     *
     * @param limit number of top balances
     * @return map of UUID to balance, sorted descending
     */
    public Map<UUID, Double> getTopBalances(int limit) {
        return databaseManager.getTopBalances(limit);
    }
}
