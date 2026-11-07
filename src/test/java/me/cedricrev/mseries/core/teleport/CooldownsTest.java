package me.cedricrev.mseries.core.teleport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class CooldownsTest {

    private final AtomicLong now = new AtomicLong(1_000_000L);
    private final Cooldowns cooldowns = new Cooldowns(this.now::get);
    private final UUID player = UUID.randomUUID();

    @Test
    void roundsRemainingSecondsUp() {
        this.cooldowns.start(this.player, 5);
        this.now.addAndGet(4_001L);
        assertEquals(1, this.cooldowns.remaining(this.player));
    }

    @Test
    void endsAfterTheTime() {
        this.cooldowns.start(this.player, 5);
        this.now.addAndGet(5_000L);
        assertFalse(this.cooldowns.active(this.player));
    }

    @Test
    void zeroSecondsStartsNothing() {
        this.cooldowns.start(this.player, 0);
        assertFalse(this.cooldowns.active(this.player));
    }

    @Test
    void clearEndsItEarly() {
        this.cooldowns.start(this.player, 30);
        assertTrue(this.cooldowns.active(this.player));
        this.cooldowns.clear(this.player);
        assertEquals(0, this.cooldowns.remaining(this.player));
    }
}
