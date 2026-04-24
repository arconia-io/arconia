package io.arconia.opentelemetry.micrometer.registry.otlp.autoconfigure;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.micrometer.registry.otlp.AggregationTemporality;
import io.micrometer.registry.otlp.HistogramFlavor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link MicrometerOtlpConfig}.
 */
class MicrometerOtlpConfigTests {

    @Test
    void shouldHaveCorrectConfigPrefix() {
        MicrometerOtlpConfig config = MicrometerOtlpConfig.builder()
                .url("http://localhost:4318/v1/metrics")
                .build();

        assertThat(config.prefix()).isEqualTo(MicrometerRegistryOtlpProperties.CONFIG_PREFIX);
    }

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        String url = "http://localhost:4318/v1/metrics";
        MicrometerOtlpConfig config = MicrometerOtlpConfig.builder()
                .url(url)
                .build();

        assertThat(config.enabled()).isTrue();
        assertThat(config.url()).isEqualTo(url);
        assertThat(config.step()).isEqualTo(Duration.ofSeconds(60));
        assertThat(config.aggregationTemporality()).isEqualTo(AggregationTemporality.CUMULATIVE);
        assertThat(config.histogramFlavor()).isEqualTo(HistogramFlavor.EXPLICIT_BUCKET_HISTOGRAM);
        assertThat(config.headers()).isEmpty();
        assertThat(config.resourceAttributes()).isEmpty();
        assertThat(config.maxScale()).isEqualTo(20);
        assertThat(config.maxBucketCount()).isEqualTo(160);
        assertThat(config.baseTimeUnit()).isEqualTo(TimeUnit.MILLISECONDS);
    }

    @Test
    void shouldUpdateValues() {
        Map<String, String> headers = Map.of("Authorization", "Bearer token", "Content-Type", "application/json");
        Map<String, String> resourceAttributes = Map.of("service.name", "test-service", "service.version", "1.0.0");

        MicrometerOtlpConfig config = MicrometerOtlpConfig.builder()
                .url("http://example.com:4318/v1/metrics")
                .enabled(false)
                .step(Duration.ofSeconds(30))
                .aggregationTemporality(AggregationTemporality.DELTA)
                .histogramFlavor(HistogramFlavor.BASE2_EXPONENTIAL_BUCKET_HISTOGRAM)
                .addHeaders(headers)
                .addResourceAttributes(resourceAttributes)
                .maxScale(10)
                .maxBucketCount(100)
                .baseTimeUnit(TimeUnit.SECONDS)
                .build();

        assertThat(config.enabled()).isFalse();
        assertThat(config.url()).isEqualTo("http://example.com:4318/v1/metrics");
        assertThat(config.step()).isEqualTo(Duration.ofSeconds(30));
        assertThat(config.aggregationTemporality()).isEqualTo(AggregationTemporality.DELTA);
        assertThat(config.histogramFlavor()).isEqualTo(HistogramFlavor.BASE2_EXPONENTIAL_BUCKET_HISTOGRAM);
        assertThat(config.headers()).containsAllEntriesOf(headers);
        assertThat(config.resourceAttributes()).containsAllEntriesOf(resourceAttributes);
        assertThat(config.maxScale()).isEqualTo(10);
        assertThat(config.maxBucketCount()).isEqualTo(100);
        assertThat(config.baseTimeUnit()).isEqualTo(TimeUnit.SECONDS);
    }

    @Test
    void shouldReturnNullForGet() {
        MicrometerOtlpConfig config = MicrometerOtlpConfig.builder()
                .url("http://localhost:4318/v1/metrics")
                .build();

        assertThat(config.get("any-key")).isNull();
    }

    @Test
    void shouldValidateSuccessfully() {
        MicrometerOtlpConfig config = MicrometerOtlpConfig.builder()
                .url("http://localhost:4318/v1/metrics")
                .build();

        assertThat(config.validate().isValid()).isTrue();
    }

    @Test
    void shouldThrowExceptionWhenUrlIsNull() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> MicrometerOtlpConfig.builder().url(null).build())
                .withMessage("url cannot be null or empty");
    }

    @Test
    void shouldThrowExceptionWhenUrlIsEmpty() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> MicrometerOtlpConfig.builder().url("").build())
                .withMessage("url cannot be null or empty");
    }

    @Test
    void shouldThrowExceptionWhenStepIsNull() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> MicrometerOtlpConfig.builder().step(null))
                .withMessage("step cannot be null");
    }

    @Test
    void shouldThrowExceptionWhenAggregationTemporalityIsNull() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> MicrometerOtlpConfig.builder().aggregationTemporality(null))
                .withMessage("aggregationTemporality cannot be null");
    }

    @Test
    void shouldThrowExceptionWhenHistogramFlavorIsNull() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> MicrometerOtlpConfig.builder().histogramFlavor(null))
                .withMessage("histogramFlavor cannot be null");
    }

    @Test
    void shouldThrowExceptionWhenHeadersIsNull() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> MicrometerOtlpConfig.builder().addHeaders(null))
                .withMessage("headers cannot be null");
    }

    @Test
    void shouldThrowExceptionWhenHeadersHasNullKey() {
        var headers = new HashMap<String, String>();
        headers.put(null, "value");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> MicrometerOtlpConfig.builder().addHeaders(headers))
                .withMessage("headers cannot contain null keys");
    }

    @Test
    void shouldThrowExceptionWhenResourceAttributesIsNull() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> MicrometerOtlpConfig.builder().addResourceAttributes(null))
                .withMessage("resourceAttributes cannot be null");
    }

    @Test
    void shouldThrowExceptionWhenResourceAttributesHasNullKey() {
        var resourceAttributes = new HashMap<String, String>();
        resourceAttributes.put(null, "value");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> MicrometerOtlpConfig.builder().addResourceAttributes(resourceAttributes))
                .withMessage("resourceAttributes cannot contain null keys");
    }

    @Test
    void shouldThrowExceptionWhenBaseTimeUnitIsNull() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> MicrometerOtlpConfig.builder().baseTimeUnit(null))
                .withMessage("baseTimeUnit cannot be null");
    }

    @Test
    void shouldReturnDefaultHistogramFlavorPerMeter() {
        MicrometerOtlpConfig config = MicrometerOtlpConfig.builder()
                .url("http://localhost:4318/v1/metrics")
                .build();

        assertThat(config.histogramFlavorPerMeter()).isEmpty();
    }

    @Test
    void shouldReturnDefaultMaxBucketsPerMeter() {
        MicrometerOtlpConfig config = MicrometerOtlpConfig.builder()
                .url("http://localhost:4318/v1/metrics")
                .build();

        assertThat(config.maxBucketsPerMeter()).isEmpty();
    }

}
