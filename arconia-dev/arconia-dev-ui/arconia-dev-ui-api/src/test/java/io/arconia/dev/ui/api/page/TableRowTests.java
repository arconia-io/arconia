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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link TableRow}.
 */
class TableRowTests {

    @Test
    void rowIsBuiltWithAllComponents() {
        List<TableCell> cells = List.of(TableCell.text("spring.datasource.password"), TableCell.secret("secret"));
        List<InfoItem> details = List.of(InfoItem.of("Origin", "application.yml"));

        TableRow row = TableRow.builder().id("spring.datasource.password").cells(cells).details(details).build();

        assertThat(row.getId()).isEqualTo("spring.datasource.password");
        assertThat(row.getCells()).isEqualTo(cells);
        assertThat(row.getDetails()).isEqualTo(details);
    }

    @Test
    void rowHasNoDetailsByDefault() {
        TableRow row = TableRow.builder().id("server.port").cells("server.port", "8080").build();

        assertThat(row.getDetails()).isEmpty();
    }

    @Test
    void cellsCanBeGivenAsTexts() {
        TableRow row = TableRow.builder().id("server.port").cells("server.port", "").build();

        assertThat(row.getCells()).containsExactly(TableCell.text("server.port"), TableCell.text(""));
    }

    @Test
    void cellsAndDetailsAreCopied() {
        List<TableCell> cells = new ArrayList<>(List.of(TableCell.text("server.port")));
        List<InfoItem> details = new ArrayList<>(List.of(InfoItem.of("Origin", "application.yml")));
        TableRow row = TableRow.builder().id("server.port").cells(cells).details(details).build();

        cells.clear();
        details.clear();

        assertThat(row.getCells()).hasSize(1);
        assertThat(row.getDetails()).hasSize(1);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRows")
    void whenAComponentIsInvalidThenThrow(String component, ThrowingCallable creation) {
        assertThatThrownBy(creation)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(component)
                .hasMessageContaining(" cannot");
    }

    static Stream<Arguments> invalidRows() {
        return Stream.of(
                Arguments.of("id", (ThrowingCallable) () -> TableRow.builder().cells("8080").build()),
                Arguments.of("cells", (ThrowingCallable) () -> TableRow.builder().id("server.port").build()),
                Arguments.of("cells", (ThrowingCallable) () -> TableRow.builder().id("server.port").cells(List.of()).build()),
                Arguments.of("cells", (ThrowingCallable) () -> TableRow.builder().id("server.port")
                        .cells(Arrays.asList(TableCell.text("8080"), null)).build()),
                Arguments.of("texts", (ThrowingCallable) () -> TableRow.builder().id("server.port").cells("8080", null).build()),
                Arguments.of("details", (ThrowingCallable) () -> TableRow.builder().id("server.port").cells("8080").details(null).build()));
    }

}
