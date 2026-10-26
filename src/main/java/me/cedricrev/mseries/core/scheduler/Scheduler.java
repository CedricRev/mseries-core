package me.cedricrev.mseries.core.scheduler;

import java.lang.reflect.InvocationTargetException;
import me.cedricrev.mseries.core.platform.Platform;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public interface Scheduler {

    static Scheduler create(Plugin plugin) {
        return Platform.current().isFolia() ? loadFolia(plugin) : new BukkitTaskScheduler(plugin);
    }

    Task run(Runnable task);

    Task runLater(Runnable task, long delayTicks);

    Task runTimer(Runnable task, long delayTicks, long periodTicks);

    Task runAt(Location location, Runnable task);

    Task runAtLater(Location location, Runnable task, long delayTicks);

    Task runAtTimer(Location location, Runnable task, long delayTicks, long periodTicks);

    Task runFor(Entity entity, Runnable task);

    Task runForLater(Entity entity, Runnable task, long delayTicks);

    Task runForTimer(Entity entity, Runnable task, long delayTicks, long periodTicks);

    Task runAsync(Runnable task);

    Task runAsyncLater(Runnable task, long delayTicks);

    Task runAsyncTimer(Runnable task, long delayTicks, long periodTicks);

    boolean isOwnedByCurrentThread(Location location);

    boolean isOwnedByCurrentThread(Entity entity);

    void cancelAll();

    private static Scheduler loadFolia(Plugin plugin) {
        String name = Scheduler.class.getPackageName() + ".FoliaTaskScheduler";
        try {
            Class<? extends Scheduler> type = Class.forName(name, true, Scheduler.class.getClassLoader())
                    .asSubclass(Scheduler.class);
            return type.getDeclaredConstructor(Plugin.class).newInstance(plugin);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Cannot create Folia scheduler", e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot load Folia scheduler " + name, e);
        }
    }
}
