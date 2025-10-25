package io.arconia.opentelemetry.micrometer.registry.otlp.autoconfigure;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MicrometerRegistryOtlpProperties}.
 */
class MicrometerRegistryOtlpPropertiesTests {

    @Test
    void shouldHaveCorrectConfigPrefix() {
        assertThat(MicrometerRegistryOtlpProperties.CONFIG_PREFIX)
                .isEqualTo("arconia.otel.metrics.micrometer-otlp");
    }

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        MicrometerRegistryOtlpProperties properties = new MicrometerRegistryOtlpProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getBaseTimeUnit()).isEqualTo(TimeUnit.SECONDS);
        assertThat(properties.getMaxScale()).isEqualTo(20);
        assertThat(properties.getMaxBucketCount()).isEqualTo(160);
    }

    @Test
    void shouldUpdateValue() {
        MicrometerRegistryOtlpProperties properties = new MicrometerRegistryOtlpProperties();

        properties.setEnabled(false);
        properties.setBaseTimeUnit(TimeUnit.MILLISECONDS);
        properties.setMaxScale(10);
        properties.setMaxBucketCount(100);

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getBaseTimeUnit()).isEqualTo(TimeUnit.MILLISECONDS);
        assertThat(properties.getMaxScale()).isEqualTo(10);
        assertThat(properties.getMaxBucketCount()).isEqualTo(100);
    }

}
