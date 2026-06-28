package com.nocrates.storage;

import com.nocrates.NoCrates;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * MySQL/MariaDB-backed {@link DataStore} (via HikariCP) for cross-server
 * networks. All player state lives in one {@code nocrates_data} table keyed by
 * {@code (uuid, category, entry)}, mirroring the in-memory maps.
 */
public final class MySqlDataStore implements DataStore {

    private final NoCrates plugin;
    private final HikariDataSource source;

    public MySqlDataStore(NoCrates plugin, ConfigurationSection cfg) {
        this.plugin = plugin;
        HikariConfig hikari = new HikariConfig();
        String host = cfg.getString("host", "localhost");
        int port = cfg.getInt("port", 3306);
        String database = cfg.getString("database", "nocrates");
        boolean ssl = cfg.getBoolean("use-ssl", false);
        hikari.setDriverClassName("org.mariadb.jdbc.Driver");
        hikari.setJdbcUrl("jdbc:mariadb://" + host + ":" + port + "/" + database + "?useSsl=" + ssl);
        hikari.setUsername(cfg.getString("username", "root"));
        hikari.setPassword(cfg.getString("password", ""));
        hikari.setMaximumPoolSize(Math.max(1, cfg.getInt("pool-size", 8)));
        hikari.setPoolName("noCrates");
        this.source = new HikariDataSource(hikari);
        createTable();
    }

    private void createTable() {
        try (Connection c = source.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS nocrates_data ("
                             + "uuid VARCHAR(36) NOT NULL,"
                             + "category VARCHAR(16) NOT NULL,"
                             + "entry VARCHAR(160) NOT NULL,"
                             + "amount INT NOT NULL,"
                             + "PRIMARY KEY (uuid, category, entry))")) {
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create MySQL table", e);
        }
    }

    @Override
    public CompletableFuture<PlayerData> load(UUID id) {
        return CompletableFuture.supplyAsync(() -> loadSync(id));
    }

    private PlayerData loadSync(UUID id) {
        PlayerData data = new PlayerData(id);
        try (Connection c = source.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT category, entry, amount FROM nocrates_data WHERE uuid=?")) {
            ps.setString(1, id.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String entry = rs.getString("entry");
                    int amount = rs.getInt("amount");
                    switch (rs.getString("category")) {
                        case "keys" -> data.rawKeys().put(entry, amount);
                        case "opens" -> data.rawOpens().put(entry, amount);
                        case "wins" -> data.rawWins().put(entry, amount);
                        default -> {
                        }
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load player data from MySQL", e);
        }
        return data;
    }

    @Override
    public void save(PlayerData data) {
        try (Connection c = source.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement delete = c.prepareStatement("DELETE FROM nocrates_data WHERE uuid=?")) {
                delete.setString(1, data.uuid().toString());
                delete.executeUpdate();
            }
            try (PreparedStatement insert = c.prepareStatement(
                    "INSERT INTO nocrates_data (uuid, category, entry, amount) VALUES (?,?,?,?)")) {
                batch(insert, data.uuid(), "keys", data.rawKeys());
                batch(insert, data.uuid(), "opens", data.rawOpens());
                batch(insert, data.uuid(), "wins", data.rawWins());
                insert.executeBatch();
            }
            c.commit();
            data.markClean();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save player data to MySQL", e);
        }
    }

    private void batch(PreparedStatement insert, UUID id, String category, Map<String, Integer> map) throws Exception {
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            insert.setString(1, id.toString());
            insert.setString(2, category);
            insert.setString(3, entry.getKey());
            insert.setInt(4, entry.getValue());
            insert.addBatch();
        }
    }

    @Override
    public void close() {
        if (source != null && !source.isClosed()) {
            source.close();
        }
    }
}
