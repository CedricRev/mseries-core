package me.cedricrev.mseries.core.menu;

import java.util.Map;

public final class FormResponse {

    private final Map<String, Object> values;

    FormResponse(Map<String, Object> values) {
        this.values = Map.copyOf(values);
    }

    public String text(String key) {
        return (String) value(key);
    }

    public int number(String key) {
        return (Integer) value(key);
    }

    public String choice(String key) {
        return (String) value(key);
    }

    public boolean toggle(String key) {
        return (Boolean) value(key);
    }

    private Object value(String key) {
        Object value = this.values.get(key);
        if (value == null) {
            throw new IllegalArgumentException("The form has no field '" + key + "'");
        }
        return value;
    }
}
