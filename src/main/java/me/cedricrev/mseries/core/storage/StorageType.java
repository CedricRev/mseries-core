package me.cedricrev.mseries.core.storage;

import java.util.Locale;
import java.util.Optional;

public enum StorageType {
    SQLITE("sqlite"),
    MYSQL("mysql"),
    MARIADB("mysql");

    private final String dialect;

    StorageType(String dialect) {
        this.dialect = dialect;
    }

    public String dialect() {
        return this.dialect;
    }

    public boolean isSqlite() {
        return this == SQLITE;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<StorageType> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String id = value.trim().toUpperCase(Locale.ROOT);
        for (StorageType type : values()) {
            if (type.name().equals(id)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
