package io.arconia.dev.services.oracle;

import java.time.Duration;

import io.arconia.dev.services.tests.BaseJdbcDevServicesPropertiesTests;

/**
 * Unit tests for {@link OracleXeDevServicesProperties}.
 */
class OracleXeDevServicesPropertiesTests extends BaseJdbcDevServicesPropertiesTests<OracleXeDevServicesProperties> {

    @Override
    protected OracleXeDevServicesProperties createProperties() {
        return new OracleXeDevServicesProperties();
    }

    @Override
    protected DefaultValues getExpectedDefaults() {
        return DefaultValues.builder()
                .imageName(ArconiaOracleXeContainer.COMPATIBLE_IMAGE_NAME)
                .startupTimeout(Duration.ofMinutes(2))
                .build();
    }

}
