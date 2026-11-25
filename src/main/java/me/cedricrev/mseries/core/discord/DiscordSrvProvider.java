package me.cedricrev.mseries.core.discord;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

final class DiscordSrvProvider implements DiscordProvider {

    static final String PLUGIN = "DiscordSRV";
    private static final int MAX_FIELDS = 25;

    private final Logger logger;

    DiscordSrvProvider(Logger logger) {
        this.logger = logger;
    }

    @Override
    public String name() {
        return PLUGIN;
    }

    @Override
    public boolean ready() {
        return DiscordSRV.isReady;
    }

    @Override
    public boolean send(String channel, DiscordEmbed embed, String content) {
        if (!DiscordSRV.isReady) {
            return false;
        }
        TextChannel target = channel(channel);
        if (target == null) {
            this.logger.warning("DiscordSRV has no channel called '" + channel + "'");
            return false;
        }
        EmbedBuilder builder = embed == null ? new EmbedBuilder() : build(embed);
        boolean text = content != null && !content.isBlank();
        if (builder.isEmpty() && !text) {
            return false;
        }
        var action = text ? target.sendMessage(cut(content, 2000)) : target.sendMessageEmbeds(builder.build());
        if (text && !builder.isEmpty()) {
            action = action.setEmbeds(builder.build());
        }
        action.allowedMentions(EnumSet.noneOf(Message.MentionType.class)).queue(null,
                error -> this.logger.warning("Could not send a post to Discord: " + error.getMessage()));
        return true;
    }

    @Override
    public boolean canCheckLinks() {
        return true;
    }

    @Override
    public CompletableFuture<Boolean> linked(UUID player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var links = DiscordSRV.getPlugin().getAccountLinkManager();
                return links != null && links.getDiscordId(player) != null;
            } catch (RuntimeException | LinkageError e) {
                return false;
            }
        });
    }

    private static EmbedBuilder build(DiscordEmbed embed) {
        EmbedBuilder builder = new EmbedBuilder();
        if (!embed.author().isEmpty()) {
            builder.setAuthor(cut(embed.author(), 256), null, link(embed.authorIcon()));
        }
        if (!embed.title().isEmpty()) {
            builder.setTitle(cut(embed.title(), 256));
        }
        if (!embed.description().isEmpty()) {
            builder.setDescription(cut(embed.description(), 4096));
        }
        if (embed.color() != null) {
            builder.setColor(embed.color());
        }
        if (link(embed.thumbnail()) != null) {
            builder.setThumbnail(embed.thumbnail());
        }
        for (DiscordEmbed.Field field : embed.fields().subList(0, Math.min(embed.fields().size(), MAX_FIELDS))) {
            builder.addField(cut(field.name(), 256), cut(field.value(), 1024), field.inline());
        }
        if (!embed.footer().isEmpty()) {
            builder.setFooter(cut(embed.footer(), 2048), link(embed.footerIcon()));
        }
        if (embed.timestamp() && !builder.isEmpty()) {
            builder.setTimestamp(Instant.now());
        }
        return builder;
    }

    private static TextChannel channel(String name) {
        DiscordSRV discord = DiscordSRV.getPlugin();
        if (name == null || name.isEmpty() || name.equalsIgnoreCase("main")) {
            return discord.getMainTextChannel();
        }
        if (name.chars().allMatch(Character::isDigit)) {
            TextChannel byId = discord.getJda() == null ? null : discord.getJda().getTextChannelById(name);
            if (byId != null) {
                return byId;
            }
        }
        TextChannel byName = discord.getDestinationTextChannelForGameChannelName(name);
        return byName != null ? byName : discord.getOptionalTextChannel(name);
    }

    private static String link(String text) {
        return MBridgeProvider.url(text) ? text : null;
    }

    private static String cut(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max);
    }
}
