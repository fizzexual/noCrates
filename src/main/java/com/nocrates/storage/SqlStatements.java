package com.nocrates.storage;

/** Shared DDL/DML for the SQLite and MySQL stores; {@code {p}} = table prefix. */
final class SqlStatements {

    private SqlStatements() {
    }

    static String[] createTables(String p, boolean mysql) {
        String text = mysql ? "VARCHAR(255)" : "TEXT";
        String uuid = mysql ? "VARCHAR(36)" : "TEXT";
        return new String[]{
                "CREATE TABLE IF NOT EXISTS " + p + "ints (uuid " + uuid + " NOT NULL, scope " + text + " NOT NULL, k " + text + " NOT NULL, v INTEGER NOT NULL, PRIMARY KEY (uuid, scope" + (mysql ? "(64)" : "") + ", k" + (mysql ? "(128)" : "") + "))",
                "CREATE TABLE IF NOT EXISTS " + p + "longs (uuid " + uuid + " NOT NULL, scope " + text + " NOT NULL, k " + text + " NOT NULL, v BIGINT NOT NULL, PRIMARY KEY (uuid, scope" + (mysql ? "(64)" : "") + ", k" + (mysql ? "(128)" : "") + "))",
                "CREATE TABLE IF NOT EXISTS " + p + "claims (uuid " + uuid + " NOT NULL, row " + text + " NOT NULL)",
                "CREATE TABLE IF NOT EXISTS " + p + "global_wins (crate " + text + " NOT NULL, reward " + text + " NOT NULL, count INTEGER NOT NULL DEFAULT 0, cooldown_until BIGINT NOT NULL DEFAULT 0, PRIMARY KEY (crate" + (mysql ? "(96)" : "") + ", reward" + (mysql ? "(96)" : "") + "))",
                "CREATE TABLE IF NOT EXISTS " + p + "last_winners (crate " + text + " NOT NULL, at BIGINT NOT NULL, row " + text + " NOT NULL)"
        };
    }

    /** Atomic +1 on the global win counter. */
    static String incrementGlobalWin(String p, boolean mysql) {
        if (mysql) {
            return "INSERT INTO " + p + "global_wins (crate, reward, count, cooldown_until) VALUES (?,?,1,0) "
                    + "ON DUPLICATE KEY UPDATE count = count + 1";
        }
        return "INSERT INTO " + p + "global_wins (crate, reward, count, cooldown_until) VALUES (?,?,1,0) "
                + "ON CONFLICT(crate, reward) DO UPDATE SET count = count + 1";
    }

    static String upsertGlobalWin(String p, boolean mysql) {
        if (mysql) {
            return "INSERT INTO " + p + "global_wins (crate, reward, count, cooldown_until) VALUES (?,?,?,?) "
                    + "ON DUPLICATE KEY UPDATE count = VALUES(count), cooldown_until = VALUES(cooldown_until)";
        }
        return "INSERT INTO " + p + "global_wins (crate, reward, count, cooldown_until) VALUES (?,?,?,?) "
                + "ON CONFLICT(crate, reward) DO UPDATE SET count = excluded.count, cooldown_until = excluded.cooldown_until";
    }
}
