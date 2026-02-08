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
    }

    @Test
    void shouldUpdateServiceSpecificValues() {
        LgtmDevServicesProperties properties = createProperties();

        properties.setOtlpGrpcPort(ArconiaLgtmStackContainer.OTLP_GRPC_PORT);
        properties.setOtlpHttpPort(ArconiaLgtmStackContainer.OTLP_HTTP_PORT);

        assertThat(properties.getOtlpGrpcPort()).isEqualTo(ArconiaLgtmStackContainer.OTLP_GRPC_PORT);
        assertThat(properties.getOtlpHttpPort()).isEqualTo(ArconiaLgtmStackContainer.OTLP_HTTP_PORT);
    }

}
