package me.cedricrev.mseries.core.menu;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class Button {

    private final Component label;
    private final List<Component> lore;
    private final Material icon;
    private final Supplier<ItemStack> item;
    private final String image;
    private final int slot;
    private final boolean chestOnly;
    private final Consumer<Player> action;

    private Button(Builder builder) {
        this.label = builder.label;
        this.lore = List.copyOf(builder.lore);
        this.icon = builder.icon;
        this.item = builder.item;
        this.image = builder.image;
        this.slot = builder.slot;
        this.chestOnly = builder.chestOnly;
        this.action = builder.action;
    }

    public static Builder of(Component label) {
        return new Builder(label);
    }

    public Component label() {
        return this.label;
    }

    public List<Component> lore() {
        return this.lore;
    }

    public Material icon() {
        return this.icon;
    }

    public Optional<Supplier<ItemStack>> item() {
        return Optional.ofNullable(this.item);
    }

    public Optional<String> image() {
        return Optional.ofNullable(this.image);
    }

    public int slot() {
        return this.slot;
    }

    public boolean chestOnly() {
        return this.chestOnly;
    }

    public Optional<Consumer<Player>> action() {
        return Optional.ofNullable(this.action);
    }

    public static final class Builder {

        private final Component label;
        private List<Component> lore = List.of();
        private Material icon = Material.PAPER;
        private Supplier<ItemStack> item;
        private String image;
        private int slot = -1;
        private boolean chestOnly;
        private Consumer<Player> action;

        private Builder(Component label) {
            this.label = label;
        }

        public Builder lore(List<Component> lore) {
            this.lore = lore;
            return this;
        }

        public Builder icon(Material icon) {
            this.icon = icon;
            return this;
        }

        public Builder item(Supplier<ItemStack> item) {
            this.item = item;
            return this;
        }

        public Builder image(String image) {
            this.image = image;
            return this;
        }

        public Builder slot(int slot) {
            this.slot = slot;
            return this;
        }

        public Builder chestOnly() {
            this.chestOnly = true;
            return this;
        }

        public Builder action(Consumer<Player> action) {
            this.action = action;
            return this;
        }

        public Button build() {
            return new Button(this);
        }
    }
}
