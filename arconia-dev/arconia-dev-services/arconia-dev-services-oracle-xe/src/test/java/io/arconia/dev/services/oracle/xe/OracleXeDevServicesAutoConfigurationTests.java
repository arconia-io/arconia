package io.arconia.dev.services.oracle.xe;

import org.testcontainers.containers.OracleContainer;

import io.arconia.dev.services.tests.BaseJdbcDevServicesAutoConfigurationTests;

/**
 * Tests for {@link OracleXeDevServicesAutoConfiguration}.
 * <p>
 * The discovery test of the base class is not run (no discoverable container is created),
 * since the Oracle image is too heavy to start twice. The discovered connection details are
 * covered without Docker by {@code OracleXeDiscoveredConnectionDetailsTests}.
 */
class OracleXeDevServicesAutoConfigurationTests extends BaseJdbcDevServicesAutoConfigurationTests {

    OracleXeDevServicesAutoConfigurationTests() {
        super(OracleXeDevServicesAutoConfiguration.class, "oracle-xe", OracleContainer.class);
    }

}
