package io.arconia.dev.ui.api.page;

import org.jspecify.annotations.Nullable;

import io.arconia.core.annotations.Incubating;

/**
 * A page of the Dev UI. A library contributes a page by declaring a bean of one of the
 * permitted types.
 */
@Incubating
public sealed interface DevUiPage permits CardsPage, TablePage {

    /**
     * The identifier of the page, unique among the pages of the Dev UI and used in its URL.
     */
    String getId();

    /**
     * The title of the page, shown in the navigation.
     */
    String getTitle();

    /**
     * The icon of the page, shown next to its title in the navigation. It is the name of an
     * icon of the <a href="https://lucide.dev/icons">Lucide</a> icon set, such as
     * {@code database}.
     */
    String getIcon();

    /**
     * The position of the page in the navigation, or among the pages of its section when it
     * has one. Pages with a lower value come first, and pages with the same value are sorted
     * by title.
     */
    int getOrder();

    /**
     * The section of the navigation the page is listed in, if any.
     */
    @Nullable
    DevUiSection getSection();

}
