package me.cedricrev.mseries.core.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;

public final class Field {

    public enum Type {
        TEXT,
        NUMBER,
        CHOICE,
        TOGGLE
    }

    public record Option(String id, Component label) {
    }

    private static final int TEXT_MAX_LENGTH = 64;

    private final Type type;
    private final String key;
    private final Component label;
    private final String initial;
    private final int min;
    private final int max;
    private final List<Option> options;
    private final Component placeholder;

    private Field(Type type, String key, Component label, String initial, int min, int max, List<Option> options,
                  Component placeholder) {
        this.type = type;
        this.key = key;
        this.label = label;
        this.initial = initial;
        this.min = min;
        this.max = max;
        this.options = List.copyOf(options);
        this.placeholder = placeholder;
    }

    public static Field text(String key, Component label) {
        return new Field(Type.TEXT, key, label, "", 0, TEXT_MAX_LENGTH, List.of(), null);
    }

    public static Field number(String key, Component label) {
        return new Field(Type.NUMBER, key, label, "", Integer.MIN_VALUE, Integer.MAX_VALUE, List.of(), null);
    }

    public static Field choice(String key, Component label) {
        return new Field(Type.CHOICE, key, label, "", 0, 0, List.of(), null);
    }

    public static Field toggle(String key, Component label) {
        return new Field(Type.TOGGLE, key, label, "false", 0, 0, List.of(), null);
    }

    public Field initial(String value) {
        return new Field(this.type, this.key, this.label, value == null ? "" : value, this.min, this.max,
                this.options, this.placeholder);
    }

    public Field initial(int value) {
        return initial(Integer.toString(value));
    }

    public Field initial(boolean value) {
        return initial(Boolean.toString(value));
    }

    public Field range(int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("min " + min + " is greater than max " + max);
        }
        return new Field(this.type, this.key, this.label, this.initial, min, max, this.options, this.placeholder);
    }

    public Field option(String id, Component label) {
        List<Option> next = new ArrayList<>(this.options);
        next.add(new Option(id, label));
        String first = this.options.isEmpty() && this.initial.isEmpty() ? id : this.initial;
        return new Field(this.type, this.key, this.label, first, this.min, this.max, next, this.placeholder);
    }

    public Field placeholder(Component placeholder) {
        return new Field(this.type, this.key, this.label, this.initial, this.min, this.max, this.options,
                placeholder);
    }

    public Type type() {
        return this.type;
    }

    public String key() {
        return this.key;
    }

    public Component label() {
        return this.label;
    }

    public String initial() {
        return this.initial;
    }

    public int min() {
        return this.min;
    }

    public int max() {
        return this.max;
    }

    public List<Option> options() {
        return this.options;
    }

    public Optional<Component> placeholder() {
        return Optional.ofNullable(this.placeholder);
    }

    int optionIndex(String id) {
        for (int i = 0; i < this.options.size(); i++) {
            if (this.options.get(i).id().equals(id)) {
                return i;
            }
        }
        return 0;
    }
}
