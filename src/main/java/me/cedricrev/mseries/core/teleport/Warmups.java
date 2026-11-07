package me.cedricrev.mseries.core.teleport;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import me.cedricrev.mseries.core.scheduler.Scheduler;
import me.cedricrev.mseries.core.scheduler.Task;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public final class Warmups implements Listener {

    public enum Reason {
        MOVED,
        DAMAGED,
        REPLACED,
        QUIT
    }

    private record Pending(Task task, Warmup settings, Consumer<Reason> onCancel) {
    }

    private final Scheduler scheduler;
    private final Map<UUID, Pending> active = new ConcurrentHashMap<>();

    private Warmups(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public static Warmups create(Plugin plugin, Scheduler scheduler) {
        Warmups warmups = new Warmups(scheduler);
        plugin.getServer().getPluginManager().registerEvents(warmups, plugin);
        return warmups;
    }

    public void start(Player player, Warmup settings, Runnable onDone, Consumer<Reason> onCancel) {
        UUID id = player.getUniqueId();
        cancel(id, Reason.REPLACED);
        if (settings.seconds() <= 0) {
            onDone.run();
            return;
        }
        Pending[] holder = new Pending[1];
        Task task = this.scheduler.runForLater(player, () -> {
            if (this.active.remove(id, holder[0])) {
                onDone.run();
            }
        }, settings.seconds() * 20L);
        holder[0] = new Pending(task, settings, onCancel);
        this.active.put(id, holder[0]);
    }

    public boolean has(UUID player) {
        return this.active.containsKey(player);
    }

    public boolean cancel(UUID player, Reason reason) {
        Pending pending = this.active.remove(player);
        if (pending == null) {
            return false;
        }
        pending.task().cancel();
        pending.onCancel().accept(reason);
        return true;
    }

    public void cancelAll() {
        this.active.values().forEach(pending -> pending.task().cancel());
        this.active.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            Pending pending = this.active.get(player.getUniqueId());
            if (pending != null && pending.settings().cancelOnDamage()) {
                cancel(player.getUniqueId(), Reason.DAMAGED);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (this.active.isEmpty()) {
            return;
        }
        Pending pending = this.active.get(event.getPlayer().getUniqueId());
        if (pending == null || !pending.settings().cancelOnMove()) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to != null && (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ())) {
            cancel(event.getPlayer().getUniqueId(), Reason.MOVED);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancel(event.getPlayer().getUniqueId(), Reason.QUIT);
    }
}
