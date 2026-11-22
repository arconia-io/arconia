package io.arconia.dev.ui.api.page;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import io.arconia.core.annotations.Incubating;

/**
 * A piece of information with its label, which can have several values.
 * <p>
 * An item with a label and its values is created with {@link #of(String, String)}. The
 * {@link #builder() builder} also gives it an icon, or says that its values are code.
 * <p>
 * A value that is {@code null} is left out, so that an item built from the state of the
 * application doesn't have to check it. An item without any value is shown with a dash.
 */
@Incubating
public final class InfoItem implements CardItem {

    private final String label;

    private final List<String> values;

    @Nullable
    private final String icon;

    private final boolean code;

    private InfoItem(Builder builder) {
        Assert.hasText(builder.label, "label cannot be null or empty");
        String icon = (builder.icon != null && !builder.icon.isBlank()) ? builder.icon : null;
        if (icon != null) {
            Names.validate(icon, "icon");
        }
        this.label = builder.label;
        this.values = valuesOf(builder.values);
        this.icon = icon;
        this.code = builder.code;
    }

    private static List<String> valuesOf(@Nullable List<? extends @Nullable String> values) {
        List<String> known = new ArrayList<>();
        if (values != null) {
            for (String value : values) {
                if (value != null) {
                    known.add(value);
                }
            }
        }
        return List.copyOf(known);
    }

    /**
     * Creates an item that shows a piece of information with a single value.
     *
     * @param label the label of the information (e.g. {@code Image})
     * @param value the information itself, or {@code null} when it is not known
     */
    public static InfoItem of(String label, @Nullable String value) {
        return builder().label(label).value(value).build();
    }

    /**
     * Creates an item that shows a piece of information with several values.
     *
     * @param label the label of the information (e.g. {@code Ports})
     * @param values the information itself, which can be empty
     */
    public static InfoItem of(String label, @Nullable List<? extends @Nullable String> values) {
        return builder().label(label).values(values).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * The label of the information (e.g. {@code Ports}).
     */
    @Override
    public String getLabel() {
        return label;
    }

    /**
     * The information itself, as values that are never {@code null}. It is empty when the
     * information is not known.
     */
    public List<String> getValues() {
        return values;
    }

    /**
     * The name of the icon shown next to the label, from the
     * <a href="https://lucide.dev/icons">Lucide</a> icon set, if any.
     */
    @Nullable
    public String getIcon() {
        return icon;
    }

    /**
     * Whether the values are code, such as the name of a class or of a container image, which
     * is shown in a fixed-width font.
     */
    public boolean isCode() {
        return code;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof InfoItem item && this.label.equals(item.label) && this.values.equals(item.values)
                && Objects.equals(this.icon, item.icon) && this.code == item.code;
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, values, icon, code);
    }

    @Override
    public String toString() {
        return "InfoItem[label=%s, values=%s, icon=%s, code=%s]".formatted(label, values, icon, code);
    }

    /**
     * Builder for {@link InfoItem}, with no icon and values that are not code by default.
     */
    public static final class Builder {

        @Nullable
        private String label;
        @Nullable
        private List<? extends @Nullable String> values;
        @Nullable
        private String icon;
        private boolean code;

        private Builder() {}

        /**
         * The label of the information (e.g. {@code Ports}).
         */
        public Builder label(String label) {
            this.label = label;
            return this;
        }

        /**
         * The information itself, as a single value, or {@code null} when it is not known.
         */
        public Builder value(@Nullable String value) {
            this.values = Stream.ofNullable(value).toList();
            return this;
        }

        /**
         * The information itself, as values, which can be empty.
         */
        public Builder values(@Nullable List<? extends @Nullable String> values) {
            this.values = values;
            return this;
        }

        /**
         * The name of the icon shown next to the label, from the
         * <a href="https://lucide.dev/icons">Lucide</a> icon set, if any. It can only contain
         * lowercase letters, digits, and single hyphens between them. An item with the name
         * of an icon that doesn't exist is shown without one.
         */
        public Builder icon(@Nullable String icon) {
            this.icon = icon;
            return this;
        }

        /**
         * Whether the values are code, such as the name of a class or of a container image.
         * Code is shown in a fixed-width font.
         */
        public Builder code(boolean code) {
            this.code = code;
            return this;
        }

        public InfoItem build() {
            return new InfoItem(this);
        }

    }

}
