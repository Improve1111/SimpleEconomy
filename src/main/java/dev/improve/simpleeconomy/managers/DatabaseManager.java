package dev.improve.simpleeconomy.managers;

import dev.improve.simpleeconomy.SimpleEconomy;
import dev.improve.simpleeconomy.utils.Config;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseManager {

    private static final String TABLE_NAME = "balances";

    private final SimpleEconomy plugin;
    private final Map<UUID, Double> balanceCache = new ConcurrentHashMap<>();
    private final Object sqlLock = new Object();
    private final Object pendingLock = new Object();
    private final Map<UUID, Double> pendingWrites = new HashMap<>();

    private Connection connection;
    private PreparedStatement selectBalanceStatement;
    private PreparedStatement hasBalanceStatement;
    private PreparedStatement upsertBalanceStatement;
    private PreparedStatement deleteBalanceStatement;
    private PreparedStatement topBalancesStatement;
    private PreparedStatement totalEconomyStatement;
    private PreparedStatement playerCountStatement;
    private BukkitTask autoSaveTask;

    public DatabaseManager(SimpleEconomy plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Unable to create plugin data folder. Using working directory for database.");
            }

            File dbFile = new File(plugin.getDataFolder(), "balances.db");
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS %s (
                            uuid TEXT PRIMARY KEY,
                            balance REAL NOT NULL
                        )
                        """.formatted(TABLE_NAME));
            }

            prepareStatements();
            scheduleAutoSave();
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed to initialise database connection");
            ex.printStackTrace();
        }
    }

    private void prepareStatements() throws SQLException {
        selectBalanceStatement = connection.prepareStatement(
                "SELECT balance FROM " + TABLE_NAME + " WHERE uuid = ?");
        hasBalanceStatement = connection.prepareStatement(
                "SELECT 1 FROM " + TABLE_NAME + " WHERE uuid = ?");
        upsertBalanceStatement = connection.prepareStatement(
                "INSERT INTO " + TABLE_NAME + " (uuid, balance) VALUES (?, ?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET balance = excluded.balance");
        deleteBalanceStatement = connection.prepareStatement(
                "DELETE FROM " + TABLE_NAME + " WHERE uuid = ?");
        topBalancesStatement = connection.prepareStatement(
                "SELECT uuid, balance FROM " + TABLE_NAME + " ORDER BY balance DESC LIMIT ?");
        totalEconomyStatement = connection.prepareStatement(
                "SELECT SUM(balance) as total FROM " + TABLE_NAME);
        playerCountStatement = connection.prepareStatement(
                "SELECT COUNT(*) as count FROM " + TABLE_NAME);
    }

    public void reloadSettings() {
        restartAutoSave();
    }

    public boolean hasBalance(UUID uuid) {
        if (balanceCache.containsKey(uuid)) {
            return true;
        }

        synchronized (sqlLock) {
            try {
                hasBalanceStatement.setString(1, uuid.toString());
                try (ResultSet rs = hasBalanceStatement.executeQuery()) {
                    return rs.next();
                }
            } catch (SQLException ex) {
                plugin.getLogger().severe("Failed to check balance for " + uuid + ": " + ex.getMessage());
                return false;
            }
        }
    }

    public double getBalance(UUID uuid) {
        return balanceCache.computeIfAbsent(uuid, this::loadBalance);
    }

    private double loadBalance(UUID uuid) {
        synchronized (sqlLock) {
            try {
                selectBalanceStatement.setString(1, uuid.toString());
                try (ResultSet rs = selectBalanceStatement.executeQuery()) {
                    if (rs.next()) {
                        return rs.getDouble("balance");
                    }
                }
            } catch (SQLException ex) {
                plugin.getLogger().severe("Failed to load balance for " + uuid + ": " + ex.getMessage());
            }
        }

        double defaultBalance = Config.DEFAULT_BALANCE;
        queuePendingWrite(uuid, defaultBalance);
        return defaultBalance;
    }

    public EconomyResult deposit(UUID uuid, double amount) {
        if (amount <= 0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
            return EconomyResult.invalidAmount();
        }

        double newBalance;
        synchronized (sqlLock) {
            double current = getBalance(uuid);
            newBalance = current + amount;

            if (newBalance > Config.MAX_BALANCE) {
                return new EconomyResult(EconomyStatus.EXCEEDS_MAX_BALANCE, current);
            }

            balanceCache.put(uuid, newBalance);
        }

        queuePendingWrite(uuid, newBalance);
        return new EconomyResult(EconomyStatus.SUCCESS, newBalance);
    }

    public EconomyResult withdraw(UUID uuid, double amount) {
        if (amount <= 0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
            return EconomyResult.invalidAmount();
        }

        double newBalance;
        synchronized (sqlLock) {
            double current = getBalance(uuid);
            newBalance = current - amount;

            if (newBalance < Config.MIN_BALANCE) {
                return new EconomyResult(EconomyStatus.INSUFFICIENT_FUNDS, current);
            }

            balanceCache.put(uuid, newBalance);
        }

        queuePendingWrite(uuid, newBalance);
        return new EconomyResult(EconomyStatus.SUCCESS, newBalance);
    }

    public EconomyResult transfer(UUID from, UUID to, double amount) {
        if (from.equals(to)) {
            return new EconomyResult(EconomyStatus.SAME_ACCOUNT, Double.NaN);
        }

        if (amount <= 0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
            return EconomyResult.invalidAmount();
        }

        synchronized (sqlLock) {
            double senderBalance = getBalance(from);
            double receiverBalance = getBalance(to);

            if ((senderBalance - amount) < Config.MIN_BALANCE) {
                return new EconomyResult(EconomyStatus.INSUFFICIENT_FUNDS, senderBalance);
            }

            if ((receiverBalance + amount) > Config.MAX_BALANCE) {
                return new EconomyResult(EconomyStatus.EXCEEDS_MAX_BALANCE, receiverBalance);
            }

            double newSenderBalance = senderBalance - amount;
            double newReceiverBalance = receiverBalance + amount;

            try {
                executeInTransaction(() -> {
                    writeBalanceImmediately(from, newSenderBalance);
                    writeBalanceImmediately(to, newReceiverBalance);
                });
            } catch (SQLException ex) {
                plugin.getLogger().severe("Failed to execute transactional transfer: " + ex.getMessage());
                return new EconomyResult(EconomyStatus.DATABASE_ERROR, senderBalance);
            }

            balanceCache.put(from, newSenderBalance);
            balanceCache.put(to, newReceiverBalance);
            removePendingWrite(from);
            removePendingWrite(to);
        }

        return new EconomyResult(EconomyStatus.SUCCESS, Double.NaN);
    }

    public EconomyResult setBalance(UUID uuid, double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount)) {
            return EconomyResult.invalidAmount();
        }

        if (amount < Config.MIN_BALANCE) {
            return new EconomyResult(EconomyStatus.BELOW_MIN_BALANCE, amount);
        }

        if (amount > Config.MAX_BALANCE) {
            return new EconomyResult(EconomyStatus.EXCEEDS_MAX_BALANCE, amount);
        }

        synchronized (sqlLock) {
            balanceCache.put(uuid, amount);
        }

        queuePendingWrite(uuid, amount);
        return new EconomyResult(EconomyStatus.SUCCESS, amount);
    }

    private void writeBalanceImmediately(UUID uuid, double amount) throws SQLException {
        upsertBalanceStatement.setString(1, uuid.toString());
        upsertBalanceStatement.setDouble(2, amount);
        upsertBalanceStatement.executeUpdate();
    }

    public Map<UUID, Double> getTopBalances(int limit) {
        flushPendingWrites();

        Map<UUID, Double> top = new LinkedHashMap<>();
        synchronized (sqlLock) {
            try {
                topBalancesStatement.setInt(1, limit);
                try (ResultSet rs = topBalancesStatement.executeQuery()) {
                    while (rs.next()) {
                        try {
                            UUID uuid = UUID.fromString(rs.getString("uuid"));
                            top.put(uuid, rs.getDouble("balance"));
                        } catch (IllegalArgumentException ignored) {
                            // Skip malformed UUIDs but keep going
                        }
                    }
                }
            } catch (SQLException ex) {
                plugin.getLogger().severe("Failed to fetch top balances: " + ex.getMessage());
            }
        }
        return top;
    }

    public void deleteBalance(UUID uuid) {
        balanceCache.remove(uuid);
        removePendingWrite(uuid);

        synchronized (sqlLock) {
            try {
                deleteBalanceStatement.setString(1, uuid.toString());
                deleteBalanceStatement.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().severe("Failed to delete balance for " + uuid + ": " + ex.getMessage());
            }
        }
    }

    public void evictFromCache(UUID uuid) {
        balanceCache.remove(uuid);
    }

    public double getTotalEconomy() {
        flushPendingWrites();

        synchronized (sqlLock) {
            try (ResultSet rs = totalEconomyStatement.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total");
                }
            } catch (SQLException ex) {
                plugin.getLogger().severe("Failed to calculate total economy: " + ex.getMessage());
            }
        }
        return 0.0;
    }

    public int getPlayerCount() {
        flushPendingWrites();

        synchronized (sqlLock) {
            try (ResultSet rs = playerCountStatement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            } catch (SQLException ex) {
                plugin.getLogger().severe("Failed to count players: " + ex.getMessage());
            }
        }
        return 0;
    }

    public void flushPendingWrites() {
        Map<UUID, Double> snapshot;
        synchronized (pendingLock) {
            if (pendingWrites.isEmpty()) {
                return;
            }
            snapshot = new HashMap<>(pendingWrites);
            pendingWrites.clear();
        }

        synchronized (sqlLock) {
            try {
                executeInTransaction(() -> {
                    for (Map.Entry<UUID, Double> entry : snapshot.entrySet()) {
                        writeBalanceImmediately(entry.getKey(), entry.getValue());
                    }
                });
            } catch (SQLException ex) {
                plugin.getLogger().severe("Failed to flush balances: " + ex.getMessage());
                synchronized (pendingLock) {
                    snapshot.forEach((uuid, value) -> pendingWrites.putIfAbsent(uuid, value));
                }
            }
        }
    }

    private void queuePendingWrite(UUID uuid, double amount) {
        synchronized (pendingLock) {
            pendingWrites.put(uuid, amount);
        }

        if (Config.SAVE_INTERVAL_TICKS <= 0) {
            flushPendingWrites();
        }
    }

    private void removePendingWrite(UUID uuid) {
        synchronized (pendingLock) {
            pendingWrites.remove(uuid);
        }
    }

    private void scheduleAutoSave() {
        long interval = Config.SAVE_INTERVAL_TICKS;
        if (interval <= 0) {
            return;
        }

        autoSaveTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
                plugin,
                this::flushPendingWrites,
                interval,
                interval
        );
    }

    private void restartAutoSave() {
        cancelAutoSaveTask();
        scheduleAutoSave();
    }

    private void cancelAutoSaveTask() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
    }

    public void shutdown() {
        cancelAutoSaveTask();
        flushPendingWrites();

        synchronized (sqlLock) {
            closeStatement(selectBalanceStatement);
            closeStatement(hasBalanceStatement);
            closeStatement(upsertBalanceStatement);
            closeStatement(deleteBalanceStatement);
            closeStatement(topBalancesStatement);
            closeStatement(totalEconomyStatement);
            closeStatement(playerCountStatement);

            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ex) {
                    plugin.getLogger().severe("Failed to close database connection: " + ex.getMessage());
                }
            }
        }
    }

    private void closeStatement(PreparedStatement statement) {
        if (statement == null) {
            return;
        }
        try {
            statement.close();
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed to close statement: " + ex.getMessage());
        }
    }

    private void executeInTransaction(SQLRunnable action) throws SQLException {
        boolean previousAutoCommit = connection.getAutoCommit();
        if (previousAutoCommit) {
            connection.setAutoCommit(false);
        }

        try {
            action.run();
            connection.commit();
        } catch (SQLException ex) {
            connection.rollback();
            throw ex;
        } finally {
            if (previousAutoCommit) {
                connection.setAutoCommit(true);
            }
        }
    }

    @FunctionalInterface
    private interface SQLRunnable {
        void run() throws SQLException;
    }
}