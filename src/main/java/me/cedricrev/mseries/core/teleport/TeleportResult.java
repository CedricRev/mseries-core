package me.cedricrev.mseries.core.teleport;

public enum TeleportResult {
    SUCCESS,
    UNSAFE,
    NO_WORLD,
    OFFLINE,
    FAILED;

    public boolean succeeded() {
        return this == SUCCESS;
    }
}
