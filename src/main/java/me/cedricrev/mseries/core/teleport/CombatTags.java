package me.cedricrev.mseries.core.teleport;

import java.util.UUID;
import java.util.function.IntSupplier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public final class CombatTags implements Listener {

    private final Cooldowns tags = new Cooldowns();
    private final IntSupplier seconds;

    private CombatTags(IntSupplier seconds) {
        this.seconds = seconds;
    }

    public static CombatTags create(Plugin plugin, IntSupplier seconds) {
        CombatTags tags = new CombatTags(seconds);
        plugin.getServer().getPluginManager().registerEvents(tags, plugin);
        return tags;
    }

    public int remaining(UUID player) {
        return this.tags.remaining(player);
    }

    public void tag(UUID player) {
        this.tags.start(player, this.seconds.getAsInt());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = attacker(event.getDamager());
        if (attacker == null || attacker.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }
        tag(victim.getUniqueId());
        tag(attacker.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.tags.clear(event.getPlayer().getUniqueId());
    }

    private static Player attacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }
}
