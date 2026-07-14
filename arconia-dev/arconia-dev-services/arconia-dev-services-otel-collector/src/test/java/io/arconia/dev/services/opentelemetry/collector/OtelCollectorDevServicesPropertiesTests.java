package io.arconia.dev.services.opentelemetry.collector;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.tests.BaseDevServicesPropertiesTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OtelCollectorDevServicesProperties}.
 */
class OtelCollectorDevServicesPropertiesTests extends BaseDevServicesPropertiesTests<OtelCollectorDevServicesProperties> {

    @Override
    protected OtelCollectorDevServicesProperties createProperties() {
        return new OtelCollectorDevServicesProperties();
    }

    @Override
    protected DefaultValues getExpectedDefaults() {
        return DefaultValues.builder()
                .imageName(ArconiaOtelCollectorContainer.COMPATIBLE_IMAGE_NAME)
                .shared(true)
                .build();
    }

    @Test
    void shouldCreateInstanceWithServiceSpecificDefaultValues() {
        OtelCollectorDevServicesProperties properties = createProperties();
        assertThat(properties.getOtlpGrpcPort()).isEqualTo(0);
    }

    @Test
    void shouldUpdateServiceSpecificValues() {
        OtelCollectorDevServicesProperties properties = createProperties();
        properties.setOtlpGrpcPort(ArconiaOtelCollectorContainer.OTLP_GRPC_PORT);
        assertThat(properties.getOtlpGrpcPort()).isEqualTo(ArconiaOtelCollectorContainer.OTLP_GRPC_PORT);
    }

}
