package io.arconia.dev.services.oracle;

import org.testcontainers.oracle.OracleContainer;

import io.arconia.dev.services.tests.BaseJdbcDevServicesAutoConfigurationTests;

/**
 * Tests for {@link OracleDevServicesAutoConfiguration}.
 * <p>
 * The discovery test of the base class is not run (no discoverable container is created),
 * since the Oracle image is too heavy to start twice. The discovered connection details are
 * covered without Docker by {@code OracleDiscoveredConnectionDetailsTests}.
 */
class OracleDevServicesAutoConfigurationTests extends BaseJdbcDevServicesAutoConfigurationTests {

    OracleDevServicesAutoConfigurationTests() {
        super(OracleDevServicesAutoConfiguration.class, "oracle", OracleContainer.class);
    }

}
