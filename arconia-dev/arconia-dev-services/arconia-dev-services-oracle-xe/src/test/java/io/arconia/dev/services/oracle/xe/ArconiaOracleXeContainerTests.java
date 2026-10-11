package io.arconia.dev.services.oracle.xe;

import java.util.Map;


import io.arconia.dev.services.tests.BaseDevServiceContainerTests;

/**
 * Unit tests for {@link ArconiaOracleXeContainer}.
 */
class ArconiaOracleXeContainerTests extends BaseDevServiceContainerTests<OracleXeDevServicesProperties, ArconiaOracleXeContainer> {

    @Override
    protected OracleXeDevServicesProperties createProperties() {
        return new OracleXeDevServicesProperties();
    }

    @Override
    protected ArconiaOracleXeContainer createContainer(OracleXeDevServicesProperties properties) {
        return new ArconiaOracleXeContainer(properties);
    }

    @Override
    protected Map<Integer, Integer> configureFixedPorts(OracleXeDevServicesProperties properties) {
        properties.setPort(1234);
        return Map.of(1234, ArconiaOracleXeContainer.ORACLE_PORT);
    }

}
