package me.cedricrev.mseries.core.hook;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import me.cedricrev.mseries.core.scheduler.Scheduler;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public interface Claims {

    static Optional<Claims> griefPrevention(Plugin plugin, Scheduler scheduler) {
        return Hooks.of(plugin).load("GriefPrevention", () -> GriefPreventionClaims.create(plugin, scheduler));
    }

    String name();

    Optional<ClaimInfo> claimAt(Location location);

    boolean canBuild(Player player, Location location);

    boolean canBuild(UUID player, Location location);

    void onChange(Consumer<ClaimChange> listener);
}
