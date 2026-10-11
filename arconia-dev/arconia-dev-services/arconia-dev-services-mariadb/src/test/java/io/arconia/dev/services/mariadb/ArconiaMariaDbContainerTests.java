package io.arconia.dev.services.mariadb;

import java.util.Map;


import io.arconia.dev.services.tests.BaseDevServiceContainerTests;

/**
 * Unit tests for {@link ArconiaMariaDbContainer}.
 */
class ArconiaMariaDbContainerTests extends BaseDevServiceContainerTests<MariaDbDevServicesProperties, ArconiaMariaDbContainer> {

    @Override
    protected MariaDbDevServicesProperties createProperties() {
        return new MariaDbDevServicesProperties();
    }

    @Override
    protected ArconiaMariaDbContainer createContainer(MariaDbDevServicesProperties properties) {
        return new ArconiaMariaDbContainer(properties);
    }

    @Override
    protected Map<Integer, Integer> configureFixedPorts(MariaDbDevServicesProperties properties) {
        properties.setPort(1234);
        return Map.of(1234, ArconiaMariaDbContainer.MARIADB_PORT);
    }

}
