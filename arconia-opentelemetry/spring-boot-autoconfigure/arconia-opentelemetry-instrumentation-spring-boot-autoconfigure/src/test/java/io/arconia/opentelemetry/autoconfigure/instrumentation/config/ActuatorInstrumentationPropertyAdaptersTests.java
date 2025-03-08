package io.arconia.opentelemetry.autoconfigure.instrumentation.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.TimeUnit;

/**
 * Unit tests for {@link ActuatorInstrumentationPropertyAdapters}.
 */
class ActuatorInstrumentationPropertyAdaptersTests {

    @Test
    void metricsShouldThrowExceptionWhenEnvironmentIsNull() {
        assertThatThrownBy(() -> ActuatorInstrumentationPropertyAdapters.metrics(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("environment cannot be null");
    }

    @Test
    void metricsShouldMapProperties() {
        var environment = new MockEnvironment()
            .withProperty("management.otlp.metrics.export.base-time-unit", "seconds");

        var adapter = ActuatorInstrumentationPropertyAdapters.metrics(environment);

        assertThat((TimeUnit)adapter.getArconiaProperties().get("arconia.otel.instrumentation.micrometer.base-time-unit"))
            .isEqualTo(TimeUnit.SECONDS);
    }

}
