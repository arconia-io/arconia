package io.arconia.opentelemetry.autoconfigure.instrumentation.micrometer;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MicrometerProperties}.
 */
class MicrometerPropertiesTests {

    @Test
    void shouldHaveCorrectConfigPrefix() {
        assertThat(MicrometerProperties.CONFIG_PREFIX)
            .isEqualTo("arconia.otel.instrumentation.micrometer");
    }

    @Test
    void shouldHaveCorrectInstrumentationName() {
        assertThat(MicrometerProperties.INSTRUMENTATION_NAME)
            .isEqualTo("micrometer");
    }

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        MicrometerProperties properties = new MicrometerProperties();

        assertThat(properties.getBaseTimeUnit()).isEqualTo(TimeUnit.SECONDS);
        assertThat(properties.isHistogramGauges()).isTrue();
    }

    @Test
    void shouldUpdateBaseTimeUnit() {
        MicrometerProperties properties = new MicrometerProperties();

        properties.setBaseTimeUnit(TimeUnit.MILLISECONDS);

        assertThat(properties.getBaseTimeUnit()).isEqualTo(TimeUnit.MILLISECONDS);
    }

    @Test
    void shouldUpdateHistogramGauges() {
        MicrometerProperties properties = new MicrometerProperties();

        properties.setHistogramGauges(false);

        assertThat(properties.isHistogramGauges()).isFalse();
    }

}
