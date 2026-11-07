package me.cedricrev.mseries.core.teleport;

public record Warmup(int seconds, boolean cancelOnMove, boolean cancelOnDamage) {

    public Warmup {
        seconds = Math.max(0, seconds);
    }

    public static Warmup instant() {
        return new Warmup(0, false, false);
    }
}
