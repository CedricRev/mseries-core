package me.cedricrev.mseries.core.storage;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface SqlWork<T> {

    T apply(Connection connection) throws SQLException;
}
