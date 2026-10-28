package me.cedricrev.mseries.core.storage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

record Migration(int version, String description, String sql) {

    private static final Pattern NAME = Pattern.compile("V(\\d+)__([A-Za-z0-9_]+)\\.sql");

    static List<Migration> fromJar(Class<?> owner, String dialect) {
        String folder = "db/migration/" + dialect + "/";
        File jar = jarOf(owner);
        List<Migration> migrations = new ArrayList<>();
        try (JarFile file = new JarFile(jar)) {
            Enumeration<JarEntry> entries = file.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().startsWith(folder)) {
                    continue;
                }
                String name = entry.getName().substring(folder.length());
                Matcher matcher = NAME.matcher(name);
                if (!matcher.matches()) {
                    throw new StorageException("Migration file " + entry.getName()
                            + " must be named V<number>__<description>.sql");
                }
                try (InputStream in = file.getInputStream(entry)) {
                    migrations.add(new Migration(Integer.parseInt(matcher.group(1)),
                            matcher.group(2).replace('_', ' '),
                            new String(in.readAllBytes(), StandardCharsets.UTF_8)));
                }
            }
        } catch (IOException e) {
            throw new StorageException("Cannot read migrations from " + jar, e);
        }
        return sorted(migrations);
    }

    static List<Migration> sorted(List<Migration> migrations) {
        List<Migration> sorted = new ArrayList<>(migrations);
        sorted.sort(Comparator.comparingInt(Migration::version));
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).version() != i + 1) {
                throw new StorageException("Migrations must be numbered V1, V2, V3 and so on without gaps; found V"
                        + sorted.get(i).version() + " at position " + (i + 1));
            }
        }
        return List.copyOf(sorted);
    }

    private static File jarOf(Class<?> owner) {
        try {
            return new File(owner.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException | SecurityException | NullPointerException e) {
            throw new StorageException("Cannot find the plugin jar to read migrations", e);
        }
    }
}
