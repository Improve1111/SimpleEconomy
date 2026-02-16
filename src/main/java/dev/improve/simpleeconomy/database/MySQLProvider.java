package dev.improve.simpleeconomy.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class MySQLProvider implements DatabaseProvider {

    private static final String TABLE_NAME = "simpleeconomy_balances";

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final int poolSize;

    private HikariDataSource dataSource;

    public MySQLProvider(String host, int port, String database, String username, String password, int poolSize) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.poolSize = poolSize;
    }

    @Override
    public void initialize() throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true");
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(poolSize);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setPoolName("SimpleEconomy-Pool");

        dataSource = new HikariDataSource(config);

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS %s (
                        uuid VARCHAR(36) PRIMARY KEY,
                        balance DOUBLE NOT NULL
                    )
                    """.formatted(TABLE_NAME));
        }
    }

    @Override
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public boolean hasBalance(UUID uuid) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT 1 FROM " + TABLE_NAME + " WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public Double loadBalance(UUID uuid) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT balance FROM " + TABLE_NAME + " WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("balance");
                }
            }
        }
        return null;
    }

    @Override
    public void saveBalance(UUID uuid, double balance) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO " + TABLE_NAME + " (uuid, balance) VALUES (?, ?) " +
                             "ON DUPLICATE KEY UPDATE balance = VALUES(balance)")) {
            stmt.setString(1, uuid.toString());
            stmt.setDouble(2, balance);
            stmt.executeUpdate();
        }
    }

    @Override
    public void saveBalances(Map<UUID, Double> balances) throws SQLException {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO " + TABLE_NAME + " (uuid, balance) VALUES (?, ?) " +
                            "ON DUPLICATE KEY UPDATE balance = VALUES(balance)")) {
                for (Map.Entry<UUID, Double> entry : balances.entrySet()) {
                    stmt.setString(1, entry.getKey().toString());
                    stmt.setDouble(2, entry.getValue());
                    stmt.addBatch();
                }
                stmt.executeBatch();
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    @Override
    public void deleteBalance(UUID uuid) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM " + TABLE_NAME + " WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        }
    }

    @Override
    public Map<UUID, Double> getTopBalances(int limit) throws SQLException {
        Map<UUID, Double> top = new LinkedHashMap<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT uuid, balance FROM " + TABLE_NAME + " ORDER BY balance DESC LIMIT ?")) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    try {
                        UUID uuid = UUID.fromString(rs.getString("uuid"));
                        top.put(uuid, rs.getDouble("balance"));
                    } catch (IllegalArgumentException ignored) {
                        // Skip malformed UUIDs
                    }
                }
            }
        }
        return top;
    }

    @Override
    public double getTotalBalance() throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT SUM(balance) as total FROM " + TABLE_NAME);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getDouble("total");
            }
        }
        return 0.0;
    }

    @Override
    public int getPlayerCount() throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT COUNT(*) as count FROM " + TABLE_NAME);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("count");
            }
        }
        return 0;
    }

    @Override
    public void executeTransfer(UUID from, double fromBalance, UUID to, double toBalance) throws SQLException {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO " + TABLE_NAME + " (uuid, balance) VALUES (?, ?) " +
                            "ON DUPLICATE KEY UPDATE balance = VALUES(balance)")) {
                stmt.setString(1, from.toString());
                stmt.setDouble(2, fromBalance);
                stmt.addBatch();

                stmt.setString(1, to.toString());
                stmt.setDouble(2, toBalance);
                stmt.addBatch();

                stmt.executeBatch();
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    @Override
    public String getName() {
        return "MySQL";
    }
}
