package io.arconia.dev.ui.api.page;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link LogItem}.
 */
class LogItemTests {

    @Test
    void sourceIsNotFollowedWhenTheItemIsCreated() throws Exception {
        List<String> lines = new ArrayList<>();
        LogSource source = consumer -> consumer.accept("started");

        LogItem item = LogItem.of("Logs", source);

        assertThat(item.getLabel()).isEqualTo("Logs");
        assertThat(lines).isEmpty();
        item.getSource().follow(lines::add);
        assertThat(lines).containsExactly("started");
    }

    @Test
    void whenLabelIsEmptyThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> LogItem.of("", consumer -> {}))
                .withMessage("label cannot be null or empty");
    }

    @Test
    void whenSourceIsNullThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> LogItem.of("Logs", null))
                .withMessage("source of the log 'Logs' cannot be null");
    }

}
