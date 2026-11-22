package io.arconia.dev.ui.api.page;

import java.util.List;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import io.arconia.core.annotations.Incubating;

/**
 * A page of the Dev UI that shows a set of cards.
 */
@Incubating
public final class CardsPage implements DevUiPage {

    private static final String DEFAULT_ICON = "layout-grid";

    private static final int DEFAULT_COLUMNS = 3;

    private final String id;

    private final String title;

    private final String icon;

    private final int order;

    @Nullable
    private final DevUiSection section;

    private final int maxColumns;

    private final Supplier<List<Card>> cards;

    private CardsPage(Builder builder) {
        this.id = Names.validatePageId(builder.id);
        Assert.hasText(builder.title, "title cannot be null or empty");
        Assert.isTrue(builder.maxColumns >= 1, "maxColumns cannot be less than 1");
        Assert.notNull(builder.cards, "cards cannot be null");
        this.title = builder.title;
        this.icon = Names.validate(builder.icon, "icon");
        this.order = builder.order;
        this.section = builder.section;
        this.maxColumns = builder.maxColumns;
        this.cards = builder.cards;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getIcon() {
        return icon;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    @Nullable
    public DevUiSection getSection() {
        return section;
    }

    /**
     * The number of columns the cards are laid out in, at most. There are fewer of them when
     * the page is not wide enough.
     */
    public int getMaxColumns() {
        return maxColumns;
    }

    /**
     * Supplies the cards of the page, called whenever the Dev UI needs them.
     */
    public Supplier<List<Card>> getCards() {
        return cards;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link CardsPage}, with a generic icon, an order of 0, and 3 columns by
     * default.
     */
    public static final class Builder {

        @Nullable
        private String id;
        @Nullable
        private String title;
        private String icon = DEFAULT_ICON;
        private int order;
        @Nullable
        private DevUiSection section;
        private int maxColumns = DEFAULT_COLUMNS;
        @Nullable
        private Supplier<List<Card>> cards;

        private Builder() {}

        /**
         * The identifier of the page, unique among the pages of the Dev UI and used in its URL.
         * It can only contain lowercase letters, digits, and single hyphens between them, and it
         * cannot be {@code overview}, which is the identifier of the page the Dev UI opens on.
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * The title of the page, shown in the navigation.
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * The icon of the page, shown next to its title in the navigation. It is the name of
         * an icon of the <a href="https://lucide.dev/icons">Lucide</a> icon set, such as
         * {@code database}.
         */
        public Builder icon(String icon) {
            this.icon = icon;
            return this;
        }

        /**
         * The position of the page in the navigation, or among the pages of its section when
         * it has one. Pages with a lower value come first, and pages with the same value are
         * sorted by title.
         */
        public Builder order(int order) {
            this.order = order;
            return this;
        }

        /**
         * The section of the navigation the page is listed in, if any. A page without a
         * section is listed on its own.
         */
        public Builder section(@Nullable DevUiSection section) {
            this.section = section;
            return this;
        }

        /**
         * The number of columns the cards are laid out in, at most. There are fewer of them
         * when the page is not wide enough. Use fewer columns for cards with a lot of content.
         */
        public Builder maxColumns(int maxColumns) {
            this.maxColumns = maxColumns;
            return this;
        }

        /**
         * Supplies the cards of the page. The supplier is called whenever the Dev UI needs
         * the cards and never when the application starts, so it can read the state of the
         * application. That can be several times while someone looks at the page, for example
         * when they come back to it or open one of its logs, so the supplier should be quick and
         * change nothing in the application. Opening a log supplies the cards again to find it,
         * so a log must not depend on the cards supplied before.
         * <p>
         * The supplier runs on the thread of the request that needs the cards, and two requests
         * can call it at the same time, so it must be safe to call from several threads. When
         * it fails, or returns {@code null}, the failure is logged and shown in place of the
         * page, and the application is not affected.
         */
        public Builder cards(Supplier<List<Card>> cards) {
            this.cards = cards;
            return this;
        }

        public CardsPage build() {
            return new CardsPage(this);
        }

    }

}
