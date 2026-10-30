package me.cedricrev.mseries.core.hook;

import java.util.Optional;
import java.util.UUID;

public record ClaimChange(Type type, ClaimInfo claim, UUID newOwnerId) {

    public enum Type {
        DELETED, TRANSFERRED
    }

    public Optional<UUID> newOwner() {
        return Optional.ofNullable(this.newOwnerId);
    }
}
