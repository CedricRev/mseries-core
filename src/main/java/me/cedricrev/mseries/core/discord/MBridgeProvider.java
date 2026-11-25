package me.cedricrev.mseries.core.discord;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.plugin.Plugin;

final class MBridgeProvider implements DiscordProvider {

    static final String PLUGIN = "MBridge";

    private final Plugin bridge;
    private final Method ready;
    private final Method post;
    private final Method linked;

    private MBridgeProvider(Plugin bridge, Method ready, Method post, Method linked) {
        this.bridge = bridge;
        this.ready = ready;
        this.post = post;
        this.linked = linked;
    }

    static MBridgeProvider of(Plugin bridge) {
        try {
            Class<?> type = bridge.getClass();
            return new MBridgeProvider(bridge, type.getMethod("discordReady"), type.getMethod("discordPost", Map.class),
                    type.getMethod("discordLinked", UUID.class));
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    Plugin plugin() {
        return this.bridge;
    }

    @Override
    public String name() {
        return PLUGIN;
    }

    @Override
    public boolean ready() {
        try {
            return this.bridge.isEnabled() && (boolean) this.ready.invoke(this.bridge);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    @Override
    public boolean send(String channel, DiscordEmbed embed, String content) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("channel", channel == null || channel.isBlank() || channel.equalsIgnoreCase("main") ? "chat" : channel);
        if (content != null && !content.isBlank()) {
            out.put("content", content);
        }
        if (embed != null) {
            if (embed.color() != null) {
                out.put("color", String.format("%06x", embed.color() & 0xFFFFFF));
            }
            if (!embed.author().isEmpty()) {
                Map<String, Object> author = new LinkedHashMap<>();
                author.put("name", embed.author());
                if (url(embed.authorIcon())) {
                    author.put("icon", embed.authorIcon());
                }
                out.put("author", author);
            }
            put(out, "title", embed.title());
            put(out, "description", embed.description());
            if (url(embed.thumbnail())) {
                out.put("thumbnail", embed.thumbnail());
            }
            List<Map<String, Object>> fields = new ArrayList<>();
            for (DiscordEmbed.Field field : embed.fields()) {
                Map<String, Object> f = new LinkedHashMap<>();
                f.put("name", field.name());
                f.put("value", field.value());
                f.put("inline", field.inline());
                fields.add(f);
            }
            if (!fields.isEmpty()) {
                out.put("fields", fields);
            }
            put(out, "footer", embed.footer());
            if (url(embed.footerIcon())) {
                out.put("footer-icon", embed.footerIcon());
            }
            if (embed.timestamp()) {
                out.put("timestamp", true);
            }
        }
        try {
            return (boolean) this.post.invoke(this.bridge, out);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    @Override
    public boolean canCheckLinks() {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Boolean> linked(UUID player) {
        try {
            return ((CompletableFuture<Boolean>) this.linked.invoke(this.bridge, player)).exceptionally(e -> false);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return CompletableFuture.completedFuture(false);
        }
    }

    private static void put(Map<String, Object> out, String key, String value) {
        if (!value.isEmpty()) {
            out.put(key, value);
        }
    }

    static boolean url(String text) {
        return text.startsWith("https://") || text.startsWith("http://");
    }
}
