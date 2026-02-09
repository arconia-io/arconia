package io.arconia.dev.services.rabbitmq;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.tests.BaseDevServicesPropertiesTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RabbitMqDevServicesProperties}.
 */
class RabbitMqDevServicesPropertiesTests extends BaseDevServicesPropertiesTests<RabbitMqDevServicesProperties> {

    @Override
    protected RabbitMqDevServicesProperties createProperties() {
        return new RabbitMqDevServicesProperties();
    }

    @Override
    protected DefaultValues getExpectedDefaults() {
        return DefaultValues.builder()
                .imageName(ArconiaRabbitMqContainer.COMPATIBLE_IMAGE_NAME)
                .shared(true)
                .build();
    }

    @Test
    void shouldCreateInstanceWithServiceSpecificDefaultValues() {
        RabbitMqDevServicesProperties properties = createProperties();
        assertThat(properties.getManagementConsolePort()).isEqualTo(0);
    }

    @Test
    void shouldUpdateServiceSpecificValues() {
        RabbitMqDevServicesProperties properties = createProperties();
        properties.setManagementConsolePort(ArconiaRabbitMqContainer.HTTP_PORT);
        assertThat(properties.getManagementConsolePort()).isEqualTo(ArconiaRabbitMqContainer.HTTP_PORT);
    }

}
