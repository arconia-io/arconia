package io.arconia.dev.ui.api.page;

import org.junit.jupiter.api.Test;

import io.arconia.dev.ui.api.page.StatusLevel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link Status}.
 */
class StatusTests {

    @Test
    void statusHasTextAndLevel() {
        Status status = new Status("Running", StatusLevel.SUCCESS);

        assertThat(status.text()).isEqualTo("Running");
        assertThat(status.level()).isEqualTo(StatusLevel.SUCCESS);
    }

    @Test
    void whenTextIsEmptyThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Status(" ", StatusLevel.SUCCESS))
                .withMessage("text cannot be null or empty");
    }

    @Test
    void whenLevelIsNullThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Status("Running", null))
                .withMessage("level cannot be null");
    }

}
