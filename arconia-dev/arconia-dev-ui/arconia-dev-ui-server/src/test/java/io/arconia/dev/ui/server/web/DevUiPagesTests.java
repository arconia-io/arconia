package io.arconia.dev.ui.server.web;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import io.arconia.dev.ui.api.page.CardsPage;
import io.arconia.dev.ui.api.page.DevUiPage;
import io.arconia.dev.ui.api.page.DevUiSection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevUiPages}.
 */
@ExtendWith(OutputCaptureExtension.class)
class DevUiPagesTests {

    private final DevUiIcons icons = new DevUiIcons(DevUiImportMap.load());

    @Test
    void pagesAreSortedByOrderAndThenByTitle() {
        DevUiPages pages = new DevUiPages(List.of(
                page("health", "Health", 0),
                page("configuration", "configuration", 0),
                page("beans", "Beans", 0),
                page("dev-services", "Dev Services", -10)), this.icons);

        assertThat(pages.all()).extracting(DevUiPage::getId)
                .containsExactly("dev-services", "beans", "configuration", "health");
    }

    @Test
    void pagesOfSectionStayTogetherWhereTheSectionIs() {
        DevUiPages pages = new DevUiPages(List.of(
                page("health", "Health", 0),
                CardsPage.builder().id("loggers").title("Loggers").section(DevUiSection.CONFIGURATION).cards(List::of).build(),
                CardsPage.builder().id("properties").title("Properties").section(DevUiSection.CONFIGURATION).order(-5).cards(List::of).build(),
                page("first", "First", -200),
                page("dev-services", "Dev Services", -10)), this.icons);

        assertThat(pages.all()).extracting(DevUiPage::getId)
                .containsExactly("first", "properties", "loggers", "dev-services", "health");
    }

    @Test
    void pageIsFoundByIdentifier() {
        DevUiPage devServices = page("dev-services", "Dev Services", 0);
        DevUiPages pages = new DevUiPages(List.of(devServices), this.icons);

        assertThat(pages.find("dev-services")).isSameAs(devServices);
        assertThat(pages.find("unknown")).isNull();
    }

    @Test
    void whenTwoPagesHaveTheSameIdentifierThenTheFirstInNavigationOrderIsKept(CapturedOutput output) {
        DevUiPage first = page("services", "Dev Services", 0);
        DevUiPage second = page("services", "Other Services", 0);

        DevUiPages pages = new DevUiPages(List.of(second, first), this.icons);

        assertThat(pages.all()).containsExactly(first);
        assertThat(output).contains(
                "The Dev UI page 'Other Services' is left out because the page 'Dev Services' has the same identifier 'services'");
    }

    @Test
    void pageIsShownWithItsIcon() {
        DevUiPage page = CardsPage.builder().id("dev-services").title("Dev Services").icon("container").cards(List::of).build();

        DevUiPages pages = new DevUiPages(List.of(page), this.icons);

        assertThat(pages.icon(page)).isEqualTo("container");
    }

    @Test
    void whenTheIconDoesNotExistThenThePageIsShownWithGenericIcon(CapturedOutput output) {
        DevUiPage page = CardsPage.builder().id("dev-services").title("Dev Services").icon("no-such-icon").cards(List::of).build();

        DevUiPages pages = new DevUiPages(List.of(page), this.icons);

        assertThat(pages.icon(page)).isEqualTo("file");
        assertThat(output).contains(
                "The Dev UI page 'Dev Services' is shown with a generic icon because there is no icon named 'no-such-icon'");
    }

    private static DevUiPage page(String id, String title, int order) {
        return CardsPage.builder().id(id).title(title).order(order).cards(List::of).build();
    }

}
