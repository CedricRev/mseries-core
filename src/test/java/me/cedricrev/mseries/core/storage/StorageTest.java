package me.cedricrev.mseries.core.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StorageTest {

    private static final Logger LOGGER = Logger.getLogger("StorageTest");
    private static final List<Migration> MIGRATIONS = List.of(
            new Migration(1, "init", "CREATE TABLE ${prefix}players (uuid VARCHAR(36) PRIMARY KEY, votes INTEGER NOT NULL);"),
            new Migration(2, "add name", "ALTER TABLE ${prefix}players ADD COLUMN name VARCHAR(16);"));

    @TempDir
    File folder;

    private Storage open(List<Migration> migrations) {
        StorageSettings settings = new StorageSettings(StorageType.SQLITE, "data.db", "localhost", 3306, "minecraft",
                "root", "", 4, Map.of(), "mt_");
        return Storage.open("Test", this.folder, LOGGER, settings, List.of("players"), dialect -> migrations);
    }

    @Test
    void appliesMigrationsOnceWithThePrefix() throws Exception {
        try (Storage storage = open(MIGRATIONS)) {
            assertTrue(storage.isEmpty().join());
            storage.transaction(connection -> insert(connection, "a", 3, "Steve")).join();
            assertFalse(storage.isEmpty().join());
        }
        try (Storage storage = open(MIGRATIONS)) {
            int version = storage.query(connection -> new Migrator("mt_", LOGGER).currentVersion(connection)).join();
            assertEquals(2, version);
            String name = storage.query(connection -> {
                try (Statement statement = connection.createStatement();
                     ResultSet result = statement.executeQuery("SELECT name FROM " + storage.table("players"))) {
                    return result.next() ? result.getString(1) : null;
                }
            }).join();
            assertEquals("Steve", name);
        }
    }

    @Test
    void failedTransactionLeavesNothingBehind() {
        try (Storage storage = open(MIGRATIONS)) {
            CompletionException error = assertThrows(CompletionException.class, () -> storage.transaction(connection -> {
                insert(connection, "a", 1, "Alex");
                throw new SQLException("boom");
            }).join());
            assertInstanceOf(StorageException.class, error.getCause());
            assertTrue(storage.isEmpty().join());
        }
    }

    @Test
    void refusesDatabaseFromNewerPlugin() {
        open(MIGRATIONS).close();

        StorageException error = assertThrows(StorageException.class, () -> open(MIGRATIONS.subList(0, 1)));

        assertTrue(error.getMessage().contains("schema version 2"));
    }

    @Test
    void brokenMigrationIsRolledBack() {
        List<Migration> broken = List.of(MIGRATIONS.get(0), new Migration(2, "broken", "CREATE TABLE ${prefix}x (id INTEGER); NOT SQL;"));

        assertThrows(StorageException.class, () -> open(broken));

        try (Storage storage = open(MIGRATIONS.subList(0, 1))) {
            int version = storage.query(connection -> new Migrator("mt_", LOGGER).currentVersion(connection)).join();
            assertEquals(1, version);
        }
    }

    @Test
    void refusesToMigrateBetweenSameDialect() {
        try (Storage storage = open(MIGRATIONS)) {
            assertThrows(CompletionException.class, () -> storage.migrate(StorageType.MYSQL, StorageType.MARIADB).join());
        }
    }

    private static int insert(java.sql.Connection connection, String uuid, int votes, String name) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO mt_players (uuid, votes, name) VALUES (?, ?, ?)")) {
            statement.setString(1, uuid);
            statement.setInt(2, votes);
            statement.setString(3, name);
            return statement.executeUpdate();
        }
    }
}
