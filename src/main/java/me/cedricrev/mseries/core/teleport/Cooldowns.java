package me.cedricrev.mseries.core.teleport;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

public final class Cooldowns {

    private final Map<UUID, Long> until = new ConcurrentHashMap<>();
    private final LongSupplier clock;

    public Cooldowns() {
        this(System::currentTimeMillis);
    }

    public Cooldowns(LongSupplier clock) {
        this.clock = clock;
    }

    public void start(UUID player, int seconds) {
        if (seconds > 0) {
            this.until.put(player, this.clock.getAsLong() + seconds * 1000L);
        }
    }

    public int remaining(UUID player) {
        Long end = this.until.get(player);
        if (end == null) {
            return 0;
        }
        long left = end - this.clock.getAsLong();
        if (left <= 0) {
            this.until.remove(player, end);
            return 0;
        }
        return (int) ((left + 999) / 1000);
    }

    public boolean active(UUID player) {
        return remaining(player) > 0;
    }

    public void clear(UUID player) {
        this.until.remove(player);
    }

    public void clearAll() {
        this.until.clear();
    }
}
