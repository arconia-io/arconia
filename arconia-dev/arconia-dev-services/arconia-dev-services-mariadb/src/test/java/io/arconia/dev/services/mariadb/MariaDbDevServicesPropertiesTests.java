package io.arconia.dev.services.mariadb;

import io.arconia.dev.services.tests.BaseJdbcDevServicesPropertiesTests;

/**
 * Unit tests for {@link MariaDbDevServicesProperties}.
 */
class MariaDbDevServicesPropertiesTests extends BaseJdbcDevServicesPropertiesTests<MariaDbDevServicesProperties> {

    @Override
    protected MariaDbDevServicesProperties createProperties() {
        return new MariaDbDevServicesProperties();
    }

    @Override
    protected DefaultValues getExpectedDefaults() {
        return DefaultValues.builder()
                .imageName(ArconiaMariaDbContainer.COMPATIBLE_IMAGE_NAME)
                .build();
    }

}
