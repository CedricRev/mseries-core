package me.cedricrev.mseries.core.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;

public final class Storage implements AutoCloseable {

    private static final int COPY_BATCH = 500;
    private static final long TIMEOUT_MS = 10_000;
    private static final long PROBE_TIMEOUT_MS = 3_000;

    private final String owner;
    private final File dataFolder;
    private final Logger logger;
    private final StorageSettings settings;
    private final List<String> tables;
    private final Function<String, List<Migration>> migrations;
    private final HikariDataSource dataSource;
    private final ExecutorService executor;

    private Storage(String owner, File dataFolder, Logger logger, StorageSettings settings, List<String> tables,
                    Function<String, List<Migration>> migrations) {
        this.owner = owner;
        this.dataFolder = dataFolder;
        this.logger = logger;
        this.settings = settings;
        this.tables = List.copyOf(tables);
        this.migrations = migrations;
        this.dataSource = new HikariDataSource(config());
        int threads = settings.type().isSqlite() ? 1 : settings.poolSize();
        this.executor = Executors.newFixedThreadPool(threads, threads(owner + "-Storage"));
    }

    public static Storage open(Plugin plugin, StorageSettings settings, String... tables) {
        Class<?> owner = plugin.getClass();
        return open(plugin.getName(), plugin.getDataFolder(), plugin.getLogger(), settings, List.of(tables),
                dialect -> Migration.fromJar(owner, dialect));
    }

    static Storage open(String owner, File dataFolder, Logger logger, StorageSettings settings, List<String> tables,
                        Function<String, List<Migration>> migrations) {
        Storage storage = create(owner, dataFolder, logger, settings, tables, migrations);
        try {
            storage.runMigrations();
            return storage;
        } catch (RuntimeException e) {
            storage.close();
            throw e;
        }
    }

    private static Storage create(String owner, File dataFolder, Logger logger, StorageSettings settings,
                                  List<String> tables, Function<String, List<Migration>> migrations) {
        try {
            return new Storage(owner, dataFolder, logger, settings, tables, migrations);
        } catch (RuntimeException e) {
            throw new StorageException("Cannot connect to the " + settings.type().id() + " database: "
                    + rootMessage(e), e);
        }
    }

