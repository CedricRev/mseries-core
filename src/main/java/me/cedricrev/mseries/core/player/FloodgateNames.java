package me.cedricrev.mseries.core.player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

final class FloodgateNames {

    private FloodgateNames() {
    }

    static String prefix() {
        String prefix = FloodgateApi.getInstance().getPlayerPrefix();
        return prefix == null ? "" : prefix;
    }

    static Optional<String[]> gamertagAndXuid(UUID player) {
        FloodgatePlayer bedrock = FloodgateApi.getInstance().getPlayer(player);
        if (bedrock == null) {
            return Optional.empty();
        }
        return Optional.of(new String[] {bedrock.getUsername(), bedrock.getXuid()});
    }

    static Optional<UUID> lookup(String gamertag) {
        try {
            return Optional.ofNullable(FloodgateApi.getInstance().getUuidFor(gamertag).get(5, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception | LinkageError e) {
            return Optional.empty();
        }
    }
}
