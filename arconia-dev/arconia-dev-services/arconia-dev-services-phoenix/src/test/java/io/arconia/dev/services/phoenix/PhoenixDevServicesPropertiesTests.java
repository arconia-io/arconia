package io.arconia.dev.services.phoenix;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.tests.BaseDevServicesPropertiesTests;
import io.arconia.testcontainers.phoenix.PhoenixContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PhoenixDevServicesProperties}.
 */
class PhoenixDevServicesPropertiesTests extends BaseDevServicesPropertiesTests<PhoenixDevServicesProperties> {

    @Override
    protected PhoenixDevServicesProperties createProperties() {
        return new PhoenixDevServicesProperties();
    }

    @Override
    protected DefaultValues getExpectedDefaults() {
        return DefaultValues.builder()
                .imageName(ArconiaPhoenixContainer.COMPATIBLE_IMAGE_NAME)
                .shared(true)
                .build();
    }

    @Test
    void shouldCreateInstanceWithServiceSpecificDefaultValues() {
        PhoenixDevServicesProperties properties = createProperties();
        assertThat(properties.getOtlpGrpcPort()).isEqualTo(0);
    }

    @Test
    void shouldUpdateServiceSpecificValues() {
        PhoenixDevServicesProperties properties = createProperties();
        properties.setOtlpGrpcPort(PhoenixContainer.GRPC_PORT);
        assertThat(properties.getOtlpGrpcPort()).isEqualTo(PhoenixContainer.GRPC_PORT);
    }

}
