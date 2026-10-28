package me.cedricrev.mseries.core.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public final class Menu {

    public enum Kind {
        BUTTONS,
        CONFIRM
    }

    private final Kind kind;
    private final Component title;
    private final Component body;
    private final List<Button> buttons;
    private final int rows;
    private final Component previousPage;
    private final Component nextPage;
    private final Consumer<Player> onClose;

    private Menu(Kind kind, Component title, Component body, List<Button> buttons, int rows,
                 Component previousPage, Component nextPage, Consumer<Player> onClose) {
        this.kind = kind;
        this.title = title;
        this.body = body;
        this.buttons = List.copyOf(buttons);
        this.rows = rows;
        this.previousPage = previousPage;
        this.nextPage = nextPage;
        this.onClose = onClose;
    }

    public static Builder buttons(Component title) {
        return new Builder(title);
    }

    public static Menu confirm(Component title, Component body, Button yes, Button no) {
        return new Menu(Kind.CONFIRM, title, body, List.of(yes, no), 3, Component.text("<"), Component.text(">"), null);
    }

    public Kind kind() {
        return this.kind;
    }

    public Component title() {
        return this.title;
    }

    public Optional<Component> body() {
        return Optional.ofNullable(this.body);
    }

    public List<Button> buttons() {
        return this.buttons;
    }

    public int rows() {
        return this.rows;
    }

    public Component previousPage() {
        return this.previousPage;
    }

    public Component nextPage() {
        return this.nextPage;
    }

    public Optional<Consumer<Player>> onClose() {
        return Optional.ofNullable(this.onClose);
    }

    public static final class Builder {

        private final Component title;
        private final List<Button> buttons = new ArrayList<>();
        private Component body;
        private int rows;
        private Component previousPage = Component.text("<");
        private Component nextPage = Component.text(">");
        private Consumer<Player> onClose;

        private Builder(Component title) {
            this.title = title;
        }

        public Builder body(Component body) {
            this.body = body;
            return this;
        }

        public Builder button(Button button) {
            this.buttons.add(button);
            return this;
        }

        public Builder button(Button.Builder button) {
            return button(button.build());
        }

        public Builder rows(int rows) {
            this.rows = Math.max(0, Math.min(6, rows));
            return this;
        }

        public Builder pageLabels(Component previous, Component next) {
            this.previousPage = previous;
            this.nextPage = next;
            return this;
        }

        public Builder onClose(Consumer<Player> onClose) {
            this.onClose = onClose;
            return this;
        }

        public Menu build() {
            return new Menu(Kind.BUTTONS, this.title, this.body, this.buttons, this.rows, this.previousPage,
                    this.nextPage, this.onClose);
        }
    }
}
