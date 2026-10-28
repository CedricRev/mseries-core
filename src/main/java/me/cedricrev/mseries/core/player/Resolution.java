package me.cedricrev.mseries.core.player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record Resolution(Status status, UUID uuid, String name, List<String> candidates) {

    public enum Status {
        FOUND,
        NOT_FOUND,
        AMBIGUOUS,
        INVALID
    }

    public Resolution {
        candidates = List.copyOf(candidates);
    }

    static Resolution found(UUID uuid, String name) {
        return new Resolution(Status.FOUND, uuid, name, List.of());
    }

    static Resolution notFound() {
        return new Resolution(Status.NOT_FOUND, null, null, List.of());
    }

    static Resolution ambiguous(List<String> candidates) {
        return new Resolution(Status.AMBIGUOUS, null, null, candidates);
    }

    static Resolution invalid() {
        return new Resolution(Status.INVALID, null, null, List.of());
    }

    public boolean isFound() {
        return this.status == Status.FOUND;
    }

    public Optional<UUID> player() {
        return Optional.ofNullable(this.uuid);
    }
}
