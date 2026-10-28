package me.cedricrev.mseries.core.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.logging.Logger;

final class Migrator {

    private final String historyTable;
    private final String prefix;
    private final Logger logger;

    Migrator(String prefix, Logger logger) {
        this.prefix = prefix;
        this.historyTable = prefix + "schema_history";
        this.logger = logger;
    }

    int migrate(Connection connection, List<Migration> migrations) throws SQLException {
        boolean autoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(true);
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE IF NOT EXISTS " + this.historyTable
                        + " (version INTEGER NOT NULL PRIMARY KEY, description VARCHAR(200) NOT NULL,"
                        + " installed_at BIGINT NOT NULL)");
            }
            int current = currentVersion(connection);
            int latest = migrations.isEmpty() ? 0 : migrations.get(migrations.size() - 1).version();
            if (current > latest) {
                throw new StorageException("The database is at schema version " + current
                        + ", but this plugin only knows up to " + latest + ". Update the plugin or restore a backup.");
            }
            int applied = 0;
            for (Migration migration : migrations) {
                if (migration.version() <= current) {
                    continue;
                }
                apply(connection, migration);
                applied++;
            }
            return applied;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }

    int currentVersion(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT MAX(version) FROM " + this.historyTable)) {
            return result.next() ? result.getInt(1) : 0;
        }
    }

    private void apply(Connection connection, Migration migration) throws SQLException {
        connection.setAutoCommit(false);
        try {
            for (String sql : SqlScript.statements(migration.sql().replace("${prefix}", this.prefix))) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(sql);
                }
            }
            try (PreparedStatement insert = connection.prepareStatement("INSERT INTO " + this.historyTable
                    + " (version, description, installed_at) VALUES (?, ?, ?)")) {
                insert.setInt(1, migration.version());
                insert.setString(2, migration.description());
                insert.setLong(3, System.currentTimeMillis());
                insert.executeUpdate();
            }
            connection.commit();
            this.logger.info("Database updated to version " + migration.version() + " (" + migration.description() + ")");
        } catch (SQLException | RuntimeException e) {
            connection.rollback();
            throw new StorageException("Database migration V" + migration.version() + " ("
                    + migration.description() + ") failed: " + e.getMessage(), e);
        } finally {
            connection.setAutoCommit(true);
        }
    }
}
