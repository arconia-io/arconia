package io.arconia.dev.ui.server.frontend;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Frontend tests of {@code arconia-page.js}, the element that loads a page of the Dev UI and
 * shows it.
 */
class PageFrontendIT extends BaseFrontendIT {

    @Test
    void devUiOpensOnTheOverviewOfTheApplication(Page page) {
        page.navigate(devUi("/"));

        assertThat(page).hasURL(pageUrl("overview"));
        assertThat(heading(page, "Overview")).isVisible();
    }

    /**
     * Each kind of page, built in or contributed, loads its content in the browser. A script
     * that fails to load, such as one importing a library missing from the import map, is
     * reported by the browser and fails the test.
     */
    @ParameterizedTest
    @CsvSource({ "overview, Overview", "properties, Properties", "environment, Environment", "services, Services",
            "settings, Settings" })
    void pageIsShownWithItsTitle(String pageId, String title, Page page) {
        page.navigate(pageUrl(pageId));

        assertThat(heading(page, title)).isVisible();
        assertThat(page).hasTitle(title + " · Application · Arconia Dev UI");
    }

    private static Locator heading(Page page, String title) {
        return page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName(title).setLevel(1));
    }

}
