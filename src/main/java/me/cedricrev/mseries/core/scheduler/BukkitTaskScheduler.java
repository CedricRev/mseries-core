package me.cedricrev.mseries.core.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

final class BukkitTaskScheduler implements Scheduler {

    private final Plugin plugin;
    private final BukkitScheduler scheduler;

    BukkitTaskScheduler(Plugin plugin) {
        this.plugin = plugin;
        this.scheduler = plugin.getServer().getScheduler();
    }

    @Override
    public Task run(Runnable task) {
        return wrap(this.scheduler.runTask(this.plugin, task));
    }

    @Override
    public Task runLater(Runnable task, long delayTicks) {
        return wrap(this.scheduler.runTaskLater(this.plugin, task, delay(delayTicks)));
    }

    @Override
    public Task runTimer(Runnable task, long delayTicks, long periodTicks) {
        return wrap(this.scheduler.runTaskTimer(this.plugin, task, delay(delayTicks), period(periodTicks)));
    }

    @Override
    public Task runAt(Location location, Runnable task) {
        return run(task);
    }

    @Override
    public Task runAtLater(Location location, Runnable task, long delayTicks) {
        return runLater(task, delayTicks);
    }

    @Override
    public Task runAtTimer(Location location, Runnable task, long delayTicks, long periodTicks) {
        return runTimer(task, delayTicks, periodTicks);
    }

    @Override
    public Task runFor(Entity entity, Runnable task) {
        return run(whileValid(entity, task));
    }

    @Override
    public Task runForLater(Entity entity, Runnable task, long delayTicks) {
        return runLater(whileValid(entity, task), delayTicks);
    }

    @Override
    public Task runForTimer(Entity entity, Runnable task, long delayTicks, long periodTicks) {
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                if (isValid(entity)) {
                    task.run();
                } else {
                    cancel();
                }
            }
        };
        return wrap(runnable.runTaskTimer(this.plugin, delay(delayTicks), period(periodTicks)));
    }

    @Override
    public Task runAsync(Runnable task) {
        return wrap(this.scheduler.runTaskAsynchronously(this.plugin, task));
    }

    @Override
    public Task runAsyncLater(Runnable task, long delayTicks) {
        return wrap(this.scheduler.runTaskLaterAsynchronously(this.plugin, task, delay(delayTicks)));
    }

    @Override
    public Task runAsyncTimer(Runnable task, long delayTicks, long periodTicks) {
        return wrap(this.scheduler.runTaskTimerAsynchronously(this.plugin, task, delay(delayTicks), period(periodTicks)));
    }

    @Override
    public boolean isOwnedByCurrentThread(Location location) {
        return this.plugin.getServer().isPrimaryThread();
    }

    @Override
    public boolean isOwnedByCurrentThread(Entity entity) {
        return this.plugin.getServer().isPrimaryThread();
    }

    @Override
    public void cancelAll() {
        this.scheduler.cancelTasks(this.plugin);
    }

    private static Runnable whileValid(Entity entity, Runnable task) {
        return () -> {
            if (isValid(entity)) {
                task.run();
            }
        };
    }

    private static boolean isValid(Entity entity) {
        return entity instanceof Player player ? player.isOnline() : entity.isValid();
    }

    private static long delay(long ticks) {
        return Math.max(0L, ticks);
    }

    private static long period(long ticks) {
        return Math.max(1L, ticks);
    }

    private static Task wrap(BukkitTask task) {
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
