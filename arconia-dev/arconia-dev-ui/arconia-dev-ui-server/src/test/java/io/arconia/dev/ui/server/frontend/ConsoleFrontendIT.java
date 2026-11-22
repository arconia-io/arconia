package io.arconia.dev.ui.server.frontend;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Frontend tests of {@code arconia-console.js}, the console docked at the bottom of every page,
 * which follows the logs of the application.
 */
class ConsoleFrontendIT extends BaseFrontendIT {

    private static final Logger logger = LoggerFactory.getLogger("com.example.shop.OrderService");

    @Test
    void openConsoleShowsWhatTheApplicationLogs(Page page) {
        page.navigate(pageUrl("overview"));
        consoleButton(page).click();
        assertThat(consoleButton(page)).hasAttribute("aria-expanded", "true");

        logger.info("Order 42 placed");

        Locator line = line(page, "Order 42 placed");
        assertThat(line).isVisible();
        // A logger named after a class is shown as the name of the class only.
        assertThat(line.locator(".logger")).hasText("OrderService");
        assertThat(line.locator(".level")).hasText("INFO");
    }

    /**
     * Lines arriving after the level was chosen are shown or hidden by it too.
     */
    @Test
    void levelHidesTheLessSevereLines(Page page) {
        page.navigate(pageUrl("overview"));
        consoleButton(page).click();
        logger.info("Stock checked");
        assertThat(line(page, "Stock checked")).isVisible();

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Level")).click();
        page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("WARN and above")).click();
        logger.info("Stock checked again");
        logger.warn("Stock running low");

        assertThat(line(page, "Stock running low")).isVisible();
        assertThat(line(page, "Stock checked")).hasCount(2);
        assertThat(line(page, "Stock checked").first()).isHidden();
        assertThat(line(page, "Stock checked").last()).isHidden();
    }

    @Test
    void webAddressInALineIsALink(Page page) {
        page.navigate(pageUrl("overview"));
        consoleButton(page).click();

        logger.info("Order 42 can be tracked at https://shop.example.com/orders/42.");

        Locator link = log(page).getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("https://shop.example.com/orders/42").setExact(true));
        assertThat(link).hasAttribute("href", "https://shop.example.com/orders/42");
        assertThat(link).hasAttribute("target", "_blank");
    }

    @Test
    void openConsoleStaysOpenAfterAReload(Page page) {
        page.navigate(pageUrl("overview"));
        consoleButton(page).click();
        assertThat(log(page)).isVisible();

        page.reload();

        assertThat(consoleButton(page)).hasAttribute("aria-expanded", "true");
        assertThat(log(page)).isVisible();

        consoleButton(page).click();
        page.reload();

        assertThat(consoleButton(page)).hasAttribute("aria-expanded", "false");
        assertThat(log(page)).hasCount(0);
    }

    private static Locator consoleButton(Page page) {
        return page.locator("arconia-console").getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Logs").setExact(true));
    }

    private static Locator log(Page page) {
        return page.locator("arconia-console").getByRole(AriaRole.LOG);
    }

    private static Locator line(Page page, String text) {
        return log(page).locator(".line", new Locator.LocatorOptions().setHasText(text));
    }

}
