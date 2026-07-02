package com.nocrates.storage;

import com.nocrates.core.MainConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.SQLException;

/** MySQL/MariaDB backend via HikariCP for cross-server networks. */
public final class MySqlDataStore extends SqlDataStore {

    private final HikariDataSource pool;

    public MySqlDataStore(Plugin plugin, MainConfig config) {
        super(plugin, config.tablePrefix(), true, config.mysqlInt("pool-size", 8));
        HikariConfig hc = new HikariConfig();
        hc.setPoolName("noCrates");
        hc.setJdbcUrl("jdbc:" + driverProtocol() + "://" + config.mysql("host", "localhost") + ":"
                + config.mysqlInt("port", 3306) + "/" + config.mysql("database", "nocrates")
                + "?useSSL=" + config.mysqlSsl() + "&characterEncoding=utf8");
        hc.setUsername(config.mysql("username", "root"));
        hc.setPassword(config.mysql("password", ""));
        hc.setMaximumPoolSize(Math.max(2, config.mysqlInt("pool-size", 8)));
        hc.setConnectionTimeout(8000);
        String driver = driverClass();
        if (driver != null) hc.setDriverClassName(driver);
        this.pool = new HikariDataSource(hc);
        createTables();
    }

    /** Prefer the shaded MariaDB driver; fall back to a server-provided MySQL driver. */
    private static String driverClass() {
        for (String candidate : new String[]{
                "com.nocrates.lib.mariadb.Driver",
                "org.mariadb.jdbc.Driver",
                "com.mysql.cj.jdbc.Driver"}) {
            try {
                Class.forName(candidate);
                return candidate;
            } catch (ClassNotFoundException ignored) {
            }
        }
        return null;
    }

    private static String driverProtocol() {
        String driver = driverClass();
        return driver != null && driver.contains("mariadb") ? "mariadb" : "mysql";
    }

    @Override
    protected Connection connection() throws SQLException {
        return pool.getConnection();
    }

    @Override
    protected boolean closeConnections() {
        return true;
    }

    @Override
    protected void closeBackend() {
        pool.close();
    }
}
