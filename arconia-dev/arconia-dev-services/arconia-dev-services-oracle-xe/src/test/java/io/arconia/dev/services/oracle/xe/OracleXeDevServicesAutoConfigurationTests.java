package io.arconia.dev.services.oracle.xe;

import org.testcontainers.containers.OracleContainer;

import io.arconia.dev.services.tests.BaseJdbcDevServicesAutoConfigurationTests;

/**
 * Tests for {@link OracleXeDevServicesAutoConfiguration}.
 */
class OracleXeDevServicesAutoConfigurationTests extends BaseJdbcDevServicesAutoConfigurationTests {

    OracleXeDevServicesAutoConfigurationTests() {
        super(OracleXeDevServicesAutoConfiguration.class, "oracle-xe", OracleContainer.class);
    }

}
