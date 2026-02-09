package io.arconia.dev.services.lldap;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.tests.BaseDevServicesPropertiesTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LldapDevServicesProperties}.
 */
class LldapDevServicesPropertiesTests extends BaseDevServicesPropertiesTests<LldapDevServicesProperties> {

    @Override
    protected LldapDevServicesProperties createProperties() {
        return new LldapDevServicesProperties();
    }

    @Override
    protected DefaultValues getExpectedDefaults() {
        return DefaultValues.builder()
                .imageName(ArconiaLldapContainer.COMPATIBLE_IMAGE_NAME)
                .build();
    }

    @Test
    void shouldCreateInstanceWithServiceSpecificDefaultValues() {
        LldapDevServicesProperties properties = createProperties();
        assertThat(properties.getManagementConsolePort()).isEqualTo(0);
    }

    @Test
    void shouldUpdateServiceSpecificValues() {
        LldapDevServicesProperties properties = new LldapDevServicesProperties();
        properties.setManagementConsolePort(ArconiaLldapContainer.UI_PORT);
        assertThat(properties.getManagementConsolePort()).isEqualTo(ArconiaLldapContainer.UI_PORT);
    }

}
