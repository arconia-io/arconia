package io.arconia.dev.ui.api.page;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link TableCell}.
 */
class TableCellTests {

    @Test
    void cellShowsText() {
        TableCell cell = TableCell.text("8080");

        assertThat(cell.getText()).isEqualTo("8080");
        assertThat(cell.isSecret()).isFalse();
        assertThat(cell).hasToString("TableCell[text=8080, secret=false]");
    }

    @Test
    void textCanBeEmpty() {
        assertThat(TableCell.text("").getText()).isEmpty();
    }

    @Test
    void secretCellKeepsItsTextOutOfItsDescription() {
        TableCell cell = TableCell.secret("s3cret");

        assertThat(cell.getText()).isEqualTo("s3cret");
        assertThat(cell.isSecret()).isTrue();
        assertThat(cell.toString()).doesNotContain("s3cret");
    }

    @Test
    void cellsAreEqualWhenTheyHoldTheSameTextTheSameWay() {
        assertThat(TableCell.text("8080")).isEqualTo(TableCell.text("8080")).hasSameHashCodeAs(TableCell.text("8080"));
        assertThat(TableCell.text("8080")).isNotEqualTo(TableCell.secret("8080")).isNotEqualTo(TableCell.text("9090"));
    }

    @Test
    void whenTextIsNullThenThrow() {
        assertThatIllegalArgumentException().isThrownBy(() -> TableCell.text(null)).withMessage("text cannot be null");
        assertThatIllegalArgumentException().isThrownBy(() -> TableCell.secret(null)).withMessage("text cannot be null");
    }

}
