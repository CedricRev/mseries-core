package me.cedricrev.mseries.core.storage;

import java.util.ArrayList;
import java.util.List;

final class SqlScript {

    private SqlScript() {
    }

    static List<String> statements(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (String line : script.split("\\R")) {
            String trimmed = line.trim();
            if (!quoted && (trimmed.isEmpty() || trimmed.startsWith("--"))) {
                continue;
            }
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (c == '\'') {
                    quoted = !quoted;
                }
                if (c == ';' && !quoted) {
                    add(statements, current);
                } else {
                    current.append(c);
                }
            }
            current.append('\n');
        }
        add(statements, current);
        return statements;
    }

    private static void add(List<String> statements, StringBuilder current) {
        String statement = current.toString().trim();
        if (!statement.isEmpty()) {
            statements.add(statement);
        }
        current.setLength(0);
    }
}
