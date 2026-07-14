package io.arconia.dev.services.lgtm;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.tests.BaseDevServicesPropertiesTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LgtmDevServicesProperties}.
 */
class LgtmDevServicesPropertiesTests extends BaseDevServicesPropertiesTests<LgtmDevServicesProperties> {

    @Override
    protected LgtmDevServicesProperties createProperties() {
        return new LgtmDevServicesProperties();
    }

    @Override
    protected DefaultValues getExpectedDefaults() {
        return DefaultValues.builder()
                .imageName(ArconiaLgtmStackContainer.COMPATIBLE_IMAGE_NAME)
                .shared(true)
                .startupTimeout(Duration.ofMinutes(2))
                .build();
    }

    @Test
    void shouldCreateInstanceWithServiceSpecificDefaultValues() {
        LgtmDevServicesProperties properties = createProperties();

        assertThat(properties.getOtlpGrpcPort()).isEqualTo(0);
        assertThat(properties.getOtlpHttpPort()).isEqualTo(0);
        assertThat(properties.getLokiPort()).isEqualTo(0);
        assertThat(properties.getTempoPort()).isEqualTo(0);
        assertThat(properties.getPrometheusPort()).isEqualTo(0);
    }

    @Test
    void shouldUpdateServiceSpecificValues() {
        LgtmDevServicesProperties properties = createProperties();

        properties.setOtlpGrpcPort(9001);
        properties.setOtlpHttpPort(9002);
        properties.setLokiPort(9003);
        properties.setTempoPort(9004);
        properties.setPrometheusPort(9005);

        assertThat(properties.getOtlpGrpcPort()).isEqualTo(9001);
        assertThat(properties.getOtlpHttpPort()).isEqualTo(9002);
        assertThat(properties.getLokiPort()).isEqualTo(9003);
        assertThat(properties.getTempoPort()).isEqualTo(9004);
        assertThat(properties.getPrometheusPort()).isEqualTo(9005);
    }

}
