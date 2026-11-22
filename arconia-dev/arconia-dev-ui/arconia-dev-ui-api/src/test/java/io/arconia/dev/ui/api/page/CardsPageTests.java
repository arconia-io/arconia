package io.arconia.dev.ui.api.page;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link CardsPage}.
 */
class CardsPageTests {

    @Test
    void pageIsBuiltWithAllComponents() {
        List<Card> cards = List.of(Card.builder().id("postgresql").title("PostgreSQL").build());

        CardsPage page = validPage().icon("container").order(10).maxColumns(2).cards(() -> cards).build();

        assertThat(page.getId()).isEqualTo("dev-services");
        assertThat(page.getTitle()).isEqualTo("Dev Services");
        assertThat(page.getIcon()).isEqualTo("container");
        assertThat(page.getOrder()).isEqualTo(10);
        assertThat(page.getMaxColumns()).isEqualTo(2);
        assertThat(page.getCards().get()).isSameAs(cards);
    }

    @Test
    void pageHasGenericIconOrderZeroAndThreeColumnsByDefault() {
        CardsPage page = validPage().build();

        assertThat(page.getIcon()).isEqualTo("layout-grid");
        assertThat(page.getOrder()).isZero();
        assertThat(page.getSection()).isNull();
        assertThat(page.getMaxColumns()).isEqualTo(3);
    }

    @Test
    void cardsAreNotSuppliedWhenThePageIsBuilt() {
        AtomicInteger calls = new AtomicInteger();

        validPage().cards(() -> {
            calls.incrementAndGet();
            return List.of();
        }).build();

        assertThat(calls).hasValue(0);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidPages")
    void whenAComponentIsMissingThenThrow(String component, ThrowingCallable creation) {
        assertThatThrownBy(creation)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(component + " cannot be");
    }

    static Stream<Arguments> invalidPages() {
        return Stream.of(
                Arguments.of("id", (ThrowingCallable) () -> validPage().id("").build()),
                Arguments.of("title", (ThrowingCallable) () -> validPage().title("").build()),
                Arguments.of("icon", (ThrowingCallable) () -> validPage().icon("").build()),
                Arguments.of("maxColumns", (ThrowingCallable) () -> validPage().maxColumns(0).build()),
                Arguments.of("cards", (ThrowingCallable) () -> CardsPage.builder().id("page").title("Page").build()));
    }

    @ParameterizedTest
    @ValueSource(strings = { "Dev-Services", "dev services", "dev_services", "dev/services", "-dev", "dev-", "dev--services" })
    void whenIdCannotBeUsedInUrlThenThrow(String id) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> validPage().id(id).build())
                .withMessage("id '%s' can only contain lowercase letters, digits, and single hyphens between them", id);
    }

    @Test
    void whenIdIsTheOneOfTheOverviewThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> validPage().id("overview").build())
                .withMessage("id 'overview' is reserved for the overview of the Dev UI");
    }

    @ParameterizedTest
    @ValueSource(strings = { "Database", "data base", "database.svg", "../database", "-database" })
    void whenIconIsNotAnIconNameThenThrow(String icon) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> validPage().icon(icon).build())
                .withMessage("icon '%s' can only contain lowercase letters, digits, and single hyphens between them", icon);
    }

    private static CardsPage.Builder validPage() {
        return CardsPage.builder()
                .id("dev-services")
                .title("Dev Services")
                .cards(List::of);
    }

}
