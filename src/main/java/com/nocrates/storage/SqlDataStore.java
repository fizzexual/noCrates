package com.nocrates.storage;

import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Shared implementation for the SQL backends; subclasses only supply connections and
 * the dialect flag. Player saves are transactional delete+insert (data volumes here
 * are tiny), globals use dialect upserts.
 */
public abstract class SqlDataStore implements DataStore {

    private static final String[] INT_SCOPES = {"keys", "opens", "wins", "milestones", "rerolls"};
    private static final String[] LONG_SCOPES = {"cooldowns", "win-cooldowns"};

    protected final Plugin plugin;
    protected final String prefix;
    private final boolean mysql;
    private final ExecutorService io;

    protected SqlDataStore(Plugin plugin, String prefix, boolean mysql, int threads) {
        this.plugin = plugin;
        this.prefix = prefix;
        this.mysql = mysql;
        this.io = Executors.newFixedThreadPool(Math.max(1, threads), r -> {
            Thread t = new Thread(r, "noCrates-sql");
            t.setDaemon(true);
            return t;
        });
    }

    /** Borrow a connection; SQLite returns a shared one, MySQL borrows from the pool. */
    protected abstract Connection connection() throws SQLException;

    /** Whether {@link #connection()} results should be closed after use. */
    protected abstract boolean closeConnections();

