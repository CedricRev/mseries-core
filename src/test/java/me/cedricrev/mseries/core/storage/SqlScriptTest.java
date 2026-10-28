package me.cedricrev.mseries.core.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class SqlScriptTest {

    @Test
    void splitsStatementsAndSkipsCommentLines() {
        String script = """
                -- players
                CREATE TABLE a (id INTEGER);

                CREATE TABLE b (
                    name TEXT DEFAULT 'x;y'
                );
                INSERT INTO a VALUES (1)
                """;

        List<String> statements = SqlScript.statements(script);

        assertEquals(3, statements.size());
        assertEquals("CREATE TABLE a (id INTEGER)", statements.get(0));
        assertEquals("CREATE TABLE b (\n    name TEXT DEFAULT 'x;y'\n)", statements.get(1));
        assertEquals("INSERT INTO a VALUES (1)", statements.get(2));
    }

    @Test
    void migrationsMustBeNumberedWithoutGaps() {
        List<Migration> withGap = List.of(new Migration(1, "init", ""), new Migration(3, "late", ""));

        assertThrows(StorageException.class, () -> Migration.sorted(withGap));
    }

    @Test
    void migrationsAreSortedByVersion() {
        List<Migration> sorted = Migration.sorted(List.of(new Migration(2, "second", ""), new Migration(1, "first", "")));

        assertEquals(List.of(1, 2), sorted.stream().map(Migration::version).toList());
    }
}
