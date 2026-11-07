package me.cedricrev.mseries.core.player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Pattern;

final class Names {

    static final int MAX_IN_GAME = 16;
    private static final Pattern TYPED = Pattern.compile("[A-Za-z0-9_ .*\\-+~#]{1,40}");

    private Names() {
    }

    static boolean plausible(String typed) {
        return typed != null && !typed.isBlank() && TYPED.matcher(typed).matches();
    }

    static String exactKey(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    static String looseKey(String name) {
        return name.toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "");
    }

    static String floodgateName(String prefix, String gamertag) {
        String name = prefix + gamertag.replace(' ', '_');
        return name.length() > MAX_IN_GAME ? name.substring(0, MAX_IN_GAME) : name;
    }

    static <T> List<T> matchOnline(String typed, Collection<T> candidates, Function<T, List<String>> names) {
        String wanted = onlineKey(typed);
        if (wanted.isEmpty()) {
            return List.of();
        }
        List<T> exact = new ArrayList<>();
        List<T> prefix = new ArrayList<>();
        for (T candidate : candidates) {
            boolean isExact = false;
            boolean isPrefix = false;
            for (String raw : names.apply(candidate)) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                String name = onlineKey(raw);
                String bare = stripPrefix(name);
                if (name.equals(wanted) || bare.equals(wanted)) {
                    isExact = true;
                } else if (name.startsWith(wanted) || bare.startsWith(wanted)) {
                    isPrefix = true;
                }
            }
            if (isExact) {
                exact.add(candidate);
            } else if (isPrefix) {
                prefix.add(candidate);
            }
        }
        return List.copyOf(exact.isEmpty() ? prefix : exact);
    }

    static String onlineKey(String name) {
        return name.trim().replaceAll("\\s+", "_").toLowerCase(Locale.ROOT);
    }

    private static String stripPrefix(String key) {
        int i = 0;
        while (i < key.length() && !Character.isLetterOrDigit(key.charAt(i)) && key.charAt(i) != '_') {
            i++;
        }
        return key.substring(i);
    }
}
