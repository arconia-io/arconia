package io.arconia.dev.ui.api.page;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
 * Unit tests for {@link Card}.
 */
class CardTests {

    @Test
    void cardCanLinkToItsDocumentation() {
        assertThat(Card.builder().id("java").title("Java").documentation("https://docs.oracle.com/en/java/").build().getDocumentation())
                .isEqualTo("https://docs.oracle.com/en/java/");
        assertThat(Card.builder().id("java").title("Java").build().getDocumentation()).isNull();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Card.builder().id("java").title("Java").documentation("javascript:alert(1)").build())
                .withMessage("documentation 'javascript:alert(1)' of the card 'java' must be an HTTP or HTTPS URL");
    }

    @Test
    void cardCanLinkToItsProperties() {
        assertThat(Card.builder().id("data").title("Data").propertiesPrefix("spring.datasource").build().getPropertiesPrefix())
                .isEqualTo("spring.datasource");
        assertThat(Card.builder().id("data").title("Data").build().getPropertiesPrefix()).isNull();
    }

    @Test
    void whatIsOptionalAndBlankIsLeftOut() {
        Card card = Card.builder().id("data").title("Data").description(" ").icon("").documentation("").propertiesPrefix(" ").build();

        assertThat(card.getDescription()).isNull();
        assertThat(card.getIcon()).isNull();
        assertThat(card.getDocumentation()).isNull();
        assertThat(card.getPropertiesPrefix()).isNull();
    }

    @Test
    void itemsCanBeGivenOneByOneOrAsListOfOneKind() {
        List<InfoItem> information = List.of(InfoItem.of("Image", "postgres"));

        assertThat(Card.builder().id("data").title("Data").items(information).build().getItems()).hasSize(1);
        assertThat(Card.builder().id("data").title("Data").items(InfoItem.of("Image", "postgres"), LinkItem.of("Console", "/console")).build()
                .getItems()).hasSize(2);
    }

    @Test
    void cardCanHaveAnIcon() {
        assertThat(Card.builder().id("java").title("Java").icon("coffee").build().getIcon()).isEqualTo("coffee");
        assertThat(Card.builder().id("java").title("Java").build().getIcon()).isNull();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Card.builder().id("java").title("Java").icon("My Icon").build())
                .withMessage("icon 'My Icon' can only contain lowercase letters, digits, and single hyphens between them");
    }

    @Test
    void cardIsBuiltWithAllComponents() {
        InfoItem image = InfoItem.of("Image", "postgres");
        LinkItem console = LinkItem.of("Console", "http://localhost:8080");

        Card card = Card.builder()
                .id("postgresql")
                .title("PostgreSQL")
                .description("Relational database")
                .status(new Status("Running", StatusLevel.SUCCESS))
                .items(List.of(image, console))
                .build();

        assertThat(card.getId()).isEqualTo("postgresql");
        assertThat(card.getTitle()).isEqualTo("PostgreSQL");
        assertThat(card.getDescription()).isEqualTo("Relational database");
        assertThat(card.getStatus()).isEqualTo(new Status("Running", StatusLevel.SUCCESS));
        assertThat(card.getItems()).containsExactly(image, console);
    }

    @Test
    void cardHasNoDescriptionNoStatusAndNoItemsByDefault() {
        Card card = Card.builder().id("postgresql").title("PostgreSQL").build();

        assertThat(card.getDescription()).isNull();
        assertThat(card.getStatus()).isNull();
        assertThat(card.getItems()).isEmpty();
    }

    @Test
    void itemsAreCopied() {
        List<CardItem> items = new ArrayList<>(List.of(InfoItem.of("Image", "postgres")));
        Card card = Card.builder().id("postgresql").title("PostgreSQL").items(items).build();

        items.clear();

        assertThat(card.getItems()).hasSize(1);
    }

    @Test
    void whenSeveralLogsHaveTheSameLabelThenThrow() {
        LogSource source = lines -> {};

        assertThatIllegalArgumentException()
                .isThrownBy(() -> Card.builder().id("postgresql").title("PostgreSQL")
                        .items(List.of(LogItem.of("Logs", source), InfoItem.of("Image", "postgres"), LogItem.of("Logs", source)))
                        .build())
                .withMessage("items of the card 'postgresql' cannot contain several logs with the label 'Logs'");
    }

    @Test
    void itemsOfDifferentKindsCanHaveTheSameLabel() {
        Card card = Card.builder().id("postgresql").title("PostgreSQL")
                .items(List.of(InfoItem.of("Logs", "Available"), LogItem.of("Logs", lines -> {}), LogItem.of("Audit logs", lines -> {})))
                .build();

        assertThat(card.getItems()).hasSize(3);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidCards")
    void whenAComponentIsInvalidThenThrow(String component, ThrowingCallable creation) {
        assertThatThrownBy(creation)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(component)
                .hasMessageContaining(" cannot");
    }

    @Test
    void messageOfAnInvalidCardSaysWhichCardItIs() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Card.builder().id("HikariCP").title(" ").build())
                .withMessage("title of the card 'HikariCP' cannot be null or empty");
    }

    @ParameterizedTest
    @ValueSource(strings = { "postgresql", "Orders_DB", "HikariCP", "jakarta.servlet-api", "com.example:shop", "PostgreSQL 18" })
    void idCanBeAnyTextThatTellsTheCardApart(String id) {
        assertThat(Card.builder().id(id).title("Card").build().getId()).isEqualTo(id);
    }

    static Stream<Arguments> invalidCards() {
        return Stream.of(
                Arguments.of("id", (ThrowingCallable) () -> Card.builder().title("Card").build()),
                Arguments.of("title", (ThrowingCallable) () -> Card.builder().id("postgresql").title("").build()),
                Arguments.of("items", (ThrowingCallable) () -> Card.builder().id("postgresql").title("Card").items((List<CardItem>) null).build()),
                Arguments.of("items", (ThrowingCallable) () -> Card.builder().id("postgresql").title("Card")
                        .items(Arrays.asList(InfoItem.of("Image", "postgres"), null)).build()));
    }

}
