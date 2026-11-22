package io.arconia.dev.ui.api.page;

import java.util.Arrays;
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
 * Unit tests for {@link TablePage}.
 */
class TablePageTests {

    @Test
    void pageIsBuiltWithAllComponents() {
        List<TableRow> rows = List.of(TableRow.builder().id("server.port").cells("server.port", "8080").build());

        TablePage page = validPage().icon("variable").order(10).section(DevUiSection.CONFIGURATION).filterColumn("Value").rows(() -> rows).build();

        assertThat(page.getId()).isEqualTo("environment");
        assertThat(page.getTitle()).isEqualTo("Environment");
        assertThat(page.getIcon()).isEqualTo("variable");
        assertThat(page.getOrder()).isEqualTo(10);
        assertThat(page.getColumns()).containsExactly("Property", "Value");
        assertThat(page.getSection()).isSameAs(DevUiSection.CONFIGURATION);
        assertThat(page.getFilterColumn()).isEqualTo("Value");
        assertThat(page.getRows().get()).isSameAs(rows);
    }

    @Test
    void pageHasGenericIconAndOrderZeroByDefault() {
        TablePage page = validPage().build();

        assertThat(page.getIcon()).isEqualTo("table");
        assertThat(page.getOrder()).isZero();
        assertThat(page.getSection()).isNull();
        assertThat(page.getFilterColumn()).isNull();
    }

    @Test
    void rowsAreNotSuppliedWhenThePageIsBuilt() {
        AtomicInteger calls = new AtomicInteger();

        validPage().rows(() -> {
            calls.incrementAndGet();
            return List.of();
        }).build();

        assertThat(calls).hasValue(0);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidPages")
    void whenAComponentIsInvalidThenThrow(String component, ThrowingCallable creation) {
        assertThatThrownBy(creation)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(component + " cannot");
    }

    @ParameterizedTest
    @ValueSource(strings = { "Environment", "my environment", "environment_", "-environment" })
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

    @Test
    void columnsCanBeListedOneByOne() {
        assertThat(validPage().columns("Table", "Rows").build().getColumns()).containsExactly("Table", "Rows");
    }

    @Test
    void whenTwoColumnsHaveTheSameTitleThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> validPage().columns(List.of("Name", "Name")).build())
                .withMessage("columns cannot contain the same title twice");
    }

    @Test
    void whenTheColumnToFilterByIsNotOneOfTheColumnsThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> validPage().filterColumn("Source").build())
                .withMessage("filterColumn 'Source' is not one of the columns");
    }

    static Stream<Arguments> invalidPages() {
        return Stream.of(
                Arguments.of("id", (ThrowingCallable) () -> validPage().id(null).build()),
                Arguments.of("title", (ThrowingCallable) () -> validPage().title("").build()),
                Arguments.of("icon", (ThrowingCallable) () -> validPage().icon("").build()),
                Arguments.of("columns", (ThrowingCallable) () -> validPage().columns(List.of()).build()),
                Arguments.of("columns", (ThrowingCallable) () -> validPage().columns(Arrays.asList("Property", " ")).build()),
                Arguments.of("rows", (ThrowingCallable) () -> validPage().rows(null).build()));
    }

    private static TablePage.Builder validPage() {
        return TablePage.builder().id("environment").title("Environment").columns(List.of("Property", "Value")).rows(List::of);
    }

}
