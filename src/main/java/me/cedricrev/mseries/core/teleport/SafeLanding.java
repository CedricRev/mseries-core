package me.cedricrev.mseries.core.teleport;

import java.util.EnumSet;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public final class SafeLanding {

    public enum Kind {
        OPEN,
        WATER,
        SOLID,
        DANGER
    }

    public interface Column {

        int minY();

        int maxY();

        Kind kind(int y);
    }

    private static final Set<Material> DANGER = EnumSet.of(
            Material.LAVA, Material.FIRE, Material.SOUL_FIRE, Material.MAGMA_BLOCK, Material.CAMPFIRE,
            Material.SOUL_CAMPFIRE, Material.CACTUS, Material.SWEET_BERRY_BUSH, Material.WITHER_ROSE,
            Material.POWDER_SNOW, Material.POINTED_DRIPSTONE, Material.END_PORTAL, Material.NETHER_PORTAL);

    private SafeLanding() {
    }

    public static Optional<Location> find(Location destination, int radius) {
        World world = destination.getWorld();
        if (world == null) {
            return Optional.empty();
        }
        int x = destination.getBlockX();
        int z = destination.getBlockZ();
        Column column = new Column() {
            @Override
            public int minY() {
                return world.getMinHeight();
            }

            @Override
            public int maxY() {
                return world.getMaxHeight();
            }

            @Override
            public Kind kind(int y) {
                return classify(world.getBlockAt(x, y, z));
            }
        };
        int startY = destination.getBlockY();
        OptionalInt found = findY(column, startY, Math.max(0, radius));
        if (found.isEmpty()) {
            return Optional.empty();
        }
        if (found.getAsInt() == startY) {
            return Optional.of(destination);
        }
        Location safe = destination.clone();
        safe.setY(found.getAsInt());
        return Optional.of(safe);
    }

    public static OptionalInt findY(Column column, int startY, int radius) {
        if (isSafe(column, startY)) {
            return OptionalInt.of(startY);
        }
        for (int offset = 1; offset <= radius; offset++) {
            if (isSafe(column, startY + offset)) {
                return OptionalInt.of(startY + offset);
            }
            if (isSafe(column, startY - offset)) {
                return OptionalInt.of(startY - offset);
            }
        }
        return OptionalInt.empty();
    }

    static boolean isSafe(Column column, int y) {
        if (y - 1 < column.minY() || y + 1 >= column.maxY()) {
            return false;
        }
        Kind ground = column.kind(y - 1);
        return (ground == Kind.SOLID || ground == Kind.WATER) && room(column.kind(y)) && room(column.kind(y + 1));
    }

    private static boolean room(Kind kind) {
        return kind == Kind.OPEN || kind == Kind.WATER;
    }

    static Kind classify(Block block) {
        Material type = block.getType();
        if (DANGER.contains(type)) {
            return Kind.DANGER;
        }
        if (type == Material.WATER) {
            return Kind.WATER;
        }
        return type.isSolid() ? Kind.SOLID : Kind.OPEN;
    }
}
