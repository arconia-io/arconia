package io.arconia.dev.services.mysql;

import io.arconia.dev.services.tests.BaseJdbcDevServicesPropertiesTests;

/**
 * Unit tests for {@link MySqlDevServicesProperties}.
 */
class MySqlDevServicesPropertiesTests extends BaseJdbcDevServicesPropertiesTests<MySqlDevServicesProperties> {

    @Override
    protected MySqlDevServicesProperties createProperties() {
        return new MySqlDevServicesProperties();
    }

    @Override
    protected DefaultValues getExpectedDefaults() {
        return DefaultValues.builder()
                .imageName(ArconiaMySqlContainer.COMPATIBLE_IMAGE_NAME)
                .build();
    }

}
