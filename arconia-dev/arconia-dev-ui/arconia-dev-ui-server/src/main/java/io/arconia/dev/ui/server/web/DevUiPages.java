package io.arconia.dev.ui.server.web;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.arconia.dev.ui.api.page.DevUiPage;

/**
 * The pages contributed to the Dev UI, in the order they appear in the navigation.
 */
final class DevUiPages {

    private static final Logger logger = LoggerFactory.getLogger(DevUiPages.class);

    /**
     * The identifier of the overview of the application, the page the Dev UI itself provides
     * and opens on.
     */
    static final String OVERVIEW_ID = "overview";

    /**
     * The pages without a section and the sections come by order and then by title. The pages
     * of a section stay together, by order and then by title among themselves.
     */
    private static final Comparator<DevUiPage> NAVIGATION_ORDER = Comparator
            .comparingInt((DevUiPage page) -> (page.getSection() != null) ? page.getSection().getOrder() : page.getOrder())
            .thenComparing(page -> (page.getSection() != null) ? page.getSection().getTitle() : page.getTitle(), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(page -> (page.getSection() != null) ? page.getSection().getId() : "")
            .thenComparingInt(DevUiPage::getOrder)
            .thenComparing(DevUiPage::getTitle, String.CASE_INSENSITIVE_ORDER);

    private final Map<String, DevUiPage> pages = new LinkedHashMap<>();

    private final Map<String, String> icons = new HashMap<>();

    private final DevUiIcons iconSet;

    /**
     * When two pages have the same identifier, the first one in navigation order is kept and
     * the other is left out with a warning. A page with an icon that the icon set doesn't have
     * gets a generic one, also with a warning. Neither must stop the application from starting.
     */
    DevUiPages(Collection<DevUiPage> pages, DevUiIcons icons) {
        this.iconSet = icons;
        // No page can have the identifier of the overview: the builders of the pages reject it.
        pages.stream().sorted(NAVIGATION_ORDER).forEach(page -> {
            DevUiPage existingPage = this.pages.putIfAbsent(page.getId(), page);
            if (existingPage != null) {
                logger.warn("The Dev UI page '{}' is left out because the page '{}' has the same identifier '{}'",
                        page.getTitle(), existingPage.getTitle(), page.getId());
            } else if (icons.has(page.getIcon())) {
                this.icons.put(page.getId(), page.getIcon());
            } else {
                logger.warn("The Dev UI page '{}' is shown with a generic icon because there is no icon named '{}'",
                        page.getTitle(), page.getIcon());
                this.icons.put(page.getId(), DevUiIcons.FALLBACK);
            }
        });
    }

    List<DevUiPage> all() {
        return List.copyOf(this.pages.values());
    }

    /**
     * The icon to show for the given page, which is always one that the icon set has.
     */
    String icon(DevUiPage page) {
        return this.icons.getOrDefault(page.getId(), DevUiIcons.FALLBACK);
    }

    /**
     * Whether the icon set has an icon with the given name.
     */
    boolean hasIcon(String name) {
        return this.iconSet.has(name);
    }

    @Nullable
    DevUiPage find(String id) {
        return this.pages.get(id);
    }

}
