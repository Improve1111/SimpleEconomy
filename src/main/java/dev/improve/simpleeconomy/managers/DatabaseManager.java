package dev.improve.simpleeconomy.managers;

import dev.improve.simpleeconomy.SimpleEconomy;
import dev.improve.simpleeconomy.database.DatabaseProvider;
import dev.improve.simpleeconomy.database.MySQLProvider;
import dev.improve.simpleeconomy.database.SQLiteProvider;
import dev.improve.simpleeconomy.utils.Config;
import org.bukkit.scheduler.BukkitTask;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseManager {

    private final SimpleEconomy plugin;
    private final Map<UUID, Double> balanceCache = new ConcurrentHashMap<>();
    private final Object dbLock = new Object();
    private final Object pendingLock = new Object();
    private final Map<UUID, Double> pendingWrites = new HashMap<>();

    private DatabaseProvider provider;
    private BukkitTask autoSaveTask;
    private ExecutorService asyncExecutor;

    public DatabaseManager(SimpleEconomy plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        try {
            asyncExecutor = Executors.newFixedThreadPool(2, r -> {
                Thread t = new Thread(r, "SimpleEconomy-DB");
                t.setDaemon(true);
                return t;
            });
            provider = createProvider();
            provider.initialize();
            plugin.getLogger().info("Database connected using " + provider.getName() + ".");
            scheduleAutoSave();
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed to initialise database connection: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private DatabaseProvider createProvider() {
        return switch (Config.DATABASE_TYPE) {
            case "mysql", "mariadb" -> new MySQLProvider(
                    Config.MYSQL_HOST,
                    Config.MYSQL_PORT,
                    Config.MYSQL_DATABASE,
                    Config.MYSQL_USERNAME,
                    Config.MYSQL_PASSWORD,
                    Config.MYSQL_POOL_SIZE
            );
            default -> new SQLiteProvider(plugin.getDataFolder());
        };
    }

    public void reloadSettings() {
        restartAutoSave();
    }

    public boolean hasBalance(UUID uuid) {
        if (balanceCache.containsKey(uuid)) {
            return true;
        }

        synchronized (dbLock) {
            try {
                return provider.hasBalance(uuid);
            } catch (SQLException ex) {
                plugin.getLogger().severe("Failed to check balance for " + uuid + ": " + ex.getMessage());
                return false;
            }
        }
    }

    public double getBalance(UUID uuid) {
        Double cached = balanceCache.get(uuid);
        if (cached != null) {
            return cached;
        }
        // Synchronous fallback for Vault compatibility
        return loadBalance(uuid);
    }

    /**
     * Asynchronously loads and caches a player's balance.
     * Use this for preloading on join to avoid main thread DB access.
     */
    public CompletableFuture<Double> loadBalanceAsync(UUID uuid) {
        Double cached = balanceCache.get(uuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return CompletableFuture.supplyAsync(() -> {
            double balance = loadBalance(uuid);
            balanceCache.put(uuid, balance);
            return balance;
        }, asyncExecutor);
    }

    private double loadBalance(UUID uuid) {
        synchronized (dbLock) {
            try {
                Double balance = provider.loadBalance(uuid);
                if (balance != null) {
                    return balance;
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
        synchronized (dbLock) {
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
        synchronized (dbLock) {
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

        synchronized (dbLock) {
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
                provider.executeTransfer(from, newSenderBalance, to, newReceiverBalance);
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

        synchronized (dbLock) {
            balanceCache.put(uuid, amount);
        }

        queuePendingWrite(uuid, amount);
        return new EconomyResult(EconomyStatus.SUCCESS, amount);
    }

    public Map<UUID, Double> getTopBalances(int limit) {
        flushPendingWrites();

        synchronized (dbLock) {
            try {
                return provider.getTopBalances(limit);
            } catch (SQLException ex) {
                plugin.getLogger().severe("Failed to fetch top balances: " + ex.getMessage());
                return Map.of();
            }
        }
    }

    public void deleteBalance(UUID uuid) {
        balanceCache.remove(uuid);
        removePendingWrite(uuid);

        synchronized (dbLock) {
            try {
                provider.deleteBalance(uuid);
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

        synchronized (dbLock) {
            try {
                return provider.getTotalBalance();
            } catch (SQLException ex) {
                plugin.getLogger().severe("Failed to calculate total economy: " + ex.getMessage());
                return 0.0;
            }
        }
    }

    public int getPlayerCount() {
        flushPendingWrites();

        synchronized (dbLock) {
            try {
                return provider.getPlayerCount();
            } catch (SQLException ex) {
                plugin.getLogger().severe("Failed to count players: " + ex.getMessage());
                return 0;
            }
        }
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

        synchronized (dbLock) {
            try {
                provider.saveBalances(snapshot);
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

        if (asyncExecutor != null) {
            asyncExecutor.shutdown();
        }

        synchronized (dbLock) {
            if (provider != null) {
                provider.shutdown();
            }
        }
    }

    /**
     * Get the async executor for database operations.
     */
    public ExecutorService getAsyncExecutor() {
        return asyncExecutor;
    }
}
