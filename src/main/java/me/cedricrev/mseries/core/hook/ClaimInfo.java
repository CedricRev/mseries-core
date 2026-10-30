package me.cedricrev.mseries.core.hook;

import java.util.Optional;
import java.util.UUID;

public record ClaimInfo(String id, UUID ownerId, boolean admin) {

    public Optional<UUID> owner() {
        return Optional.ofNullable(this.ownerId);
    }

    public boolean ownedBy(UUID player) {
        return this.ownerId != null && this.ownerId.equals(player);
    }
}
