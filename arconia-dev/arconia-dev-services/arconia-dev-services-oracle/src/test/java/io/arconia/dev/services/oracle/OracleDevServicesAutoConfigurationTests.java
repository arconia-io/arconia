package io.arconia.dev.services.oracle;

import org.testcontainers.oracle.OracleContainer;

import io.arconia.dev.services.tests.BaseJdbcDevServicesAutoConfigurationTests;

/**
 * Tests for {@link OracleDevServicesAutoConfiguration}.
 */
class OracleDevServicesAutoConfigurationTests extends BaseJdbcDevServicesAutoConfigurationTests {

    OracleDevServicesAutoConfigurationTests() {
        super(OracleDevServicesAutoConfiguration.class, "oracle", OracleContainer.class);
    }

}
