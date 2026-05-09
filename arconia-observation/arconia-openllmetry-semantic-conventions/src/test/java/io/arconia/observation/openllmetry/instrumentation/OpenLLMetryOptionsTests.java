package io.arconia.observation.openllmetry.instrumentation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenLLMetryOptions}.
 */
class OpenLLMetryOptionsTests {

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        OpenLLMetryOptions options = new OpenLLMetryOptions();

        assertThat(options.getInference().isIncludeContent()).isTrue();
        assertThat(options.getInference().isIncludeToolDefinitions()).isTrue();
        assertThat(options.getToolExecution().isIncludeContent()).isTrue();
    }

    @Test
    void shouldUpdateValues() {
        OpenLLMetryOptions options = new OpenLLMetryOptions();

        options.getInference().setIncludeContent(false);
        options.getInference().setIncludeToolDefinitions(false);
        options.getToolExecution().setIncludeContent(false);

        assertThat(options.getInference().isIncludeContent()).isFalse();
        assertThat(options.getInference().isIncludeToolDefinitions()).isFalse();
        assertThat(options.getToolExecution().isIncludeContent()).isFalse();
    }

}
