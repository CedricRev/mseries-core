package me.cedricrev.mseries.core.teleport;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import me.cedricrev.mseries.core.scheduler.Scheduler;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class Teleports {

    public static final int NO_SAFE_LANDING = -1;

    private final Plugin plugin;
    private final Scheduler scheduler;
    private final MethodHandle chunkAtAsync;
    private final MethodHandle teleportAsync;

    private Teleports(Plugin plugin, Scheduler scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        this.chunkAtAsync = find(lookup, World.class, "getChunkAtAsync",
                MethodType.methodType(CompletableFuture.class, int.class, int.class));
        this.teleportAsync = find(lookup, Entity.class, "teleportAsync",
                MethodType.methodType(CompletableFuture.class, Location.class));
    }

    public static Teleports create(Plugin plugin, Scheduler scheduler) {
        return new Teleports(plugin, scheduler);
    }

    public CompletableFuture<TeleportResult> teleport(Player player, Location destination, int safeRadius) {
        CompletableFuture<TeleportResult> result = new CompletableFuture<>();
        World world = destination.getWorld();
        if (world == null || this.plugin.getServer().getWorld(world.getUID()) == null) {
            result.complete(TeleportResult.NO_WORLD);
            return result;
        }
        UUID id = player.getUniqueId();
        Location target = destination.clone();
        loadChunk(world, target).whenComplete((ignored, error) -> {
            if (error != null) {
                fail(result, "Could not load the destination chunk", error);
                return;
            }
            this.scheduler.runAt(target, () -> land(id, target, safeRadius, result));
        });
        return result;
    }

    private void land(UUID id, Location target, int safeRadius, CompletableFuture<TeleportResult> result) {
        Optional<Location> landing;
        try {
            landing = safeRadius < 0 ? Optional.of(target) : SafeLanding.find(target, safeRadius);
        } catch (RuntimeException e) {
            fail(result, "Could not check the destination", e);
            return;
        }
        if (landing.isEmpty()) {
            result.complete(TeleportResult.UNSAFE);
            return;
        }
        Player traveller = this.plugin.getServer().getPlayer(id);
        if (traveller == null) {
            result.complete(TeleportResult.OFFLINE);
            return;
        }
        Location spot = landing.get();
        this.scheduler.runFor(traveller, () -> move(traveller, spot).whenComplete((moved, error) -> {
            if (error != null) {
                fail(result, "Teleport failed", error);
            } else if (!traveller.isOnline()) {
                result.complete(TeleportResult.OFFLINE);
            } else {
                result.complete(Boolean.TRUE.equals(moved) ? TeleportResult.SUCCESS : TeleportResult.FAILED);
            }
        }));
    }

    private CompletableFuture<?> loadChunk(World world, Location target) {
        if (this.chunkAtAsync == null) {
            return CompletableFuture.completedFuture(null);
        }
        try {
            return (CompletableFuture<?>) this.chunkAtAsync.invoke(world, target.getBlockX() >> 4,
                    target.getBlockZ() >> 4);
        } catch (Throwable e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @SuppressWarnings("unchecked")
    private CompletableFuture<Boolean> move(Player player, Location spot) {
        if (this.teleportAsync == null) {
            return CompletableFuture.completedFuture(player.teleport(spot));
        }
        try {
            return (CompletableFuture<Boolean>) this.teleportAsync.invoke(player, spot);
        } catch (Throwable e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private void fail(CompletableFuture<TeleportResult> result, String message, Throwable error) {
        this.plugin.getLogger().log(Level.WARNING, message, error);
        result.complete(TeleportResult.FAILED);
    }

    private static MethodHandle find(MethodHandles.Lookup lookup, Class<?> owner, String name, MethodType type) {
        try {
            return lookup.findVirtual(owner, name, type);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            return null;
        }
    }
}
