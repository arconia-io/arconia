package io.arconia.dev.services.mariadb;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.mariadb.MariaDBContainer;

import io.arconia.dev.services.tests.BaseJdbcDevServicesAutoConfigurationTests;

/**
 * Tests for {@link MariaDbDevServicesAutoConfiguration}.
 */
class MariaDbDevServicesAutoConfigurationTests extends BaseJdbcDevServicesAutoConfigurationTests {

    MariaDbDevServicesAutoConfigurationTests() {
        super(MariaDbDevServicesAutoConfiguration.class, "mariadb", MariaDBContainer.class);
    }

    @Override
    protected GenericContainer<?> createDiscoverableContainer(String ownerId) {
        var properties = new MariaDbDevServicesProperties();
        return asDiscoverableContainer(MariaDbDevServicesAutoConfiguration.createContainer(properties), properties, ownerId);
    }

}
