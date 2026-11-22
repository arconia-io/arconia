package io.arconia.dev.ui.server.frontend;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Frontend tests of {@code app.js}, the root element of the Dev UI: its navigation between the
 * pages, what it shows for an address without a page, and the color scheme.
 */
class DevUiFrontendIT extends BaseFrontendIT {

    private static final String BASE_TITLE = "Application · Arconia Dev UI";

    @Nested
    class Navigation {

        /**
         * Going to another page shows it without loading the Dev UI again, which would close
         * the console and its log. What a script left on the window would be gone after a
         * reload.
         */
        @Test
        void linkOfTheNavigationShowsThePageWithoutLoadingTheDevUiAgain(Page page) {
            page.navigate(pageUrl("overview"));
            assertThat(heading(page, "Overview")).isVisible();
            page.evaluate("window.loadedOnce = true");

            navigationLink(page, "Settings").click();

            assertThat(page).hasURL(pageUrl("settings"));
            assertThat(heading(page, "Settings")).isVisible();
            assertThat(page).hasTitle("Settings · " + BASE_TITLE);
            assertThat(page.evaluate("window.loadedOnce")).isEqualTo(true);
        }

        @Test
        void currentPageIsMarkedInTheNavigation(Page page) {
            page.navigate(pageUrl("overview"));

            navigationLink(page, "Settings").click();

            assertThat(navigationLink(page, "Settings")).hasAttribute("aria-current", "page");
            assertThat(navigationLink(page, "Overview")).not().hasAttribute("aria-current", "page");
        }

        @Test
        void backAndForwardSwitchPagesWithoutLoadingTheDevUiAgain(Page page) {
            page.navigate(pageUrl("overview"));
            assertThat(heading(page, "Overview")).isVisible();
            page.evaluate("window.loadedOnce = true");
            navigationLink(page, "Settings").click();
            assertThat(heading(page, "Settings")).isVisible();

            page.goBack();

            assertThat(page).hasURL(pageUrl("overview"));
            assertThat(heading(page, "Overview")).isVisible();

            page.goForward();

            assertThat(page).hasURL(pageUrl("settings"));
            assertThat(heading(page, "Settings")).isVisible();
            assertThat(page.evaluate("window.loadedOnce")).isEqualTo(true);
        }

        private static Locator navigationLink(Page page, String title) {
            return page.getByRole(AriaRole.NAVIGATION).getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName(title).setExact(true));
        }

    }

    @Nested
    class UnknownPage {

        @Test
        void pageTheApplicationDoesNotHaveIsNotFound(Page page) {
            expectFailedRequest("/api/pages/missing");

            page.navigate(pageUrl("missing"));

            assertThat(heading(page, "Page not found")).isVisible();
            assertThat(page.getByText("There is no page at this address.")).isVisible();
            assertThat(page).hasTitle(BASE_TITLE);
        }

        /**
         * An address that cannot be the one of a page isn't asked of the application, and
         * is shown the same as a page it doesn't have.
         */
        @Test
        void addressThatCannotBeTheOneOfAPageIsNotFound(Page page) {
            page.navigate(pageUrl("Not_A_Page"));

            assertThat(heading(page, "Page not found")).isVisible();
            assertThat(page.getByText("There is no page at this address.")).isVisible();
        }

        @Test
        void titleOfThePreviousPageIsNotKept(Page page) {
            page.navigate(pageUrl("settings"));
            assertThat(page).hasTitle("Settings · " + BASE_TITLE);

            page.evaluate("navigation.navigate('" + pageUrl("Not_A_Page") + "')");

            assertThat(heading(page, "Page not found")).isVisible();
            assertThat(page).hasTitle(BASE_TITLE);
        }

    }

    @Nested
    class ColorScheme {

        /**
         * The button goes from following the operating system to the light scheme, then the
         * dark one, and back. The choice is remembered, and applied before the page is shown
         * when it is loaded again.
         */
        @Test
        void chosenColorSchemeIsAppliedAndRemembered(Page page) {
            page.navigate(pageUrl("overview"));
            assertThat(colorScheme(page)).isEqualTo("");

            colorSchemeButton(page, "Color scheme: system. Click to use the light one.").click();

            assertThat(colorSchemeButton(page, "Color scheme: light. Click to use the dark one.")).isVisible();
            assertThat(colorScheme(page)).isEqualTo("light");

            colorSchemeButton(page, "Color scheme: light. Click to use the dark one.").click();
            page.reload();

            assertThat(colorSchemeButton(page, "Color scheme: dark. Click to follow the system.")).isVisible();
            assertThat(colorScheme(page)).isEqualTo("dark");

            colorSchemeButton(page, "Color scheme: dark. Click to follow the system.").click();
            page.reload();

            assertThat(colorSchemeButton(page, "Color scheme: system. Click to use the light one.")).isVisible();
            assertThat(colorScheme(page)).isEqualTo("");
        }

        private static Locator colorSchemeButton(Page page, String label) {
            return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(label).setExact(true));
        }

        private static Object colorScheme(Page page) {
            return page.evaluate("document.documentElement.style.colorScheme");
        }

    }

    private static Locator heading(Page page, String title) {
        return page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName(title).setLevel(1));
    }

}
