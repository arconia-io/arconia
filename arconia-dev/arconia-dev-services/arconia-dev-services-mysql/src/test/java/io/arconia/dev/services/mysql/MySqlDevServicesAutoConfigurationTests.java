package io.arconia.dev.services.mysql;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.mysql.MySQLContainer;

import io.arconia.dev.services.tests.BaseJdbcDevServicesAutoConfigurationTests;

/**
 * Tests for {@link MySqlDevServicesAutoConfiguration}.
 */
class MySqlDevServicesAutoConfigurationTests extends BaseJdbcDevServicesAutoConfigurationTests {

    MySqlDevServicesAutoConfigurationTests() {
        super(MySqlDevServicesAutoConfiguration.class, "mysql", MySQLContainer.class);
    }

    @Override
    protected GenericContainer<?> createDiscoverableContainer(String ownerId) {
        var properties = new MySqlDevServicesProperties();
        return asDiscoverableContainer(MySqlDevServicesAutoConfiguration.createContainer(properties), properties, ownerId);
    }

}
