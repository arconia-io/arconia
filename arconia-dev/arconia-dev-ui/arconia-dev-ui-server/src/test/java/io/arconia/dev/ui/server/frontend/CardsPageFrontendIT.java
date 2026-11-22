package io.arconia.dev.ui.server.frontend;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

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

    private static Locator heading(Page page, String title) {
        return page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName(title).setLevel(1));
    }

    private static Locator filter(Page page) {
        return page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Filter the rows of the table"));
    }

}
