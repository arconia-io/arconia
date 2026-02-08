package io.arconia.dev.services.pulsar;

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
                .build();
    }

    @Test
    void shouldCreateInstanceWithServiceSpecificDefaultValues() {
        PulsarDevServicesProperties properties = createProperties();
        assertThat(properties.getManagementConsolePort()).isEqualTo(0);
    }

    @Test
    void shouldUpdateServiceSpecificValues() {
        PulsarDevServicesProperties properties = createProperties();
        properties.setManagementConsolePort(PulsarContainer.BROKER_HTTP_PORT);
        assertThat(properties.getManagementConsolePort()).isEqualTo(ArconiaPulsarContainer.BROKER_HTTP_PORT);
    }

}
