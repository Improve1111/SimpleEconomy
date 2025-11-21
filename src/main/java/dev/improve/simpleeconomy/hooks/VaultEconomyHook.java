package dev.improve.simpleeconomy.hooks;

import dev.improve.simpleeconomy.SimpleEconomy;
import dev.improve.simpleeconomy.managers.DatabaseManager;
import dev.improve.simpleeconomy.managers.EconomyResult;
import dev.improve.simpleeconomy.managers.EconomyStatus;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VaultEconomyHook implements Economy {

    private final SimpleEconomy plugin;
    private final DatabaseManager databaseManager;

    public VaultEconomyHook(SimpleEconomy plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
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
        return false;
    }

    @Override
    public int fractionalDigits() {
        return 2;
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
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        return player.hasPlayedBefore() || player.isOnline();
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return player.hasPlayedBefore() || player.isOnline();
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return hasAccount(playerName);
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return hasAccount(player);
    }

    @Override
    public double getBalance(String playerName) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        return databaseManager.getBalance(player.getUniqueId());
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return databaseManager.getBalance(player.getUniqueId());
    }

    @Override
    public double getBalance(String playerName, String worldName) {
        return getBalance(playerName);
    }

    @Override
    public double getBalance(OfflinePlayer player, String worldName) {
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
        return has(playerName, amount);
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        UUID uuid = player.getUniqueId();
        EconomyResult result = databaseManager.withdraw(uuid, amount);
        if (!result.success()) {
            return failureFromStatus(result.status(), getBalance(playerName));
        }

        return new EconomyResponse(amount, result.resultingBalance(), ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        UUID uuid = player.getUniqueId();
        EconomyResult result = databaseManager.withdraw(uuid, amount);
        if (!result.success()) {
            return failureFromStatus(result.status(), getBalance(player));
        }

        return new EconomyResponse(amount, result.resultingBalance(), ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        UUID uuid = player.getUniqueId();

        EconomyResult result = databaseManager.deposit(uuid, amount);
        if (!result.success()) {
            return failureFromStatus(result.status(), getBalance(playerName));
        }

        return new EconomyResponse(amount, result.resultingBalance(), ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        UUID uuid = player.getUniqueId();

        EconomyResult result = databaseManager.deposit(uuid, amount);
        if (!result.success()) {
            return failureFromStatus(result.status(), getBalance(player));
        }

        return new EconomyResponse(amount, result.resultingBalance(), ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
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
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        databaseManager.getBalance(player.getUniqueId());
        return true;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        databaseManager.getBalance(player.getUniqueId());
        return true;
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return createPlayerAccount(playerName);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return createPlayerAccount(player);
    }

    private EconomyResponse failureFromStatus(EconomyStatus status, double balance) {
        return switch (status) {
            case INSUFFICIENT_FUNDS -> new EconomyResponse(0, balance, ResponseType.FAILURE, "Insufficient funds");
            case EXCEEDS_MAX_BALANCE -> new EconomyResponse(0, balance, ResponseType.FAILURE, "Maximum balance exceeded");
            case BELOW_MIN_BALANCE -> new EconomyResponse(0, balance, ResponseType.FAILURE, "Below minimum balance");
            case NEGATIVE_AMOUNT, INVALID_AMOUNT -> new EconomyResponse(0, balance, ResponseType.FAILURE, "Invalid amount");
            case DATABASE_ERROR -> new EconomyResponse(0, balance, ResponseType.FAILURE, "Database error");
            default -> new EconomyResponse(0, balance, ResponseType.FAILURE, "Transaction failed");
        };
    }
}