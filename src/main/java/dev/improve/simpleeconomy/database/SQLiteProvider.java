package dev.improve.simpleeconomy.database;

import java.io.File;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class SQLiteProvider implements DatabaseProvider {

    private static final String TABLE_NAME = "balances";

    private final File dataFolder;
    private Connection connection;
    private PreparedStatement selectBalanceStatement;
    private PreparedStatement hasBalanceStatement;
    private PreparedStatement upsertBalanceStatement;
    private PreparedStatement deleteBalanceStatement;
    private PreparedStatement topBalancesStatement;
    private PreparedStatement totalEconomyStatement;
    private PreparedStatement playerCountStatement;

    public SQLiteProvider(File dataFolder) {
        this.dataFolder = dataFolder;
    }

    @Override
    public void initialize() throws SQLException {
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            throw new SQLException("Unable to create plugin data folder for database.");
        }

        File dbFile = new File(dataFolder, "balances.db");
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

    @Override
    public void shutdown() {
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
            } catch (SQLException ignored) {
            }
        }
    }

    private void closeStatement(PreparedStatement statement) {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException ignored) {
            }
        }
    }

    @Override
    public boolean hasBalance(UUID uuid) throws SQLException {
        hasBalanceStatement.setString(1, uuid.toString());
        try (ResultSet rs = hasBalanceStatement.executeQuery()) {
            return rs.next();
        }
    }

    @Override
    public Double loadBalance(UUID uuid) throws SQLException {
        selectBalanceStatement.setString(1, uuid.toString());
        try (ResultSet rs = selectBalanceStatement.executeQuery()) {
            if (rs.next()) {
                return rs.getDouble("balance");
            }
        }
        return null;
    }

    @Override
    public void saveBalance(UUID uuid, double balance) throws SQLException {
        upsertBalanceStatement.setString(1, uuid.toString());
        upsertBalanceStatement.setDouble(2, balance);
        upsertBalanceStatement.executeUpdate();
    }

    @Override
    public void saveBalances(Map<UUID, Double> balances) throws SQLException {
        boolean previousAutoCommit = connection.getAutoCommit();
        if (previousAutoCommit) {
            connection.setAutoCommit(false);
        }

        try {
            for (Map.Entry<UUID, Double> entry : balances.entrySet()) {
                saveBalance(entry.getKey(), entry.getValue());
            }
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

    @Override
    public void deleteBalance(UUID uuid) throws SQLException {
        deleteBalanceStatement.setString(1, uuid.toString());
        deleteBalanceStatement.executeUpdate();
    }

    @Override
    public Map<UUID, Double> getTopBalances(int limit) throws SQLException {
        Map<UUID, Double> top = new LinkedHashMap<>();
        topBalancesStatement.setInt(1, limit);
        try (ResultSet rs = topBalancesStatement.executeQuery()) {
            while (rs.next()) {
                try {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    top.put(uuid, rs.getDouble("balance"));
                } catch (IllegalArgumentException ignored) {
                    // Skip malformed UUIDs
                }
            }
        }
        return top;
    }

    @Override
    public double getTotalBalance() throws SQLException {
        try (ResultSet rs = totalEconomyStatement.executeQuery()) {
            if (rs.next()) {
                return rs.getDouble("total");
            }
        }
        return 0.0;
    }

    @Override
    public int getPlayerCount() throws SQLException {
        try (ResultSet rs = playerCountStatement.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("count");
            }
        }
        return 0;
    }

    @Override
    public void executeTransfer(UUID from, double fromBalance, UUID to, double toBalance) throws SQLException {
        boolean previousAutoCommit = connection.getAutoCommit();
        if (previousAutoCommit) {
            connection.setAutoCommit(false);
        }

        try {
            saveBalance(from, fromBalance);
            saveBalance(to, toBalance);
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

    @Override
    public String getName() {
        return "SQLite";
    }
}
