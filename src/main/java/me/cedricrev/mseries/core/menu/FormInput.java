package me.cedricrev.mseries.core.menu;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import me.cedricrev.mseries.core.text.Text;

final class FormInput {

    static final char SEPARATOR = '¦';

    private static final Set<String> YES = Set.of("true", "yes", "on", "1");
    private static final Set<String> NO = Set.of("false", "no", "off", "0");

    record Result(FormResponse response, Field invalid) {

        boolean valid() {
            return this.response != null;
        }
    }

    private FormInput() {
    }

    static Map<String, String> initialValues(List<Field> fields) {
        Map<String, String> values = new LinkedHashMap<>();
        for (Field field : fields) {
            values.put(field.key(), field.initial());
        }
        return values;
    }

    static Result parse(List<Field> fields, Map<String, String> raw) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (Field field : fields) {
            Optional<Object> value = check(field, raw.getOrDefault(field.key(), ""));
            if (value.isEmpty()) {
                return new Result(null, field);
            }
            values.put(field.key(), value.get());
        }
        return new Result(new FormResponse(values), null);
    }

    static Optional<Object> check(Field field, String raw) {
        String value = raw == null ? "" : raw.strip();
        return switch (field.type()) {
            case TEXT -> value.length() >= field.min() && value.length() <= field.max()
                    ? Optional.of(value) : Optional.empty();
            case NUMBER -> number(field, value);
            case CHOICE -> choice(field, value);
            case TOGGLE -> toggle(value);
        };
    }

    static Optional<List<String>> split(String joined, int count) {
        List<String> parts = new ArrayList<>();
        String rest = joined == null ? "" : joined;
        int start = 0;
        for (int i = 0; i < rest.length(); i++) {
            if (rest.charAt(i) == SEPARATOR) {
                parts.add(rest.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(rest.substring(start));
        return parts.size() == count ? Optional.of(parts) : Optional.empty();
    }

    private static Optional<Object> number(Field field, String value) {
        String digits = value.startsWith("+") ? value.substring(1) : value;
        if (digits.isEmpty() || digits.length() > 11) {
            return Optional.empty();
        }
        try {
            long number = Long.parseLong(digits);
            if (number < field.min() || number > field.max()) {
                return Optional.empty();
            }
            return Optional.of((int) number);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static Optional<Object> choice(Field field, String value) {
        for (Field.Option option : field.options()) {
            if (option.id().equalsIgnoreCase(value)) {
                return Optional.of(option.id());
            }
        }
        for (Field.Option option : field.options()) {
            if (Text.plain(option.label()).strip().equalsIgnoreCase(value)) {
                return Optional.of(option.id());
            }
        }
        return Optional.empty();
    }

    private static Optional<Object> toggle(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        if (YES.contains(lower)) {
            return Optional.of(true);
        }
        if (NO.contains(lower)) {
            return Optional.of(false);
        }
        return Optional.empty();
    }
}
