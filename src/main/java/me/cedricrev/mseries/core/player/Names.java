package me.cedricrev.mseries.core.player;

import java.util.Locale;
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
}
