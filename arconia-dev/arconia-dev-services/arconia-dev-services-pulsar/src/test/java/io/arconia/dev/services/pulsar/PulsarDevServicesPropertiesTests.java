package io.arconia.dev.services.pulsar;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.testcontainers.pulsar.PulsarContainer;

import io.arconia.dev.services.tests.BaseDevServicesPropertiesTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PulsarDevServicesProperties}.
 */
class PulsarDevServicesPropertiesTests extends BaseDevServicesPropertiesTests<PulsarDevServicesProperties> {

    @Override
    protected PulsarDevServicesProperties createProperties() {
        return new PulsarDevServicesProperties();
    }

    @Override
    protected DefaultValues getExpectedDefaults() {
        return DefaultValues.builder()
                .imageName(ArconiaPulsarContainer.COMPATIBLE_IMAGE_NAME)
                .shared(true)
                .startupTimeout(Duration.ofMinutes(2))
                .build();
    }

    @Test
    void shouldCreateInstanceWithServiceSpecificDefaultValues() {
        PulsarDevServicesProperties properties = createProperties();
        assertThat(properties.getAdminPort()).isEqualTo(0);
    }

    @Test
    void shouldUpdateServiceSpecificValues() {
        PulsarDevServicesProperties properties = createProperties();
        properties.setAdminPort(PulsarContainer.BROKER_HTTP_PORT);
        assertThat(properties.getAdminPort()).isEqualTo(ArconiaPulsarContainer.BROKER_HTTP_PORT);
    }

}
