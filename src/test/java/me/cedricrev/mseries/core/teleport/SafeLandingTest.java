package me.cedricrev.mseries.core.teleport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class SafeLandingTest {

    private static final class FakeColumn implements SafeLanding.Column {

        private final Map<Integer, SafeLanding.Kind> blocks = new HashMap<>();

        FakeColumn set(int y, SafeLanding.Kind kind) {
            this.blocks.put(y, kind);
            return this;
        }

        @Override
        public int minY() {
            return -64;
        }

        @Override
        public int maxY() {
            return 320;
        }

        @Override
        public SafeLanding.Kind kind(int y) {
            return this.blocks.getOrDefault(y, SafeLanding.Kind.OPEN);
        }
    }

    @Test
    void keepsTheSpotWhenItIsSafe() {
        FakeColumn column = new FakeColumn().set(63, SafeLanding.Kind.SOLID);
        assertEquals(OptionalInt.of(64), SafeLanding.findY(column, 64, 5));
    }

    @Test
    void movesUpOutOfAWall() {
        FakeColumn column = new FakeColumn().set(63, SafeLanding.Kind.SOLID).set(64, SafeLanding.Kind.SOLID)
                .set(65, SafeLanding.Kind.SOLID);
        assertEquals(OptionalInt.of(66), SafeLanding.findY(column, 64, 5));
    }

    @Test
    void movesDownOntoTheFloorBelow() {
        FakeColumn column = new FakeColumn().set(61, SafeLanding.Kind.SOLID);
        assertEquals(OptionalInt.of(62), SafeLanding.findY(column, 64, 5));
    }

    @Test
    void refusesLava() {
        FakeColumn column = new FakeColumn();
        for (int y = 50; y < 80; y++) {
            column.set(y, SafeLanding.Kind.DANGER);
        }
        assertTrue(SafeLanding.findY(column, 64, 5).isEmpty());
    }

    @Test
    void waterCountsAsGround() {
        FakeColumn column = new FakeColumn().set(63, SafeLanding.Kind.WATER).set(64, SafeLanding.Kind.WATER);
        assertEquals(OptionalInt.of(64), SafeLanding.findY(column, 64, 0));
    }

    @Test
    void neverLandsOutsideTheWorld() {
        FakeColumn column = new FakeColumn();
        assertTrue(SafeLanding.findY(column, 319, 0).isEmpty());
    }
}
