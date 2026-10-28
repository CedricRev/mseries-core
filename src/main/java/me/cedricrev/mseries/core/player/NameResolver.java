package me.cedricrev.mseries.core.player;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import me.cedricrev.mseries.core.storage.Storage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

public final class NameResolver implements Listener {

    private static final String TABLE = "known_players";
    private static final Pattern JAVA_NAME = Pattern.compile("[A-Za-z0-9_]{3,16}");

    private final Plugin plugin;
    private final Storage storage;
    private final String table;
    private final boolean floodgate;

    private NameResolver(Plugin plugin, Storage storage) {
        this.plugin = plugin;
        this.storage = storage;
        this.table = storage.table(TABLE);
        this.floodgate = plugin.getServer().getPluginManager().isPluginEnabled("floodgate");
    }

    public static NameResolver create(Plugin plugin, Storage storage) {
        NameResolver resolver = new NameResolver(plugin, storage);
        storage.query(resolver::createTable).join();
        plugin.getServer().getPluginManager().registerEvents(resolver, plugin);
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            resolver.record(online);
        }
        return resolver;
    }

    public boolean matchesGamertags() {
        return this.floodgate && FloodgateNames.prefix().isEmpty();
    }

    public CompletableFuture<Void> remember(UUID uuid, String name) {
        return save(uuid, name, null, null);
    }

    public CompletableFuture<Resolution> resolve(String typed) {
        if (!Names.plausible(typed)) {
            return CompletableFuture.completedFuture(Resolution.invalid());
        }
        String name = typed.trim();
        boolean gamertags = matchesGamertags();
        List<String> exact = new ArrayList<>(List.of(name));
        if (gamertags) {
            String converted = Names.floodgateName("", name);
            if (!converted.equalsIgnoreCase(name)) {
                exact.add(converted);
            }
        }
        for (String candidate : exact) {
            Player online = Bukkit.getPlayerExact(candidate);
            if (online != null) {
                return CompletableFuture.completedFuture(Resolution.found(online.getUniqueId(), online.getName()));
            }
        }
        return this.storage.query(connection -> findExact(connection, exact))
                .thenCompose(found -> found.isPresent()
                        ? CompletableFuture.completedFuture(found.get())
                        : CompletableFuture.supplyAsync(() -> offline(name)))
                .thenCompose(found -> {
                    if (found.isFound() || !gamertags) {
                        return CompletableFuture.completedFuture(found);
                    }
                    return CompletableFuture.supplyAsync(() -> FloodgateNames.lookup(name))
                            .thenCompose(this::joined)
                            .thenCompose(byXuid -> byXuid.isFound()
                                    ? CompletableFuture.completedFuture(byXuid)
                                    : this.storage.query(connection -> findLoose(connection, Names.looseKey(name))));
                });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        record(event.getPlayer());
    }

    private void record(Player player) {
        String gamertag = null;
        String xuid = null;
        if (this.floodgate) {
            Optional<String[]> bedrock = FloodgateNames.gamertagAndXuid(player.getUniqueId());
            if (bedrock.isPresent()) {
                gamertag = bedrock.get()[0];
                xuid = bedrock.get()[1];
            }
        }
        save(player.getUniqueId(), player.getName(), gamertag, xuid).exceptionally(error -> {
            this.plugin.getLogger().warning("Could not remember the name of " + player.getName() + ": "
                    + error.getMessage());
            return null;
        });
    }

    private CompletableFuture<Void> save(UUID uuid, String name, String gamertag, String xuid) {
        String sql = this.storage.type().isSqlite()
                ? "INSERT INTO " + this.table + " (uuid, name, name_key, loose_key, gamertag, gamertag_key, xuid,"
                + " last_seen) VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT(uuid) DO UPDATE SET name = excluded.name,"
                + " name_key = excluded.name_key, loose_key = excluded.loose_key,"
                + " gamertag = COALESCE(excluded.gamertag, gamertag),"
                + " gamertag_key = COALESCE(excluded.gamertag_key, gamertag_key),"
                + " xuid = COALESCE(excluded.xuid, xuid), last_seen = excluded.last_seen"
                : "INSERT INTO " + this.table + " (uuid, name, name_key, loose_key, gamertag, gamertag_key, xuid,"
                + " last_seen) VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE name = VALUES(name),"
                + " name_key = VALUES(name_key), loose_key = VALUES(loose_key),"
                + " gamertag = COALESCE(VALUES(gamertag), gamertag),"
                + " gamertag_key = COALESCE(VALUES(gamertag_key), gamertag_key),"
                + " xuid = COALESCE(VALUES(xuid), xuid), last_seen = VALUES(last_seen)";
        return this.storage.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, uuid.toString());
                statement.setString(2, name);
                statement.setString(3, Names.exactKey(name));
                statement.setString(4, Names.looseKey(name));
                statement.setString(5, gamertag);
                statement.setString(6, gamertag == null ? null : Names.looseKey(gamertag));
                statement.setString(7, xuid);
                statement.setLong(8, System.currentTimeMillis());
                statement.executeUpdate();
            }
            return null;
        });
    }

    private Integer createTable(Connection connection) throws SQLException {
        String columns = "uuid VARCHAR(36) NOT NULL PRIMARY KEY, name VARCHAR(16) NOT NULL,"
                + " name_key VARCHAR(16) NOT NULL, loose_key VARCHAR(40) NOT NULL, gamertag VARCHAR(40),"
                + " gamertag_key VARCHAR(40), xuid VARCHAR(20), last_seen BIGINT NOT NULL";
        try (Statement statement = connection.createStatement()) {
            if (this.storage.type().isSqlite()) {
                statement.execute("CREATE TABLE IF NOT EXISTS " + this.table + " (" + columns + ")");
                for (String key : List.of("name_key", "loose_key", "gamertag_key")) {
                    statement.execute("CREATE INDEX IF NOT EXISTS " + this.table + "_" + key + " ON " + this.table
                            + " (" + key + ")");
                }
            } else {
                statement.execute("CREATE TABLE IF NOT EXISTS " + this.table + " (" + columns
                        + ", INDEX " + this.table + "_name_key (name_key), INDEX " + this.table
                        + "_loose_key (loose_key), INDEX " + this.table + "_gamertag_key (gamertag_key))");
            }
        }
        return 0;
    }

    private Optional<Resolution> findExact(Connection connection, List<String> names) throws SQLException {
        String marks = String.join(", ", names.stream().map(name -> "?").toList());
        try (PreparedStatement statement = connection.prepareStatement("SELECT uuid, name FROM " + this.table
                + " WHERE name_key IN (" + marks + ") ORDER BY last_seen DESC")) {
            for (int i = 0; i < names.size(); i++) {
                statement.setString(i + 1, Names.exactKey(names.get(i)));
            }
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(Resolution.found(UUID.fromString(result.getString(1)), result.getString(2)));
                }
            }
        }
        return Optional.empty();
    }

    private Resolution findLoose(Connection connection, String key) throws SQLException {
        Map<UUID, String> matches = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT uuid, name FROM " + this.table
                + " WHERE loose_key = ? OR gamertag_key = ? ORDER BY last_seen DESC")) {
            statement.setString(1, key);
            statement.setString(2, key);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    matches.putIfAbsent(UUID.fromString(result.getString(1)), result.getString(2));
                }
            }
        }
        if (matches.size() == 1) {
            Map.Entry<UUID, String> match = matches.entrySet().iterator().next();
            return Resolution.found(match.getKey(), match.getValue());
        }
        return matches.isEmpty() ? Resolution.notFound() : Resolution.ambiguous(List.copyOf(matches.values()));
    }

    private CompletableFuture<Resolution> joined(Optional<UUID> uuid) {
        if (uuid.isEmpty()) {
            return CompletableFuture.completedFuture(Resolution.notFound());
        }
        UUID id = uuid.get();
        return this.storage.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("SELECT name FROM " + this.table
                    + " WHERE uuid = ?")) {
                statement.setString(1, id.toString());
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        return Resolution.found(id, result.getString(1));
                    }
                }
            }
            OfflinePlayer player = Bukkit.getOfflinePlayer(id);
            return player.hasPlayedBefore() && player.getName() != null
                    ? Resolution.found(id, player.getName())
                    : Resolution.notFound();
        });
    }

    private Resolution offline(String name) {
        if (!JAVA_NAME.matcher(name).matches()) {
            return Resolution.notFound();
        }
        OfflinePlayer player = cachedOfflinePlayer(name);
        if (player == null || !player.hasPlayedBefore()) {
            return Resolution.notFound();
        }
        return Resolution.found(player.getUniqueId(), player.getName() == null ? name : player.getName());
    }

    @SuppressWarnings("deprecation")
    private OfflinePlayer cachedOfflinePlayer(String name) {
        try {
            Method cached = Bukkit.getServer().getClass().getMethod("getOfflinePlayerIfCached", String.class);
            return (OfflinePlayer) cached.invoke(Bukkit.getServer(), name);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return Bukkit.getOfflinePlayer(name);
        }
    }
}
