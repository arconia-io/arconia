package io.arconia.dev.services.oracle;

import io.arconia.dev.services.tests.BaseJdbcDevServicesPropertiesTests;

import java.time.Duration;

/**
 * Unit tests for {@link OracleDevServicesProperties}.
 */
class OracleDevServicesPropertiesTests extends BaseJdbcDevServicesPropertiesTests<OracleDevServicesProperties> {

    @Override
    protected OracleDevServicesProperties createProperties() {
        return new OracleDevServicesProperties();
    }

    @Override
    protected DefaultValues getExpectedDefaults() {
        return DefaultValues.builder()
                .imageName(ArconiaOracleContainer.COMPATIBLE_IMAGE_NAME)
                .startupTimeout(Duration.ofMinutes(2))
                .build();
    }

}
