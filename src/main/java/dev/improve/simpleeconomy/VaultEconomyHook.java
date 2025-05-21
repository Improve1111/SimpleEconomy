package dev.improve.simpleeconomy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class VaultEconomyHook implements Economy {

    private final EconomyManager economyManager;
    private final SimpleEconomy plugin;

    public VaultEconomyHook(EconomyManager economyManager) {
        this.economyManager = economyManager;
        this.plugin = SimpleEconomy.getInstance();
    }

    @Override
    public boolean isEnabled() {
        return plugin.isEnabled();
    }

    @Override
    public String getName() {
        return "SimpleEconomy";
    }

    @Override
    public boolean hasBankSupport() {
        return false; // This economy plugin doesn't support banks
    }

    @Override
    public int fractionalDigits() {
        return 2; // Allows for two decimal places (cents)
    }

    @Override
    public String format(double amount) {
        return String.format("%.2f", amount);
    }

    @Override
    public String currencyNamePlural() {
        return plugin.getConfig().getString("settings.currency-plural", "Dollars");
    }

    @Override
    public String currencyNameSingular() {
        return plugin.getConfig().getString("settings.currency-singular", "Dollar");
    }

    @Override
    public boolean hasAccount(String playerName) {
        // In our implementation, all players have an account automatically
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        return player.hasPlayedBefore() || player.isOnline();
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        // In our implementation, all players have an account automatically
        return player.hasPlayedBefore() || player.isOnline();
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) {
        // Our economy is not world-specific
        return hasAccount(playerName);
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        // Our economy is not world-specific
        return hasAccount(player);
    }

    @Override
    public double getBalance(String playerName) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        return getBalanceSync(player.getUniqueId());
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return getBalanceSync(player.getUniqueId());
    }

    // Helper method to get balance synchronously but using CompletableFuture to handle async DB operations
    private double getBalanceSync(UUID uuid) {
        // We need to get the balance asynchronously but return it synchronously
        CompletableFuture<Double> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            double balance = plugin.getDatabaseManager().getBalance(uuid);
            future.complete(balance);
        });

        try {
            return future.get(); // This will block until the result is available
        } catch (InterruptedException | ExecutionException e) {
            plugin.getLogger().severe("Error getting balance: " + e.getMessage());
            return 0.0;
        }
    }

    @Override
    public double getBalance(String playerName, String worldName) {
        // Our economy is not world-specific
        return getBalance(playerName);
    }

    @Override
    public double getBalance(OfflinePlayer player, String worldName) {
        // Our economy is not world-specific
        return getBalance(player);
    }

    @Override
    public boolean has(String playerName, double amount) {
        return getBalance(playerName) >= amount;
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return getBalance(player) >= amount;
    }

    @Override
    public boolean has(String playerName, String worldName, double amount) {
        // Our economy is not world-specific
        return has(playerName, amount);
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        // Our economy is not world-specific
        return has(player, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        if (amount < 0) {
            return new EconomyResponse(0, getBalance(playerName), ResponseType.FAILURE, "Cannot withdraw negative amount");
        }

        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        UUID uuid = player.getUniqueId();

        // Handle this operation asynchronously with CompletableFuture
        CompletableFuture<EconomyResponse> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            double balance = plugin.getDatabaseManager().getBalance(uuid);

            if (balance < amount) {
                future.complete(new EconomyResponse(0, balance, ResponseType.FAILURE, "Insufficient funds"));
                return;
            }

            double newBalance = balance - amount;
            plugin.getDatabaseManager().setBalance(uuid, newBalance);

            future.complete(new EconomyResponse(amount, newBalance, ResponseType.SUCCESS, null));
        });

        try {
            return future.get(); // This will block until the result is available
        } catch (InterruptedException | ExecutionException e) {
            plugin.getLogger().severe("Error withdrawing funds: " + e.getMessage());
            return new EconomyResponse(0, 0, ResponseType.FAILURE, "Internal error occurred");
        }
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        if (amount < 0) {
            return new EconomyResponse(0, getBalance(player), ResponseType.FAILURE, "Cannot withdraw negative amount");
        }

        UUID uuid = player.getUniqueId();

        // Handle this operation asynchronously with CompletableFuture
        CompletableFuture<EconomyResponse> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            double balance = plugin.getDatabaseManager().getBalance(uuid);

            if (balance < amount) {
                future.complete(new EconomyResponse(0, balance, ResponseType.FAILURE, "Insufficient funds"));
                return;
            }

            double newBalance = balance - amount;
            plugin.getDatabaseManager().setBalance(uuid, newBalance);

            future.complete(new EconomyResponse(amount, newBalance, ResponseType.SUCCESS, null));
        });

        try {
            return future.get(); // This will block until the result is available
        } catch (InterruptedException | ExecutionException e) {
            plugin.getLogger().severe("Error withdrawing funds: " + e.getMessage());
            return new EconomyResponse(0, 0, ResponseType.FAILURE, "Internal error occurred");
        }
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        // Our economy is not world-specific
        return withdrawPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        // Our economy is not world-specific
        return withdrawPlayer(player, amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        if (amount < 0) {
            return new EconomyResponse(0, getBalance(playerName), ResponseType.FAILURE, "Cannot deposit negative amount");
        }

        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        UUID uuid = player.getUniqueId();

        // Handle this operation asynchronously with CompletableFuture
        CompletableFuture<EconomyResponse> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            double balance = plugin.getDatabaseManager().getBalance(uuid);
            double newBalance = balance + amount;
            plugin.getDatabaseManager().setBalance(uuid, newBalance);

            future.complete(new EconomyResponse(amount, newBalance, ResponseType.SUCCESS, null));
        });

        try {
            return future.get(); // This will block until the result is available
        } catch (InterruptedException | ExecutionException e) {
            plugin.getLogger().severe("Error depositing funds: " + e.getMessage());
            return new EconomyResponse(0, 0, ResponseType.FAILURE, "Internal error occurred");
        }
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        if (amount < 0) {
            return new EconomyResponse(0, getBalance(player), ResponseType.FAILURE, "Cannot deposit negative amount");
        }

        UUID uuid = player.getUniqueId();

        // Handle this operation asynchronously with CompletableFuture
        CompletableFuture<EconomyResponse> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            double balance = plugin.getDatabaseManager().getBalance(uuid);
            double newBalance = balance + amount;
            plugin.getDatabaseManager().setBalance(uuid, newBalance);

            future.complete(new EconomyResponse(amount, newBalance, ResponseType.SUCCESS, null));
        });

        try {
            return future.get(); // This will block until the result is available
        } catch (InterruptedException | ExecutionException e) {
            plugin.getLogger().severe("Error depositing funds: " + e.getMessage());
            return new EconomyResponse(0, 0, ResponseType.FAILURE, "Internal error occurred");
        }
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        // Our economy is not world-specific
        return depositPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        // Our economy is not world-specific
        return depositPlayer(player, amount);
    }

    @Override
    public EconomyResponse createBank(String name, String player) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "SimpleEconomy does not support bank accounts");
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "SimpleEconomy does not support bank accounts");
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "SimpleEconomy does not support bank accounts");
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "SimpleEconomy does not support bank accounts");
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "SimpleEconomy does not support bank accounts");
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "SimpleEconomy does not support bank accounts");
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "SimpleEconomy does not support bank accounts");
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "SimpleEconomy does not support bank accounts");
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "SimpleEconomy does not support bank accounts");
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "SimpleEconomy does not support bank accounts");
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "SimpleEconomy does not support bank accounts");
    }

    @Override
    public List<String> getBanks() {
        return new ArrayList<>(); // No banks supported
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        // Accounts are automatically created when accessed
        return true;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        // Accounts are automatically created when accessed
        return true;
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        // Our economy is not world-specific and accounts are created automatically
        return true;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        // Our economy is not world-specific and accounts are created automatically
        return true;
    }
}