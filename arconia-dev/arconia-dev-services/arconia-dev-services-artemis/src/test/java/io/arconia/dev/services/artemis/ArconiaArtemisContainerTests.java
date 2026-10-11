package io.arconia.dev.services.artemis;

import java.util.Map;

import io.arconia.dev.services.tests.BaseDevServiceContainerTests;

/**
 * Unit tests for {@link ArconiaArtemisContainer}.
 */
class ArconiaArtemisContainerTests extends BaseDevServiceContainerTests<ArtemisDevServicesProperties, ArconiaArtemisContainer> {

    @Override
    protected ArtemisDevServicesProperties createProperties() {
        return new ArtemisDevServicesProperties();
    }

    @Override
    protected ArconiaArtemisContainer createContainer(ArtemisDevServicesProperties properties) {
        return new ArconiaArtemisContainer(properties);
    }

    @Override
    protected Map<Integer, Integer> configureFixedPorts(ArtemisDevServicesProperties properties) {
        properties.setPort(1234);
        properties.setManagementConsolePort(5678);
        return Map.of(
                1234, ArconiaArtemisContainer.TCP_PORT,
                5678, ArconiaArtemisContainer.WEB_CONSOLE_PORT);
    }

}
