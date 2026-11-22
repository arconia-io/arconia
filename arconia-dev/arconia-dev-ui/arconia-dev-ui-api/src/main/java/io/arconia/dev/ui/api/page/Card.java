package io.arconia.dev.ui.api.page;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import io.arconia.core.annotations.Incubating;

/**
 * A card on a {@link CardsPage}, describing one thing with a title and a list of items.
 */
@Incubating
public final class Card {

    private final String id;

    private final String title;

    @Nullable
    private final String description;

    @Nullable
    private final String icon;

    @Nullable
    private final String documentation;

    @Nullable
    private final String propertiesPrefix;

    @Nullable
    private final Status status;

    private final List<CardItem> items;

    private Card(Builder builder) {
        // The content of a card usually comes from the application as it runs, where a text
        // that is not there is often an empty one. What is optional is then simply left out.
        String icon = textOrNull(builder.icon);
        String documentation = textOrNull(builder.documentation);
        Assert.hasText(builder.id, "id cannot be null or empty");
        Assert.hasText(builder.title, () -> "title of the card '%s' cannot be null or empty".formatted(builder.id));
        if (icon != null) {
            Names.validate(icon, "icon");
        }
        Assert.isTrue(documentation == null || LinkItem.isHttpUrl(documentation),
                () -> "documentation '%s' of the card '%s' must be an HTTP or HTTPS URL".formatted(documentation, builder.id));
        Assert.notNull(builder.items, "items cannot be null");
        Assert.noNullElements(builder.items, () -> "items of the card '%s' cannot contain null elements".formatted(builder.id));
        Set<String> logLabels = new HashSet<>();
        for (CardItem item : builder.items) {
            Assert.isTrue(!(item instanceof LogItem) || logLabels.add(item.getLabel()),
                    () -> "items of the card '%s' cannot contain several logs with the label '%s'".formatted(builder.id, item.getLabel()));
        }
        this.id = builder.id;
        this.title = builder.title;
        this.description = textOrNull(builder.description);
        this.icon = icon;
        this.documentation = documentation;
        this.propertiesPrefix = textOrNull(builder.propertiesPrefix);
        this.status = builder.status;
        this.items = List.copyOf(builder.items);
    }

    /**
     * The identifier of the card, which must be unique among the cards of its page.
     */
    public String getId() {
        return id;
    }

    /**
     * The title of the card.
     */
    public String getTitle() {
        return title;
    }

    /**
     * The description of the card, if any.
     */
    @Nullable
    public String getDescription() {
        return description;
    }

    /**
     * The name of the icon shown next to the title of the card, if any.
     */
    @Nullable
    public String getIcon() {
        return icon;
    }

    /**
     * The URL of the documentation of what the card is about, if any.
     */
    @Nullable
    public String getDocumentation() {
        return documentation;
    }

    /**
     * What the names of the configuration properties about the subject of the card start
     * with, if any.
     */
    @Nullable
    public String getPropertiesPrefix() {
        return propertiesPrefix;
    }

    /**
     * The status of what the card describes, if any.
     */
    @Nullable
    public Status getStatus() {
        return status;
    }

    /**
     * The items of the card, in the order they are shown. The logs among them have different
     * labels.
     */
    public List<CardItem> getItems() {
        return items;
    }

    private static @Nullable String textOrNull(@Nullable String text) {
        return (text != null && !text.isBlank()) ? text : null;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Card}, with no items by default.
     */
    public static final class Builder {

        @Nullable
        private String id;
        @Nullable
        private String title;
        @Nullable
        private String description;
        @Nullable
        private String icon;
        @Nullable
        private String documentation;
        @Nullable
        private String propertiesPrefix;
        @Nullable
        private Status status;
        private List<? extends CardItem> items = List.of();

        private Builder() {}

        /**
         * The identifier of the card, which must be unique among the cards of its page. It can be any text
         * that tells the card apart, such as the name of what the card is about.
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * The title of the card.
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * The description of the card, if any.
         */
        public Builder description(@Nullable String description) {
            this.description = description;
            return this;
        }

        /**
         * The name of the icon shown next to the title of the card, from the
         * <a href="https://lucide.dev/icons">Lucide</a> icon set, if any. It can only contain
         * lowercase letters, digits, and single hyphens between them. A card with the name of
         * an icon that doesn't exist is shown without one.
         */
        public Builder icon(@Nullable String icon) {
            this.icon = icon;
            return this;
        }

        /**
         * The URL of the documentation of what the card is about, if any. The card links to
         * it from its footer. It must be an HTTP or HTTPS URL.
         */
        public Builder documentation(@Nullable String documentation) {
            this.documentation = documentation;
            return this;
        }

        /**
         * What the names of the configuration properties about the subject of the card start
         * with, such as {@code spring.datasource}, if any. The card links from its footer to
         * the configuration properties under it.
         */
        public Builder propertiesPrefix(@Nullable String propertiesPrefix) {
            this.propertiesPrefix = propertiesPrefix;
            return this;
        }

        /**
         * The status of what the card describes, if any. It is shown next to the title of
         * the card.
         */
        public Builder status(@Nullable Status status) {
            this.status = status;
            return this;
        }

        /**
         * The items of the card, in the order they are shown. A log is found by its label
         * when someone opens it, so the logs of a card must have different labels.
         */
        public Builder items(List<? extends CardItem> items) {
            this.items = items;
            return this;
        }

        /**
         * The items of the card, in the order they are shown.
         */
        public Builder items(CardItem... items) {
            Assert.notNull(items, "items cannot be null");
            return items(Arrays.asList(items));
        }

        public Card build() {
            return new Card(this);
        }

    }

}