    protected void createTables() {
        try (Borrowed b = open()) {
            for (String ddl : SqlStatements.createTables(prefix, mysql)) {
                try (Statement st = b.connection.createStatement()) {
                    st.execute(ddl);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not create tables: " + e.getMessage());
        }
    }

    /** Tiny helper making try-with-resources honor closeConnections(). */
    private final class Borrowed implements AutoCloseable {
        final Connection connection;

        Borrowed(Connection connection) {
            this.connection = connection;
        }

        @Override
        public void close() throws SQLException {
            if (closeConnections()) connection.close();
        }
    }

    private Borrowed open() throws SQLException {
        return new Borrowed(connection());
    }

    @Override
    public CompletableFuture<PlayerData> load(UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerData data = new PlayerData(id);
            try (Borrowed b = open()) {
                try (PreparedStatement ps = b.connection.prepareStatement(
                        "SELECT scope, k, v FROM " + prefix + "ints WHERE uuid = ?")) {
                    ps.setString(1, id.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) data.loadInt(rs.getString(1), rs.getString(2), rs.getInt(3));
                    }
                }
                try (PreparedStatement ps = b.connection.prepareStatement(
                        "SELECT scope, k, v FROM " + prefix + "longs WHERE uuid = ?")) {
                    ps.setString(1, id.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) data.loadLong(rs.getString(1), rs.getString(2), rs.getLong(3));
                    }
                }
                try (PreparedStatement ps = b.connection.prepareStatement(
                        "SELECT row FROM " + prefix + "claims WHERE uuid = ?")) {
                    ps.setString(1, id.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) data.loadClaim(rs.getString(1));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Could not load " + id + ": " + e.getMessage());
            }
            return data;
        }, io);
    }

    @Override
    public void saveAsync(PlayerData data) {
        io.submit(() -> saveSync(data));
    }

    @Override
    public void saveSync(PlayerData data) {
        String id = data.id().toString();
        try (Borrowed b = open()) {
            Connection con = b.connection;
            boolean auto = con.getAutoCommit();
            con.setAutoCommit(false);
            try {
                for (String table : new String[]{"ints", "longs", "claims"}) {
                    try (PreparedStatement ps = con.prepareStatement(
                            "DELETE FROM " + prefix + table + " WHERE uuid = ?")) {
                        ps.setString(1, id);
                        ps.executeUpdate();
                    }
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO " + prefix + "ints (uuid, scope, k, v) VALUES (?,?,?,?)")) {
                    for (String scope : INT_SCOPES) {
                        for (Map.Entry<String, Integer> e : data.rawInts(scope).entrySet()) {
                            ps.setString(1, id);
                            ps.setString(2, scope);
                            ps.setString(3, e.getKey());
                            ps.setInt(4, e.getValue());
                            ps.addBatch();
                        }
                    }
                    ps.executeBatch();
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO " + prefix + "longs (uuid, scope, k, v) VALUES (?,?,?,?)")) {
                    for (String scope : LONG_SCOPES) {
                        for (Map.Entry<String, Long> e : data.rawLongs(scope).entrySet()) {
                            ps.setString(1, id);
                            ps.setString(2, scope);
                            ps.setString(3, e.getKey());
                            ps.setLong(4, e.getValue());
                            ps.addBatch();
                        }
                    }
                    ps.executeBatch();
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO " + prefix + "claims (uuid, row) VALUES (?,?)")) {
                    for (String row : data.claims()) {
                        ps.setString(1, id);
                        ps.setString(2, row);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                con.commit();
                data.clearDirty();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(auto);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Could not save " + id + ": " + e.getMessage());
        }
    }

    @Override
    public CompletableFuture<Map<String, Integer>> globalWins(String crateId) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Integer> out = new HashMap<>();
            try (Borrowed b = open(); PreparedStatement ps = b.connection.prepareStatement(
                    "SELECT reward, count FROM " + prefix + "global_wins WHERE crate = ?")) {
                ps.setString(1, crateId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) out.put(rs.getString(1), rs.getInt(2));
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("globalWins failed: " + e.getMessage());
            }
            return out;
        }, io);
    }

    @Override
    public CompletableFuture<Map<String, Long>> globalWinCooldowns(String crateId) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Long> out = new HashMap<>();
            try (Borrowed b = open(); PreparedStatement ps = b.connection.prepareStatement(
                    "SELECT reward, cooldown_until FROM " + prefix + "global_wins WHERE crate = ? AND cooldown_until > 0")) {
                ps.setString(1, crateId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) out.put(rs.getString(1), rs.getLong(2));
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("globalWinCooldowns failed: " + e.getMessage());
            }
            return out;
        }, io);
    }

    @Override
    public void setGlobalWin(String crateId, String rewardId, int count, long cooldownUntilEpochSec) {
        io.submit(() -> {
            try (Borrowed b = open(); PreparedStatement ps = b.connection.prepareStatement(
                    SqlStatements.upsertGlobalWin(prefix, mysql))) {
                ps.setString(1, crateId);
                ps.setString(2, rewardId);
                ps.setInt(3, count);
                ps.setLong(4, Math.max(0, cooldownUntilEpochSec));
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("setGlobalWin failed: " + e.getMessage());
            }
        });
    }

    @Override
    public void resetGlobalWins(String crateId) {
        io.submit(() -> {
            try (Borrowed b = open(); PreparedStatement ps = b.connection.prepareStatement(
                    "DELETE FROM " + prefix + "global_wins WHERE crate = ?")) {
                ps.setString(1, crateId);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("resetGlobalWins failed: " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<List<String>> lastWinners(String crateId, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> out = new ArrayList<>();
            try (Borrowed b = open(); PreparedStatement ps = b.connection.prepareStatement(
                    "SELECT row FROM " + prefix + "last_winners WHERE crate = ? ORDER BY at DESC LIMIT " + Math.max(1, limit))) {
                ps.setString(1, crateId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) out.add(rs.getString(1));
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("lastWinners failed: " + e.getMessage());
            }
            return out;
        }, io);
    }

    @Override
    public void pushWinner(String crateId, String row, int keep) {
        io.submit(() -> {
            try (Borrowed b = open()) {
                try (PreparedStatement ps = b.connection.prepareStatement(
                        "INSERT INTO " + prefix + "last_winners (crate, at, row) VALUES (?,?,?)")) {
                    ps.setString(1, crateId);
                    ps.setLong(2, System.nanoTime());
                    ps.setString(3, row);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = b.connection.prepareStatement(
                        "DELETE FROM " + prefix + "last_winners WHERE crate = ? AND at NOT IN "
                                + "(SELECT at FROM (SELECT at FROM " + prefix + "last_winners WHERE crate = ? ORDER BY at DESC LIMIT " + Math.max(1, keep) + ") keeprows)")) {
                    ps.setString(1, crateId);
                    ps.setString(2, crateId);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("pushWinner failed: " + e.getMessage());
            }
        });
    }

    @Override
    public void close() {
        io.shutdown();
        try {
            io.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        closeBackend();
    }

    protected abstract void closeBackend();
}
