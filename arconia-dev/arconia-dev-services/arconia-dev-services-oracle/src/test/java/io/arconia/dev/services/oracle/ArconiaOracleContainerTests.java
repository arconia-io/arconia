package io.arconia.dev.services.oracle;

import java.util.Map;


import io.arconia.dev.services.tests.BaseDevServiceContainerTests;

/**
 * Unit tests for {@link ArconiaOracleContainer}.
 */
class ArconiaOracleContainerTests extends BaseDevServiceContainerTests<OracleDevServicesProperties, ArconiaOracleContainer> {

    @Override
    protected OracleDevServicesProperties createProperties() {
        return new OracleDevServicesProperties();
    }

    @Override
    protected ArconiaOracleContainer createContainer(OracleDevServicesProperties properties) {
        return new ArconiaOracleContainer(properties);
    }

    @Override
    protected Map<Integer, Integer> configureFixedPorts(OracleDevServicesProperties properties) {
        properties.setPort(1234);
        return Map.of(1234, ArconiaOracleContainer.ORACLE_PORT);
    }

}
