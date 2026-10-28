package me.cedricrev.mseries.core.text;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import me.cedricrev.mseries.core.config.ConfigFile;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class Messages {

    public static final String PREFIX_KEY = "prefix";

    private final Plugin plugin;
    private final ConfigFile file;
    private final BukkitAudiences audiences;
    private final Set<String> reportedMissing = ConcurrentHashMap.newKeySet();
    private volatile Map<String, String> texts = Map.of();
    private volatile Component prefix = Component.empty();

    private Messages(Plugin plugin, ConfigFile file) {
        this.plugin = plugin;
        this.file = file;
        this.audiences = BukkitAudiences.create(plugin);
        cache();
    }

    public static Messages load(Plugin plugin) {
        return load(plugin, "messages.yml");
    }

    public static Messages load(Plugin plugin, String fileName) {
        return new Messages(plugin, ConfigFile.load(plugin, fileName));
    }

    public void reload() {
        this.file.reload();
        cache();
    }

    public void close() {
        this.audiences.close();
    }

    public boolean has(String key) {
        return this.texts.containsKey(key);
    }

    public String raw(String key) {
        String text = this.texts.get(key);
        if (text != null) {
            return text;
        }
        if (this.reportedMissing.add(key)) {
            this.plugin.getLogger().warning(this.file.name() + " has no message '" + key + "'");
        }
        return key;
    }

    public Component get(String key, Arg... args) {
        return Text.parse(raw(key), args);
    }

    public String legacy(String key, Arg... args) {
        return Text.legacy(get(key, args));
    }

    public String plain(String key, Arg... args) {
        return Text.plain(get(key, args));
    }

    public Component prefix() {
        return this.prefix;
    }

    public void send(CommandSender to, String key, Arg... args) {
        String text = raw(key);
        if (!text.isEmpty()) {
            this.audiences.sender(to).sendMessage(this.prefix.append(Text.parse(text, args)));
        }
    }

    public void sendRaw(CommandSender to, String key, Arg... args) {
        String text = raw(key);
        if (!text.isEmpty()) {
            this.audiences.sender(to).sendMessage(Text.parse(text, args));
        }
    }

    public void sendActionBar(Player to, String key, Arg... args) {
        String text = raw(key);
        if (!text.isEmpty()) {
            this.audiences.player(to).sendActionBar(Text.parse(text, args));
        }
    }

    public void sendTitle(Player to, String titleKey, String subtitleKey, Duration fadeIn, Duration stay,
                          Duration fadeOut, Arg... args) {
        String title = raw(titleKey);
        String subtitle = raw(subtitleKey);
        if (title.isEmpty() && subtitle.isEmpty()) {
            return;
        }
        this.audiences.player(to).showTitle(Title.title(Text.parse(title, args), Text.parse(subtitle, args),
                Title.Times.times(fadeIn, stay, fadeOut)));
    }

    public void broadcast(String key, Arg... args) {
        String text = raw(key);
        if (!text.isEmpty()) {
            this.audiences.all().sendMessage(this.prefix.append(Text.parse(text, args)));
        }
    }

    private void cache() {
        YamlConfiguration yaml = this.file.yaml();
        Map<String, String> loaded = new HashMap<>();
        for (String key : yaml.getKeys(true)) {
            if (yaml.isConfigurationSection(key) || key.equals(ConfigFile.VERSION_KEY)) {
                continue;
            }
            if (yaml.isList(key)) {
                List<String> lines = yaml.getStringList(key);
                loaded.put(key, String.join("\n", lines));
            } else {
                loaded.put(key, String.valueOf(yaml.get(key)));
            }
        }
        this.texts = Map.copyOf(loaded);
        this.prefix = Text.parse(loaded.getOrDefault(PREFIX_KEY, ""));
        this.reportedMissing.clear();
    }
}
