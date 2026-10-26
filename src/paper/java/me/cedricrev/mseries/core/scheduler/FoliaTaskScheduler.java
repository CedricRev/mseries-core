package me.cedricrev.mseries.core.scheduler;

import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

final class FoliaTaskScheduler implements Scheduler {

    private static final long MILLIS_PER_TICK = 50L;

    private final Plugin plugin;
    private final Server server;
    private final GlobalRegionScheduler global;
    private final RegionScheduler regions;
    private final AsyncScheduler async;

    FoliaTaskScheduler(Plugin plugin) {
        this.plugin = plugin;
        this.server = plugin.getServer();
        this.global = this.server.getGlobalRegionScheduler();
        this.regions = this.server.getRegionScheduler();
        this.async = this.server.getAsyncScheduler();
    }

    @Override
    public Task run(Runnable task) {
        return wrap(this.global.run(this.plugin, consumer(task)));
    }

    @Override
    public Task runLater(Runnable task, long delayTicks) {
        if (delayTicks < 1L) {
            return run(task);
        }
        return wrap(this.global.runDelayed(this.plugin, consumer(task), delayTicks));
    }

    @Override
    public Task runTimer(Runnable task, long delayTicks, long periodTicks) {
        return wrap(this.global.runAtFixedRate(this.plugin, consumer(task), delay(delayTicks), period(periodTicks)));
    }

    @Override
    public Task runAt(Location location, Runnable task) {
        return wrap(this.regions.run(this.plugin, location, consumer(task)));
    }

    @Override
    public Task runAtLater(Location location, Runnable task, long delayTicks) {
        if (delayTicks < 1L) {
            return runAt(location, task);
        }
        return wrap(this.regions.runDelayed(this.plugin, location, consumer(task), delayTicks));
    }

    @Override
    public Task runAtTimer(Location location, Runnable task, long delayTicks, long periodTicks) {
        return wrap(this.regions.runAtFixedRate(this.plugin, location, consumer(task), delay(delayTicks), period(periodTicks)));
    }

    @Override
    public Task runFor(Entity entity, Runnable task) {
        return wrap(entity.getScheduler().run(this.plugin, consumer(task), null));
    }

    @Override
    public Task runForLater(Entity entity, Runnable task, long delayTicks) {
        if (delayTicks < 1L) {
            return runFor(entity, task);
        }
        return wrap(entity.getScheduler().runDelayed(this.plugin, consumer(task), null, delayTicks));
    }

    @Override
    public Task runForTimer(Entity entity, Runnable task, long delayTicks, long periodTicks) {
        return wrap(entity.getScheduler().runAtFixedRate(this.plugin, consumer(task), null, delay(delayTicks), period(periodTicks)));
    }

    @Override
    public Task runAsync(Runnable task) {
        return wrap(this.async.runNow(this.plugin, consumer(task)));
    }

    @Override
    public Task runAsyncLater(Runnable task, long delayTicks) {
        if (delayTicks < 1L) {
            return runAsync(task);
        }
        return wrap(this.async.runDelayed(this.plugin, consumer(task), delayTicks * MILLIS_PER_TICK, TimeUnit.MILLISECONDS));
    }

    @Override
    public Task runAsyncTimer(Runnable task, long delayTicks, long periodTicks) {
        return wrap(this.async.runAtFixedRate(this.plugin, consumer(task),
                Math.max(0L, delayTicks) * MILLIS_PER_TICK, period(periodTicks) * MILLIS_PER_TICK, TimeUnit.MILLISECONDS));
    }

    @Override
    public boolean isOwnedByCurrentThread(Location location) {
        return this.server.isOwnedByCurrentRegion(location);
    }

    @Override
    public boolean isOwnedByCurrentThread(Entity entity) {
        return this.server.isOwnedByCurrentRegion(entity);
    }

    @Override
    public void cancelAll() {
        this.global.cancelTasks(this.plugin);
        this.async.cancelTasks(this.plugin);
    }

    private static Consumer<ScheduledTask> consumer(Runnable task) {
        return scheduled -> task.run();
    }

    private static long delay(long ticks) {
        return Math.max(1L, ticks);
    }

    private static long period(long ticks) {
        return Math.max(1L, ticks);
    }

    private static Task wrap(ScheduledTask task) {
        if (task == null) {
            return CancelledTask.INSTANCE;
        }
        return new Task() {
            @Override
            public void cancel() {
                task.cancel();
            }

            @Override
            public boolean isCancelled() {
                return task.isCancelled();
            }
        };
    }
}
