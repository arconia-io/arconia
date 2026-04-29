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

        assertThat(options.isTraceContent()).isTrue();
        assertThat(options.isIncludeToolDefinitions()).isTrue();
    }

    @Test
    void shouldUpdateValues() {
        OpenLLMetryOptions options = new OpenLLMetryOptions();

        options.setTraceContent(false);
        options.setIncludeToolDefinitions(false);

        assertThat(options.isTraceContent()).isFalse();
        assertThat(options.isIncludeToolDefinitions()).isFalse();
    }

}
