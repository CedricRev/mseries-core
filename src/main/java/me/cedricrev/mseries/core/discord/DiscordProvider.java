package me.cedricrev.mseries.core.discord;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

interface DiscordProvider {

    String name();

    boolean ready();

    boolean send(String channel, DiscordEmbed embed, String content);

    boolean canCheckLinks();

    CompletableFuture<Boolean> linked(UUID player);
}
