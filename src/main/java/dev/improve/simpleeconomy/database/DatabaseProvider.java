package dev.improve.simpleeconomy.database;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

/**
 * Interface for database storage backends.
 * Implementations handle connection management and SQL execution.
 */
public interface DatabaseProvider {

    /**
     * Initialize the database connection and create tables if needed.
     */
    void initialize() throws SQLException;

    /**
     * Shutdown the database connection and release resources.
     */
    void shutdown();

    /**
     * Check if a player has a balance record in the database.
     */
    boolean hasBalance(UUID uuid) throws SQLException;

    /**
     * Load a player's balance from the database.
     * @return the balance, or null if not found
     */
    Double loadBalance(UUID uuid) throws SQLException;

    /**
     * Save a player's balance to the database (insert or update).
     */
    void saveBalance(UUID uuid, double balance) throws SQLException;

    /**
     * Save multiple balances in a single transaction.
     */
    void saveBalances(Map<UUID, Double> balances) throws SQLException;

    /**
     * Delete a player's balance record.
     */
    void deleteBalance(UUID uuid) throws SQLException;

    /**
     * Get the top balances ordered by amount descending.
     */
    Map<UUID, Double> getTopBalances(int limit) throws SQLException;

    /**
     * Get the sum of all balances.
     */
    double getTotalBalance() throws SQLException;

    /**
     * Get the count of all balance records.
     */
    int getPlayerCount() throws SQLException;

    /**
     * Execute a transfer between two accounts atomically.
     */
    void executeTransfer(UUID from, double fromBalance, UUID to, double toBalance) throws SQLException;

    /**
     * Get the name of this provider for logging purposes.
     */
    String getName();
}
