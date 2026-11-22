package io.arconia.dev.ui.server.frontend;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Frontend tests of {@code arconia-log-view.js}, the element that follows a log, here the logs
 * of the catalog card of {@link FrontendTestApplication}, opened in their dialog.
 */
class LogViewFrontendIT extends BaseFrontendIT {

    @Test
    void logIsShownUntilItEnds(Page page) {
        openLog(page, "Access log");

        assertThat(line(page, "GET /orders 200")).isVisible();
        assertThat(line(page, "GET /orders/42 404")).isVisible();
        assertThat(count(page)).hasText("3 lines. The log has ended.");
    }

    @Test
    void logThatCannotBeFollowedSaysWhy(Page page) {
        openLog(page, "Audit log");

        assertThat(count(page)).hasText("0 lines. The log cannot be followed: The audit trail is offline");
    }

    @Test
    void filterShowsTheLinesWithItsText(Page page) {
        openLog(page, "Server log");
        assertThat(count(page)).hasText("3 lines");

        dialog(page).getByRole(AriaRole.TEXTBOX, new Locator.GetByRoleOptions().setName("Filter the lines of the log")).fill("error");

        assertThat(count(page)).hasText("1 of 3 lines");
        assertThat(line(page, "Connection error on port 8081")).isVisible();
        assertThat(line(page, "Server started")).isHidden();
    }

    /**
     * What ends the sentence after a web address is not part of it.
     */
    @Test
    void webAddressInALineIsALink(Page page) {
        openLog(page, "Access log");

        Locator link = dialog(page).getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("https://shop.example.com/guide").setExact(true));
        assertThat(link).hasAttribute("href", "https://shop.example.com/guide");
        assertThat(line(page, "See the guide at")).hasText("See the guide at https://shop.example.com/guide.");
    }

    private static void openLog(Page page, String label) {
        page.navigate(pageUrl("services"));
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(label).setExact(true)).click();
        assertThat(dialog(page)).hasAttribute("opened", "");
    }

    /**
     * The dialog of the log. Its content is shown in an overlay, so the dialog itself never
     * counts as visible.
     */
    private static Locator dialog(Page page) {
        return page.locator("vaadin-dialog");
    }

    private static Locator count(Page page) {
        return dialog(page).locator(".count");
    }

    private static Locator line(Page page, String text) {
        return dialog(page).locator(".line", new Locator.LocatorOptions().setHasText(text));
    }

}
