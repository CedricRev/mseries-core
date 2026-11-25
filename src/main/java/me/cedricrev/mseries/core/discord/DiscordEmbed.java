package me.cedricrev.mseries.core.discord;

import java.util.ArrayList;
import java.util.List;

public record DiscordEmbed(String author, String authorIcon, String title, String description, Integer color,
                           String thumbnail, List<Field> fields, String footer, String footerIcon, boolean timestamp) {

    public record Field(String name, String value, boolean inline) {

        public Field {
            name = name == null ? "" : name;
            value = value == null ? "" : value;
        }
    }

    public DiscordEmbed {
        author = clean(author);
        authorIcon = clean(authorIcon);
        title = clean(title);
        description = clean(description);
        thumbnail = clean(thumbnail);
        fields = fields == null ? List.of() : List.copyOf(fields);
        footer = clean(footer);
        footerIcon = clean(footerIcon);
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isEmpty() {
        return this.author.isEmpty() && this.title.isEmpty() && this.description.isEmpty() && this.fields.isEmpty()
                && this.footer.isEmpty() && this.thumbnail.isEmpty();
    }

    private static String clean(String text) {
        return text == null ? "" : text;
    }

    public static final class Builder {

        private String author = "";
        private String authorIcon = "";
        private String title = "";
        private String description = "";
        private Integer color;
        private String thumbnail = "";
        private final List<Field> fields = new ArrayList<>();
        private String footer = "";
        private String footerIcon = "";
        private boolean timestamp;

        private Builder() {
        }

        public Builder author(String name, String icon) {
            this.author = name;
            this.authorIcon = icon;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder color(Integer color) {
            this.color = color;
            return this;
        }

        public Builder thumbnail(String url) {
            this.thumbnail = url;
            return this;
        }

        public Builder field(String name, String value, boolean inline) {
            this.fields.add(new Field(name, value, inline));
            return this;
        }

        public Builder footer(String text, String icon) {
            this.footer = text;
            this.footerIcon = icon;
            return this;
        }

        public Builder timestamp(boolean timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public DiscordEmbed build() {
            return new DiscordEmbed(this.author, this.authorIcon, this.title, this.description, this.color,
                    this.thumbnail, this.fields, this.footer, this.footerIcon, this.timestamp);
        }
    }
}
