package me.cedricrev.mseries.core.text;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public final class Text {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();
    private static final Pattern LEGACY_CODE = Pattern.compile("&(#[0-9a-fA-F]{6}|[0-9a-fk-orA-FK-OR])");
    private static final Map<Character, String> LEGACY_TAGS = Map.ofEntries(
            Map.entry('0', "<reset><black>"),
            Map.entry('1', "<reset><dark_blue>"),
            Map.entry('2', "<reset><dark_green>"),
            Map.entry('3', "<reset><dark_aqua>"),
            Map.entry('4', "<reset><dark_red>"),
            Map.entry('5', "<reset><dark_purple>"),
            Map.entry('6', "<reset><gold>"),
            Map.entry('7', "<reset><gray>"),
            Map.entry('8', "<reset><dark_gray>"),
            Map.entry('9', "<reset><blue>"),
            Map.entry('a', "<reset><green>"),
            Map.entry('b', "<reset><aqua>"),
            Map.entry('c', "<reset><red>"),
            Map.entry('d', "<reset><light_purple>"),
            Map.entry('e', "<reset><yellow>"),
            Map.entry('f', "<reset><white>"),
            Map.entry('k', "<obfuscated>"),
            Map.entry('l', "<bold>"),
            Map.entry('m', "<strikethrough>"),
            Map.entry('n', "<underlined>"),
            Map.entry('o', "<italic>"),
            Map.entry('r', "<reset>"));

    private Text() {
    }

    public static Component parse(String text, Arg... args) {
        TagResolver[] resolvers = new TagResolver[args.length];
        for (int i = 0; i < args.length; i++) {
            resolvers[i] = args[i].resolver();
        }
        return MINI_MESSAGE.deserialize(toMiniMessage(text), resolvers);
    }

    public static String legacy(Component component) {
        return LEGACY.serialize(component);
    }

    public static String bedrock(Component component) {
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    public static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    public static String escape(String text) {
        return MINI_MESSAGE.escapeTags(text);
    }

    static String toMiniMessage(String text) {
        if (text.indexOf('&') < 0) {
            return text;
        }
        Matcher matcher = LEGACY_CODE.matcher(text);
        StringBuilder converted = new StringBuilder();
        while (matcher.find()) {
            String code = matcher.group(1);
            String tag = code.charAt(0) == '#'
                    ? "<reset><" + code + ">"
                    : LEGACY_TAGS.get(Character.toLowerCase(code.charAt(0)));
            matcher.appendReplacement(converted, Matcher.quoteReplacement(tag));
        }
        matcher.appendTail(converted);
        return converted.toString();
    }
}
