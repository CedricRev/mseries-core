package me.cedricrev.mseries.core.platform;

public enum ServerCore {
    SPIGOT,
    PAPER,
    PURPUR,
    FOLIA;

    public boolean isPaperBased() {
        return this != SPIGOT;
    }
}
