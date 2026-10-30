package me.cedricrev.mseries.core.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public final class Menu {

    public enum Kind {
        BUTTONS,
        CONFIRM,
        FORM
    }

    record FormParts(List<Field> fields, Component submit, Component cancel, BiConsumer<Player, FormResponse> onSubmit,
                     Function<Field, Component> invalid, Component chatHint, String cancelWord) {
    }

    private final Kind kind;
    private final Component title;
    private final Component body;
    private final List<Button> buttons;
    private final int rows;
    private final Component previousPage;
    private final Component nextPage;
    private final Consumer<Player> onClose;
    private final FormParts form;

    private Menu(Kind kind, Component title, Component body, List<Button> buttons, int rows,
                 Component previousPage, Component nextPage, Consumer<Player> onClose) {
        this(kind, title, body, buttons, rows, previousPage, nextPage, onClose, null);
    }

    private Menu(Kind kind, Component title, Component body, List<Button> buttons, int rows,
                 Component previousPage, Component nextPage, Consumer<Player> onClose, FormParts form) {
        this.kind = kind;
        this.title = title;
        this.body = body;
        this.buttons = List.copyOf(buttons);
        this.rows = rows;
        this.previousPage = previousPage;
        this.nextPage = nextPage;
        this.onClose = onClose;
        this.form = form;
    }

    public static Builder buttons(Component title) {
        return new Builder(title);
    }

    public static Menu confirm(Component title, Component body, Button yes, Button no) {
        return new Menu(Kind.CONFIRM, title, body, List.of(yes, no), 3, Component.text("<"), Component.text(">"), null);
    }

    public static FormBuilder form(Component title) {
        return new FormBuilder(title);
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

    public List<Field> fields() {
        return this.form == null ? List.of() : this.form.fields();
    }

    FormParts formParts() {
        return this.form;
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

    public static final class FormBuilder {

        private final Component title;
        private final List<Field> fields = new ArrayList<>();
        private Component body;
        private Component submit = Component.text("Submit");
        private Component cancel = Component.text("Cancel");
        private BiConsumer<Player, FormResponse> onSubmit = (player, response) -> {
        };
        private Function<Field, Component> invalid = field -> Component.text("Check this value: ", NamedTextColor.RED)
                .append(field.label());
        private Component chatHint = Component.text("Type your answer in chat, or 'cancel' to stop.",
                NamedTextColor.GRAY);
        private String cancelWord = "cancel";
        private Consumer<Player> onClose;

        private FormBuilder(Component title) {
            this.title = title;
        }

        public FormBuilder body(Component body) {
            this.body = body;
            return this;
        }

        public FormBuilder field(Field field) {
            for (Field existing : this.fields) {
                if (existing.key().equals(field.key())) {
                    throw new IllegalArgumentException("Duplicate form field '" + field.key() + "'");
                }
            }
            if (field.type() == Field.Type.CHOICE && field.options().isEmpty()) {
                throw new IllegalArgumentException("Choice field '" + field.key() + "' has no options");
            }
            this.fields.add(field);
            return this;
        }

        public FormBuilder submit(Component label, BiConsumer<Player, FormResponse> onSubmit) {
            this.submit = label;
            this.onSubmit = onSubmit;
            return this;
        }

        public FormBuilder cancel(Component label) {
            this.cancel = label;
            return this;
        }

        public FormBuilder invalid(Function<Field, Component> message) {
            this.invalid = message;
            return this;
        }

        public FormBuilder chatHint(Component hint, String cancelWord) {
            this.chatHint = hint;
            this.cancelWord = cancelWord;
            return this;
        }

        public FormBuilder onClose(Consumer<Player> onClose) {
            this.onClose = onClose;
            return this;
        }

        public Menu build() {
            if (this.fields.isEmpty()) {
                throw new IllegalStateException("A form needs at least one field");
            }
            FormParts parts = new FormParts(List.copyOf(this.fields), this.submit, this.cancel, this.onSubmit,
                    this.invalid, this.chatHint, this.cancelWord);
            return new Menu(Kind.FORM, this.title, this.body, List.of(), 0, Component.text("<"), Component.text(">"),
                    this.onClose, parts);
        }
    }
}
