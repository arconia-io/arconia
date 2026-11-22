package io.arconia.dev.ui.server.frontend;

import java.time.Duration;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.arconia.dev.ui.server.frontend.FrontendTestApplication.ServerLog;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Frontend tests of {@code arconia-cards-page.js}, the element that shows the cards of a page,
 * here the card of the catalog service of {@link FrontendTestApplication}.
 */
class CardsPageFrontendIT extends BaseFrontendIT {

    @Nested
    class PropertiesLink {

        @Test
        void propertiesLinkShowsThePropertiesUnderThePrefixOfTheCard(Page page) {
            page.navigate(pageUrl("services"));

            page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Properties of Catalog")).click();

            assertThat(page).hasURL(pageUrl("properties") + "?filter=server.");
            assertThat(heading(page, "Properties")).isVisible();
            assertThat(filter(page)).hasValue("server.");
        }

        @Test
        void backAndForwardKeepTheFilterOfTheProperties(Page page) {
            page.navigate(pageUrl("services"));
            page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Properties of Catalog")).click();
            assertThat(filter(page)).hasValue("server.");

            page.goBack();

            assertThat(heading(page, "Services")).isVisible();

            page.goForward();

            assertThat(heading(page, "Properties")).isVisible();
            assertThat(filter(page)).hasValue("server.");
        }

        /**
         * A link to the page already shown, under another prefix, shows the page anew with the
         * new filter, as the card of a Dev Service would on the page of the properties.
         */
        @Test
        void linkToThePageAlreadyShownAppliesItsFilter(Page page) {
            page.navigate(pageUrl("properties") + "?filter=server.");
            assertThat(filter(page)).hasValue("server.");

            page.evaluate("""
                    url => {
                        const link = Object.assign(document.createElement('a'), { href: url, textContent: 'Spring properties' });
                        document.querySelector('main').append(link);
                    }""", pageUrl("properties") + "?filter=spring.");
            page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Spring properties")).click();

            assertThat(page).hasURL(pageUrl("properties") + "?filter=spring.");
            assertThat(filter(page)).hasValue("spring.");
        }

    }

    @Nested
    class Status {

        /**
         * The color of a status says whether it is a problem, which assistive technologies
         * are told in words.
         */
        @Test
        void levelOfAStatusIsSaidAsWellAsShown(Page page) {
            page.navigate(pageUrl("services"));

            Locator status = page.locator("vaadin-card vaadin-badge[slot='header-suffix']");
            assertThat(status).hasAttribute("theme", "success");
            assertThat(status).hasText("Running, OK");
            assertThat(status.locator(".visually-hidden")).hasText(", OK");
        }

    }

    @Nested
    class LogDialog {

        @Test
        void logOfACardOpensInADialogNamedAfterIt(Page page) {
            page.navigate(pageUrl("services"));

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Server log").setExact(true)).click();

            assertThat(logDialog(page, "Catalog: Server log")).hasAttribute("opened", "");
            assertThat(logDialog(page, "Catalog: Server log").getByText("Server started")).isVisible();
        }

        /**
         * A log goes on for as long as somebody watches it, so it must no longer be followed
         * once its dialog is closed. The server finds out when it next writes to the browser, so
         * the log is made to write until it does.
         */
        @Test
        void closedDialogNoLongerFollowsTheLog(Page page) {
            page.navigate(pageUrl("services"));
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Server log").setExact(true)).click();
            Locator dialog = logDialog(page, "Catalog: Server log");
            assertThat(dialog.getByText("Request handled")).isVisible();
            ServerLog serverLog = bean(ServerLog.class);
            assertThat(serverLog.viewers()).isEqualTo(1);

            dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Close")).click();

            // A closed dialog has no title.
            assertThat(page.locator("vaadin-dialog")).not().hasAttribute("opened", "");
            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                serverLog.write("Request handled");
                assertThat(serverLog.viewers()).isZero();
            });
        }

        /**
         * The dialog of a log, by its title. Its content is shown in an overlay, so the dialog
         * itself never counts as visible.
         */
        private static Locator logDialog(Page page, String title) {
            return page.locator("vaadin-dialog[aria-label='" + title + "']");
        }

    }

    private static Locator heading(Page page, String title) {
        return page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName(title).setLevel(1));
    }

    private static Locator filter(Page page) {
        return page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Filter the rows of the table"));
    }

}
