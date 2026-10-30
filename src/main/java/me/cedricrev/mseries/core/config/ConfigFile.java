package me.cedricrev.mseries.core.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

public final class ConfigFile {

    public static final String VERSION_KEY = "config-version";

    private final Plugin plugin;
    private final String name;
    private final File file;
    private final List<String> openSections;
    private volatile YamlConfiguration yaml;

    private ConfigFile(Plugin plugin, String name, List<String> openSections) {
        this.plugin = plugin;
        this.name = name;
        this.file = new File(plugin.getDataFolder(), name);
        this.openSections = openSections;
    }

    public static ConfigFile load(Plugin plugin, String name) {
        return load(plugin, name, new String[0]);
    }

    public static ConfigFile load(Plugin plugin, String name, String... openSections) {
        ConfigFile config = new ConfigFile(plugin, name, List.of(openSections));
        config.reload();
        return config;
    }

    public void reload() {
        YamlConfiguration defaults = readDefaults();
        if (!this.file.exists()) {
            this.plugin.saveResource(this.name, false);
        }
        YamlConfiguration loaded = readFile();
        if (defaults != null && merge(loaded, defaults)) {
            save(loaded);
        }
        this.yaml = loaded;
    }

    public YamlConfiguration yaml() {
        return this.yaml;
    }

    public File file() {
        return this.file;
    }

    public String name() {
        return this.name;
    }

    private YamlConfiguration readDefaults() {
        InputStream stream = this.plugin.getResource(this.name);
        if (stream == null) {
            return null;
        }
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            YamlConfiguration defaults = new YamlConfiguration();
            defaults.load(reader);
            return defaults;
        } catch (IOException | InvalidConfigurationException e) {
            throw new ConfigException("Cannot read the default " + this.name + " inside the plugin jar", e);
        }
    }

    private YamlConfiguration readFile() {
        YamlConfiguration loaded = new YamlConfiguration();
        try {
            loaded.load(this.file);
            return loaded;
        } catch (IOException e) {
            throw new ConfigException("Cannot read " + this.file.getPath(), e);
        } catch (InvalidConfigurationException e) {
            throw new ConfigException(this.name + " is not valid YAML: " + e.getMessage(), e);
        }
    }

    private boolean merge(YamlConfiguration target, YamlConfiguration defaults) {
        int added = addMissing(target, defaults, this.openSections);
        int current = target.getInt(VERSION_KEY, 0);
        int latest = defaults.getInt(VERSION_KEY, 0);
        boolean upgraded = current < latest;
        if (upgraded) {
            target.set(VERSION_KEY, latest);
        } else if (current > latest) {
            this.plugin.getLogger().warning(this.name + " has " + VERSION_KEY + " " + current
                    + ", which is newer than this plugin version knows (" + latest + ")");
        }
        if (added > 0 || upgraded) {
            this.plugin.getLogger().info("Updated " + this.name + " to " + VERSION_KEY + " " + Math.max(current, latest)
                    + " and added " + added + " missing setting" + (added == 1 ? "" : "s"));
            return true;
        }
        return false;
    }

    static int addMissing(YamlConfiguration target, YamlConfiguration defaults, List<String> openSections) {
        List<String> userOwned = openSections.stream().filter(target::isConfigurationSection).toList();
        int added = 0;
        for (String path : defaults.getKeys(true)) {
            if (target.contains(path, true) || !parentIsSection(target, path) || isInside(path, userOwned)) {
                continue;
            }
            if (defaults.isConfigurationSection(path)) {
                target.createSection(path);
            } else {
                target.set(path, defaults.get(path));
            }
            target.setComments(path, defaults.getComments(path));
            target.setInlineComments(path, defaults.getInlineComments(path));
            added++;
        }
        return added;
    }

    private static boolean isInside(String path, List<String> sections) {
        for (String section : sections) {
            if (path.startsWith(section + ".")) {
                return true;
            }
        }
        return false;
    }

    private static boolean parentIsSection(YamlConfiguration target, String path) {
        int dot = path.lastIndexOf('.');
        return dot < 0 || target.isConfigurationSection(path.substring(0, dot));
    }

    private void save(YamlConfiguration loaded) {
        try {
            loaded.save(this.file);
        } catch (IOException e) {
            throw new ConfigException("Cannot save " + this.file.getPath(), e);
        }
    }
}
