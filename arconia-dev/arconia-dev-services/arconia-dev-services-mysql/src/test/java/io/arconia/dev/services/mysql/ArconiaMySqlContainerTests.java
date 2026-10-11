package io.arconia.dev.services.mysql;

import java.util.Map;


import io.arconia.dev.services.tests.BaseDevServiceContainerTests;

/**
 * Unit tests for {@link ArconiaMySqlContainer}.
 */
class ArconiaMySqlContainerTests extends BaseDevServiceContainerTests<MySqlDevServicesProperties, ArconiaMySqlContainer> {

    @Override
    protected MySqlDevServicesProperties createProperties() {
        return new MySqlDevServicesProperties();
    }

    @Override
    protected ArconiaMySqlContainer createContainer(MySqlDevServicesProperties properties) {
        return new ArconiaMySqlContainer(properties);
    }

    @Override
    protected Map<Integer, Integer> configureFixedPorts(MySqlDevServicesProperties properties) {
        properties.setPort(1234);
        return Map.of(1234, ArconiaMySqlContainer.MYSQL_PORT);
    }

}
