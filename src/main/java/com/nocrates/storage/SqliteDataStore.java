package com.nocrates.storage;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * SQLite backend using the driver bundled with the server. Availability is checked via
 * {@link #driverPresent()} before construction; single shared connection (SQLite is
 * single-writer anyway) guarded by the store's single IO thread.
 */
public final class SqliteDataStore extends SqlDataStore {

    private Connection connection;

    public SqliteDataStore(Plugin plugin, String prefix) throws SQLException {
        super(plugin, prefix, false, 1);
        File db = new File(plugin.getDataFolder(), "data.db");
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + db.getAbsolutePath());
        createTables();
    }

    public static boolean driverPresent() {
        try {
            Class.forName("org.sqlite.JDBC");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    protected Connection connection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            File db = new File(plugin.getDataFolder(), "data.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + db.getAbsolutePath());
        }
        return connection;
    }

    @Override
    protected boolean closeConnections() {
        return false;
    }

    @Override
    protected void closeBackend() {
        try {
            if (connection != null) connection.close();
        } catch (SQLException ignored) {
        }
    }
}
