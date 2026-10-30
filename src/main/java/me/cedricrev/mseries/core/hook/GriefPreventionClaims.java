package me.cedricrev.mseries.core.hook;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;
import me.cedricrev.mseries.core.scheduler.Scheduler;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.events.ClaimDeletedEvent;
import me.ryanhamshire.GriefPrevention.events.ClaimTransferEvent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

final class GriefPreventionClaims implements Claims, Listener {

    private final Plugin plugin;
    private final Scheduler scheduler;
    private final List<Consumer<ClaimChange>> listeners = new CopyOnWriteArrayList<>();

    private GriefPreventionClaims(Plugin plugin, Scheduler scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
    }

    static GriefPreventionClaims create(Plugin plugin, Scheduler scheduler) {
        GriefPreventionClaims claims = new GriefPreventionClaims(plugin, scheduler);
        plugin.getServer().getPluginManager().registerEvents(claims, plugin);
        return claims;
    }

    @Override
    public String name() {
        return "GriefPrevention";
    }

    @Override
    public Optional<ClaimInfo> claimAt(Location location) {
        return find(location).map(GriefPreventionClaims::info);
    }

    @Override
    public boolean canBuild(Player player, Location location) {
        Optional<Claim> claim = find(location);
        if (claim.isEmpty()) {
            return true;
        }
        try {
            return claim.get().checkPermission(player, ClaimPermission.Build, null) == null;
        } catch (RuntimeException | LinkageError e) {
            warn("build check", e);
            return false;
        }
    }

    @Override
    public boolean canBuild(UUID player, Location location) {
        Optional<Claim> claim = find(location);
        if (claim.isEmpty()) {
            return true;
        }
        if (player.equals(claim.get().getOwnerID())) {
            return true;
        }
        try {
            return claim.get().checkPermission(player, ClaimPermission.Build, null) == null;
        } catch (RuntimeException | LinkageError e) {
            warn("build check", e);
            return false;
        }
    }

    @Override
    public void onChange(Consumer<ClaimChange> listener) {
        this.listeners.add(listener);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeleted(ClaimDeletedEvent event) {
        Claim claim = event.getClaim();
        List<ClaimChange> changes = new ArrayList<>();
        changes.add(new ClaimChange(ClaimChange.Type.DELETED, info(claim), null));
        if (claim.children != null) {
            for (Claim child : claim.children) {
                changes.add(new ClaimChange(ClaimChange.Type.DELETED, info(child), null));
            }
        }
        publish(changes);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTransferred(ClaimTransferEvent event) {
        publish(List.of(new ClaimChange(ClaimChange.Type.TRANSFERRED, info(event.getClaim()), event.getNewOwner())));
    }

    private void publish(List<ClaimChange> changes) {
        if (this.listeners.isEmpty()) {
            return;
        }
        this.scheduler.runLater(() -> {
            for (ClaimChange change : changes) {
                for (Consumer<ClaimChange> listener : this.listeners) {
                    try {
                        listener.accept(change);
                    } catch (RuntimeException e) {
                        this.plugin.getLogger().log(Level.WARNING, "A claim change listener failed", e);
                    }
                }
            }
        }, 1L);
    }

    private Optional<Claim> find(Location location) {
        if (location == null || location.getWorld() == null) {
            return Optional.empty();
        }
        try {
            GriefPrevention instance = GriefPrevention.instance;
            DataStore store = instance == null ? null : instance.dataStore;
            if (store == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(store.getClaimAt(location, false, null));
        } catch (RuntimeException | LinkageError e) {
            warn("claim lookup", e);
            return Optional.empty();
        }
    }

    private static ClaimInfo info(Claim claim) {
        return new ClaimInfo(String.valueOf(claim.getID()), claim.getOwnerID(), claim.isAdminClaim());
    }

    private void warn(String action, Throwable e) {
        this.plugin.getLogger().log(Level.WARNING, "GriefPrevention failed a " + action + ": " + e, e);
    }
}
