package me.cedricrev.mseries.core.discord;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import org.bukkit.plugin.Plugin;

public final class Discord {

    private static final String MARKDOWN = "\\*_~`|>";

    private final Plugin plugin;
    private volatile MBridgeProvider bridge;
    private volatile DiscordSrvProvider discordSrv;
    private volatile String announced = "";

    private Discord(Plugin plugin) {
        this.plugin = plugin;
    }

    public static Discord of(Plugin plugin) {
        return new Discord(plugin);
    }

    public String provider() {
        DiscordProvider found = find();
        return found == null ? "" : found.name();
    }

    public boolean available() {
        return find() != null;
    }

    public boolean send(String channel, DiscordEmbed embed) {
        return send(List.of(channel == null ? "" : channel), embed, null);
    }

    public boolean send(Collection<String> channels, DiscordEmbed embed) {
        return send(channels, embed, null);
    }

    public boolean sendText(String channel, String text) {
        return send(List.of(channel == null ? "" : channel), null, text);
    }

    public boolean send(Collection<String> channels, DiscordEmbed embed, String text) {
        boolean noEmbed = embed == null || embed.isEmpty();
        if (noEmbed && (text == null || text.isBlank())) {
            return false;
        }
        DiscordProvider target = find();
        if (target == null) {
            return false;
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String channel : channels.isEmpty() ? List.of("") : channels) {
            unique.add(channel == null ? "" : channel.trim());
        }
        boolean sent = false;
        for (String channel : unique) {
            try {
                sent |= target.send(channel, noEmbed ? null : embed, text);
            } catch (RuntimeException | LinkageError e) {
                this.plugin.getLogger().log(Level.WARNING, "Could not send a post through " + target.name() + ": " + e, e);
            }
        }
        return sent;
    }

    public boolean canCheckLinks() {
        DiscordProvider found = find();
        return found != null && found.canCheckLinks();
    }

    public CompletableFuture<Boolean> linked(UUID player) {
        DiscordProvider found = find();
        if (found == null || player == null) {
            return CompletableFuture.completedFuture(false);
        }
        try {
            return found.linked(player);
        } catch (RuntimeException | LinkageError e) {
            return CompletableFuture.completedFuture(false);
        }
    }

    public static String escape(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length() + 8);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (MARKDOWN.indexOf(c) >= 0) {
                out.append('\\');
            }
            out.append(c);
        }
        return out.toString();
    }

    private DiscordProvider find() {
        DiscordProvider found = mbridge();
        if (found == null) {
            found = discordSrv();
        }
        String name = found == null ? "" : found.name();
        if (!name.equals(this.announced)) {
            this.announced = name;
            if (!name.isEmpty()) {
                this.plugin.getLogger().info("Discord posts go through " + name);
            }
        }
        return found;
    }

    private DiscordProvider mbridge() {
        Plugin other = this.plugin.getServer().getPluginManager().getPlugin(MBridgeProvider.PLUGIN);
        if (other == null || !other.isEnabled()) {
            return null;
        }
        MBridgeProvider current = this.bridge;
        if (current == null || current.plugin() != other) {
            current = MBridgeProvider.of(other);
            this.bridge = current;
        }
        return current != null && current.ready() ? current : null;
    }

    private DiscordProvider discordSrv() {
        Plugin other = this.plugin.getServer().getPluginManager().getPlugin(DiscordSrvProvider.PLUGIN);
        if (other == null || !other.isEnabled()) {
            return null;
        }
        try {
            DiscordSrvProvider current = this.discordSrv;
            if (current == null) {
                current = new DiscordSrvProvider(this.plugin.getLogger());
                this.discordSrv = current;
            }
            return current.ready() ? current : null;
        } catch (LinkageError e) {
            return null;
        }
    }
}
