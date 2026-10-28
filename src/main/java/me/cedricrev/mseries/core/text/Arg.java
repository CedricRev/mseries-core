package me.cedricrev.mseries.core.text;

import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public final class Arg {

    private final TagResolver resolver;

    private Arg(TagResolver resolver) {
        this.resolver = resolver;
    }

    public static Arg of(String name, Object value) {
        return new Arg(Placeholder.unparsed(name, String.valueOf(value)));
    }

    public static Arg formatted(String name, String value) {
        return new Arg(Placeholder.parsed(name, Text.toMiniMessage(value)));
    }

    public static Arg component(String name, ComponentLike value) {
        return new Arg(Placeholder.component(name, value));
    }

    TagResolver resolver() {
        return this.resolver;
    }
}