    public <T> CompletableFuture<T> query(SqlWork<T> work) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = this.dataSource.getConnection()) {
                return work.apply(connection);
            } catch (SQLException e) {
                throw new CompletionException(new StorageException("Database query failed: " + e.getMessage(), e));
            }
        }, this.executor);
    }

    public <T> CompletableFuture<T> transaction(SqlWork<T> work) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = this.dataSource.getConnection()) {
                return inTransaction(connection, work);
            } catch (SQLException e) {
                throw new CompletionException(new StorageException("Database transaction failed: " + e.getMessage(), e));
            }
        }, this.executor);
    }

    public String table(String name) {
        return this.settings.tablePrefix() + name;
    }

    public StorageType type() {
        return this.settings.type();
    }

    public StorageSettings settings() {
        return this.settings;
    }

    public List<String> tables() {
        return this.tables;
    }

    public CompletableFuture<Boolean> isEmpty() {
        return query(this::countRows).thenApply(total -> total == 0);
    }

    public CompletableFuture<Map<String, Long>> migrate(StorageType from, StorageType to) {
        if (from.dialect().equals(to.dialect())) {
            return CompletableFuture.failedFuture(new StorageException("Pick two different databases, for example"
                    + " sqlite and mysql"));
        }
        if (from != type() && to != type()) {
            return CompletableFuture.failedFuture(new StorageException("One side must be the database in use ("
                    + type().id() + ")"));
        }
        return CompletableFuture.supplyAsync(() -> {
            boolean intoThis = to == type();
            StorageSettings otherSettings = this.settings.withType(intoThis ? from : to);
            try (Storage other = intoThis
                    ? create(this.owner, this.dataFolder, this.logger, otherSettings, this.tables, this.migrations)
                    : open(this.owner, this.dataFolder, this.logger, otherSettings, this.tables, this.migrations)) {
                return intoThis ? copyFrom(other) : other.copyFrom(this);
            } catch (SQLException e) {
                throw new CompletionException(new StorageException("Copying the database failed: " + e.getMessage(), e));
            }
        }, this.executor);
    }

    public void warnIfSwitched(String migrateCommand) {
        CompletableFuture.runAsync(() -> {
            try {
                if (withConnection(this::countRows) > 0) {
                    return;
                }
                for (StorageType other : List.of(StorageType.SQLITE, StorageType.MYSQL)) {
                    if (other.dialect().equals(type().dialect())
                            || other.isSqlite() && !sqliteFile().isFile()) {
                        continue;
                    }
                    StorageSettings otherSettings = this.settings.withType(other);
                    try (Connection probe = probe(otherSettings)) {
                        if (countRows(probe) > 0) {
                            this.logger.warning("The " + type().id() + " database is empty, but the " + other.id()
                                    + " database still has data. To copy it over, run: " + migrateCommand + " "
                                    + other.id() + " " + type().id());
                            return;
                        }
                    } catch (SQLException | RuntimeException ignored) {
                    }
                }
            } catch (SQLException e) {
                this.logger.log(Level.FINE, "Empty database check failed", e);
            }
        }, this.executor);
    }

    @Override
    public void close() {
        this.executor.shutdown();
        try {
            if (!this.executor.awaitTermination(10, TimeUnit.SECONDS)) {
                this.logger.warning("Database tasks were still running at shutdown and were stopped");
                this.executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            this.executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        this.dataSource.close();
    }

    private void runMigrations() {
        try (Connection connection = this.dataSource.getConnection()) {
            int applied = new Migrator(this.settings.tablePrefix(), this.logger)
                    .migrate(connection, this.migrations.apply(type().dialect()));
            if (applied > 0) {
                this.logger.info("Applied " + applied + " database migration" + (applied == 1 ? "" : "s")
                        + " on " + type().id());
            }
        } catch (SQLException e) {
            throw new StorageException("Cannot prepare the " + type().id() + " database: " + e.getMessage(), e);
        }
    }

    private Map<String, Long> copyFrom(Storage source) throws SQLException {
        try (Connection from = source.dataSource.getConnection();
             Connection to = this.dataSource.getConnection()) {
            if (countRows(to) > 0) {
                throw new StorageException("The " + type().id() + " database already has data. Copying only works"
                        + " into an empty database, so nothing was changed.");
            }
            return inTransaction(to, connection -> {
                Map<String, Long> copied = new LinkedHashMap<>();
                for (String name : this.tables) {
                    copied.put(name, copyTable(from, source.table(name), connection, table(name)));
                }
                return copied;
            });
        }
    }

    private static long copyTable(Connection from, String sourceTable, Connection to, String targetTable)
            throws SQLException {
        try (Statement select = from.createStatement();
             ResultSet rows = select.executeQuery("SELECT * FROM " + sourceTable)) {
            ResultSetMetaData meta = rows.getMetaData();
            List<String> columns = new ArrayList<>();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                columns.add(meta.getColumnLabel(i));
            }
            String sql = "INSERT INTO " + targetTable + " (" + String.join(", ", columns) + ") VALUES ("
                    + String.join(", ", columns.stream().map(column -> "?").toList()) + ")";
            long count = 0;
            try (PreparedStatement insert = to.prepareStatement(sql)) {
                while (rows.next()) {
                    for (int i = 1; i <= columns.size(); i++) {
                        insert.setObject(i, rows.getObject(i));
                    }
                    insert.addBatch();
                    if (++count % COPY_BATCH == 0) {
                        insert.executeBatch();
                    }
                }
                insert.executeBatch();
            }
            return count;
        }
    }

    private long countRows(Connection connection) throws SQLException {
        long total = 0;
        for (String name : this.tables) {
            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + table(name))) {
                if (result.next()) {
                    total += result.getLong(1);
                }
            }
        }
        return total;
    }

    private <T> T withConnection(SqlWork<T> work) throws SQLException {
        try (Connection connection = this.dataSource.getConnection()) {
            return work.apply(connection);
        }
    }

    private static <T> T inTransaction(Connection connection, SqlWork<T> work) throws SQLException {
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            T result = work.apply(connection);
            connection.commit();
            return result;
        } catch (SQLException | RuntimeException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }

    private File sqliteFile() {
        return new File(this.dataFolder, this.settings.sqliteFile());
    }

    private HikariConfig config() {
        HikariConfig config = new HikariConfig();
        config.setPoolName(this.owner + "-" + this.settings.type().id());
        config.setConnectionTimeout(TIMEOUT_MS);
        config.setInitializationFailTimeout(TIMEOUT_MS);
        config.setDriverClassName(driver(this.settings.type()));
        config.setJdbcUrl(url(this.settings));
        config.setMaximumPoolSize(this.settings.type().isSqlite() ? 1 : this.settings.poolSize());
        if (!this.settings.type().isSqlite()) {
            config.setUsername(this.settings.username());
            config.setPassword(this.settings.password());
        }
        properties(this.settings).forEach(config::addDataSourceProperty);
        return config;
    }

    private Connection probe(StorageSettings other) throws SQLException {
        try {
            Class.forName(driver(other.type()));
        } catch (ClassNotFoundException e) {
            throw new SQLException("No " + other.type().id() + " driver on this server", e);
        }
        Properties properties = new Properties();
        properties.putAll(properties(other));
        if (!other.type().isSqlite()) {
            properties.setProperty("user", other.username());
            properties.setProperty("password", other.password());
            properties.setProperty("connectTimeout", String.valueOf(PROBE_TIMEOUT_MS));
        }
        return DriverManager.getConnection(url(other), properties);
    }

    private String url(StorageSettings target) {
        if (target.type().isSqlite()) {
            File file = new File(this.dataFolder, target.sqliteFile());
            File parent = file.getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                throw new StorageException("Cannot create the folder " + parent);
            }
            return "jdbc:sqlite:" + file.getAbsolutePath();
        }
        return "jdbc:mysql://" + target.host() + ":" + target.port() + "/" + target.database();
    }

    private static String driver(StorageType type) {
        return type.isSqlite() ? "org.sqlite.JDBC" : "com.mysql.cj.jdbc.Driver";
    }

    private static Map<String, String> properties(StorageSettings target) {
        Map<String, String> properties = new LinkedHashMap<>();
        if (target.type().isSqlite()) {
            properties.put("foreign_keys", "true");
            properties.put("journal_mode", "WAL");
            properties.put("synchronous", "NORMAL");
            properties.put("busy_timeout", "5000");
        } else {
            properties.put("characterEncoding", "utf8");
            properties.put("rewriteBatchedStatements", "true");
            properties.put("cachePrepStmts", "true");
            properties.putAll(target.properties());
        }
        return properties;
    }

    private static ThreadFactory threads(String name) {
        AtomicInteger count = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, name + "-" + count.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    private static String rootMessage(Throwable error) {
        Throwable root = error;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage() == null ? root.getClass().getSimpleName() : root.getMessage();
    }
}
