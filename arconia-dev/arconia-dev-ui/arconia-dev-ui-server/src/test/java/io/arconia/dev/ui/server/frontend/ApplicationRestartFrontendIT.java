package io.arconia.dev.ui.server.frontend;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Frontend tests of what the Dev UI does while the application restarts, as it does during
 * development: it waits for the application to come back, then loads itself again, since the
 * application may have changed, and tells the developer why.
 * <p>
 * The behavior is spread over {@code api.js}, {@code app.js}, {@code arconia-page.js} and
 * {@code arconia-console.js}, so the tests are named after the scenario.
 */
class ApplicationRestartFrontendIT extends BaseFrontendIT {

    private static final Logger logger = LoggerFactory.getLogger("com.example.shop.OrderService");

    /**
     * While the application is away, the requests of the Dev UI cannot reach it, which the
     * browser reports as errors: the ones to its API, and the ones for what it shows for the
     * first time, such as the mark of the message that it is waiting.
     */
    @BeforeEach
    void expectRequestsWhileTheApplicationIsAway() {
        expectFailedRequest("/");
    }

    /**
     * The console finds out first, since it follows the logs of the application all along.
     */
    @Test
    void openConsoleWaitsForTheApplicationAndTheDevUiLoadsAgainWhenItIsBack(Page page) {
        page.navigate(pageUrl("overview"));
        consoleButton(page).click();
        logger.info("Last order before the restart");
        assertThat(line(page, "Last order before the restart")).isVisible();
        page.evaluate("window.loadedOnce = true");

        restartApplication(() -> assertThat(page.locator("arconia-console .count")).containsText("Waiting for the application..."));

        assertThat(page.getByText("The application was restarted.")).isVisible();
        assertThat(page.evaluate("window.loadedOnce")).isNull();
        assertThat(heading(page, "Overview")).isVisible();
        // The console shows where the earlier run of the application ends.
        assertThat(page.locator("arconia-console [role=log] .line:has-text('Last order before the restart') ~ .marker",
                new Page.LocatorOptions().setHasText("The application was restarted"))).isVisible();
    }

    /**
     * Without the console, the page finds out when it loads its content again, as it does when
     * the developer comes back to the window.
     */
    @Test
    void pageWaitsForTheApplicationAndTheDevUiLoadsAgainWhenItIsBack(Page page) {
        page.navigate(pageUrl("settings"));
        assertThat(heading(page, "Settings")).isVisible();
        page.evaluate("window.loadedOnce = true");

        restartApplication(() -> {
            page.evaluate("window.dispatchEvent(new Event('focus'))");
            assertThat(heading(page, "Waiting for the application")).isVisible();
        });

        assertThat(page.getByText("The application was restarted.")).isVisible();
        assertThat(page.evaluate("window.loadedOnce")).isNull();
        assertThat(heading(page, "Settings")).isVisible();
    }

    private static Locator consoleButton(Page page) {
        return page.locator("arconia-console").getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Logs").setExact(true));
    }

    private static Locator line(Page page, String text) {
        return page.locator("arconia-console").getByRole(AriaRole.LOG).locator(".line", new Locator.LocatorOptions().setHasText(text));
    }

    private static Locator heading(Page page, String title) {
        return page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName(title).setLevel(1));
    }

}
