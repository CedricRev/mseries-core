package me.cedricrev.mseries.core.hook;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Level;
import org.bukkit.plugin.Plugin;

public final class Hooks {

    private final Plugin plugin;

    private Hooks(Plugin plugin) {
        this.plugin = plugin;
    }

    public static Hooks of(Plugin plugin) {
        return new Hooks(plugin);
    }

    public boolean isPresent(String pluginName) {
        Plugin other = this.plugin.getServer().getPluginManager().getPlugin(pluginName);
        return other != null && other.isEnabled();
    }

    public <T> Optional<T> load(String pluginName, Supplier<T> hook) {
        Plugin other = this.plugin.getServer().getPluginManager().getPlugin(pluginName);
        if (other == null || !other.isEnabled()) {
            return Optional.empty();
        }
        try {
            T loaded = hook.get();
            if (loaded == null) {
                return Optional.empty();
            }
            this.plugin.getLogger().info("Hooked into " + other.getName() + " " + other.getDescription().getVersion());
            return Optional.of(loaded);
        } catch (RuntimeException | LinkageError e) {
            this.plugin.getLogger().log(Level.WARNING, "Could not hook into " + other.getName() + " "
                    + other.getDescription().getVersion() + ", so its features are off: " + e, e);
            return Optional.empty();
        }
    }
}
