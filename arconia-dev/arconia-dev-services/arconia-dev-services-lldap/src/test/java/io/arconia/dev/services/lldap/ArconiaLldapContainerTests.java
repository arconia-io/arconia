package io.arconia.dev.services.lldap;

import java.util.Map;


import io.arconia.dev.services.tests.BaseDevServiceContainerTests;

/**
 * Unit tests for {@link ArconiaLldapContainer}.
 */
class ArconiaLldapContainerTests extends BaseDevServiceContainerTests<LldapDevServicesProperties, ArconiaLldapContainer> {

    @Override
    protected LldapDevServicesProperties createProperties() {
        return new LldapDevServicesProperties();
    }

    @Override
    protected ArconiaLldapContainer createContainer(LldapDevServicesProperties properties) {
        return new ArconiaLldapContainer(properties);
    }

    @Override
    protected Map<Integer, Integer> configureFixedPorts(LldapDevServicesProperties properties) {
        properties.setPort(1234);
        properties.setManagementConsolePort(5678);
        return Map.of(
                1234, ArconiaLldapContainer.LDAP_PORT,
                5678, ArconiaLldapContainer.UI_PORT);
    }

}
