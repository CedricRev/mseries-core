package me.cedricrev.mseries.core.storage;

import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;

public record StorageSettings(
        StorageType type,
        String sqliteFile,
        String host,
        int port,
        String database,
        String username,
        String password,
        int poolSize,
        Map<String, String> properties,
        String tablePrefix) {

    public StorageSettings {
        properties = Map.copyOf(properties);
    }

    public static StorageSettings from(ConfigurationSection section) {
        if (section == null) {
            throw new StorageException("The storage section is missing from the config");
        }
        String typeName = section.getString("type", "sqlite");
        StorageType type = StorageType.parse(typeName)
                .orElseThrow(() -> new StorageException("Unknown storage.type '" + typeName
                        + "', use sqlite, mysql or mariadb"));
        Map<String, String> properties = new LinkedHashMap<>();
        ConfigurationSection extra = section.getConfigurationSection("mysql.properties");
        if (extra != null) {
            for (String key : extra.getKeys(false)) {
                properties.put(key, String.valueOf(extra.get(key)));
            }
        }
        return new StorageSettings(
                type,
                section.getString("sqlite.file", "data.db"),
                section.getString("mysql.host", "localhost"),
                section.getInt("mysql.port", 3306),
                section.getString("mysql.database", "minecraft"),
                section.getString("mysql.username", "root"),
                section.getString("mysql.password", ""),
                Math.max(1, section.getInt("mysql.pool-size", 4)),
                properties,
                section.getString("table-prefix", ""));
    }

    public StorageSettings withType(StorageType other) {
        return new StorageSettings(other, this.sqliteFile, this.host, this.port, this.database, this.username,
                this.password, this.poolSize, this.properties, this.tablePrefix);
    }
}
