package io.arconia.dev.ui.server.frontend;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.arconia.dev.ui.server.frontend.FrontendTestApplication.Settings;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Frontend tests of {@code arconia-table-page.js}, the element that shows the table of a page,
 * here the five settings of {@link FrontendTestApplication}.
 */
class TablePageFrontendIT extends BaseFrontendIT {

    @Nested
    class Filter {

        /**
         * The filter is kept in the address of the page, which the browser reports as a
         * navigation. Showing the page anew for it would take the focus away after each key.
         */
        @Test
        void filterKeepsTheFocusWhileTyping(Page page) {
            page.navigate(pageUrl("settings"));

            filter(page).pressSequentially("orders.");

            assertThat(filter(page)).isFocused();
            assertThat(filter(page)).hasValue("orders.");
            assertThat(page).hasURL(pageUrl("settings") + "?filter=orders.");
            assertThat(count(page)).hasText("3 of 5 rows");
        }

        @Test
        void filterInTheAddressIsAppliedWhenThePageOpens(Page page) {
            page.navigate(pageUrl("settings") + "?filter=catalog.");

            assertThat(filter(page)).hasValue("catalog.");
            assertThat(count(page)).hasText("2 of 5 rows");

            page.reload();

            assertThat(filter(page)).hasValue("catalog.");
            assertThat(count(page)).hasText("2 of 5 rows");
        }

        @Test
        void clearedFilterIsRemovedFromTheAddress(Page page) {
            page.navigate(pageUrl("settings") + "?filter=catalog.");

            filter(page).fill("");

            assertThat(page).hasURL(pageUrl("settings"));
            assertThat(count(page)).hasText("5 rows");
        }

    }

    @Nested
    class Secrets {

        @Test
        void secretIsShownOnlyWhenAskedFor(Page page) {
            page.navigate(pageUrl("settings") + "?filter=catalog.password");
            assertThat(cell(page, "••••••••")).isVisible();

            button(page, "Show the value").click();

            assertThat(cell(page, "hunter2")).isVisible();

            button(page, "Hide the value").click();

            assertThat(cell(page, "••••••••")).isVisible();
            assertThat(cell(page, "hunter2")).hasCount(0);
        }

        /**
         * Typing part of a secret doesn't tell, by the rows left, whether it is right, even
         * once the secret is shown.
         */
        @Test
        void secretIsNeverMatchedByTheFilter(Page page) {
            page.navigate(pageUrl("settings") + "?filter=catalog.password");
            button(page, "Show the value").click();

            filter(page).fill("hunter2");

            assertThat(count(page)).hasText("0 of 5 rows");
            assertThat(page.getByText("No rows match the filter.")).isVisible();
        }

    }

    @Nested
    class Groups {

        @AfterEach
        void restoreSettings() {
            bean(Settings.class).reset();
        }

        @Test
        void chosenKindShowsItsRowsOnly(Page page) {
            page.navigate(pageUrl("settings"));

            chooseKind(page, "Credential");

            assertThat(count(page)).hasText("2 of 5 rows");
            assertThat(cell(page, "catalog.password")).isVisible();
            assertThat(cell(page, "orders.token")).isVisible();
        }

        /**
         * The content of a page is loaded again when the developer comes back to the window,
         * and may no longer have the kind that was chosen.
         */
        @Test
        void kindThatIsGoneIsNoLongerChosen(Page page) {
            page.navigate(pageUrl("settings"));
            chooseKind(page, "Credential");
            assertThat(count(page)).hasText("2 of 5 rows");

            bean(Settings.class).remove("Credential");
            page.evaluate("window.dispatchEvent(new Event('focus'))");

            assertThat(count(page)).hasText("3 rows");
            assertThat(kind(page)).hasText("Any kind");
        }

        private static void chooseKind(Page page, String kind) {
            kind(page).click();
            page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(kind)).click();
        }

        /**
         * The select of the kinds, which shows the chosen one.
         */
        private static Locator kind(Page page) {
            return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Kind"));
        }

    }

    @Nested
    class Details {

        @Test
        void clickOnARowOpensAndClosesItsDetails(Page page) {
            page.navigate(pageUrl("settings") + "?filter=orders.retries");

            cell(page, "orders.retries").click();

            assertThat(toggle(page, "Details shown")).isVisible();
            assertThat(page.getByText("application.properties")).isVisible();

            cell(page, "orders.retries").click();

            assertThat(toggle(page, "Details hidden")).isVisible();
        }

        /**
         * The table itself would use the Enter key to move into a cell, but it opens and
         * closes a row as the Space key does.
         */
        @Test
        void enterOnARowOpensAndClosesItsDetails(Page page) {
            page.navigate(pageUrl("settings") + "?filter=orders.retries");
            cell(page, "orders.retries").click();
            assertThat(toggle(page, "Details shown")).isVisible();

            page.keyboard().press("Enter");

            assertThat(toggle(page, "Details hidden")).isVisible();

            page.keyboard().press("Enter");

            assertThat(toggle(page, "Details shown")).isVisible();
        }

        @Test
        void enterOnTheButtonOfASecretShowsItWithoutOpeningTheRow(Page page) {
            page.navigate(pageUrl("settings") + "?filter=catalog.password");

            button(page, "Show the value").focus();
            page.keyboard().press("Enter");

            assertThat(cell(page, "hunter2")).isVisible();
            assertThat(toggle(page, "Details hidden")).isVisible();
        }

        private static Locator toggle(Page page, String state) {
            return page.locator("vaadin-grid-cell-content:visible vaadin-icon.toggle[aria-label='" + state + "']");
        }

    }

    private static Locator filter(Page page) {
        return page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Filter the rows of the table"));
    }

    private static Locator count(Page page) {
        return page.locator("arconia-table-page .count");
    }

    /**
     * The content of a cell on screen. The table reuses the elements of its cells, and keeps
     * some of them out of view, so only the visible ones count.
     */
    private static Locator cell(Page page, String text) {
        return page.locator("vaadin-grid vaadin-grid-cell-content:visible", new Page.LocatorOptions().setHasText(text));
    }

    private static Locator button(Page page, String name) {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name));
    }

}
