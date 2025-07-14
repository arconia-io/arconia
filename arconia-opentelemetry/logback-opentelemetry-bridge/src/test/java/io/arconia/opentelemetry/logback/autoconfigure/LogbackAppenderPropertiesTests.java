package io.arconia.opentelemetry.logback.autoconfigure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LogbackAppenderProperties}.
 */
class LogbackAppenderPropertiesTests {

    @Test
    void shouldHaveCorrectConfigPrefix() {
        assertThat(LogbackAppenderProperties.CONFIG_PREFIX)
                .isEqualTo("arconia.otel.logs.logback-bridge");
    }

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        LogbackAppenderProperties properties = new LogbackAppenderProperties();
        assertThat(properties.isEnabled()).isTrue();
    }

    @Test
    void shouldUpdateEnabled() {
        LogbackAppenderProperties properties = new LogbackAppenderProperties();
        properties.setEnabled(false);
        assertThat(properties.isEnabled()).isFalse();
    }

}
