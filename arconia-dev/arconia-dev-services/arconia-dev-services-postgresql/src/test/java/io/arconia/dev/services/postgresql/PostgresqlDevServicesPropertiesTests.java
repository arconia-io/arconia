package io.arconia.dev.services.postgresql;

import io.arconia.dev.services.tests.BaseJdbcDevServicesPropertiesTests;

/**
 * Unit tests for {@link PostgresqlDevServicesProperties}.
 */
class PostgresqlDevServicesPropertiesTests extends BaseJdbcDevServicesPropertiesTests<PostgresqlDevServicesProperties> {

    @Override
    protected PostgresqlDevServicesProperties createProperties() {
        return new PostgresqlDevServicesProperties();
    }

    @Override
    protected DefaultValues getExpectedDefaults() {
        return DefaultValues.builder()
                .imageName("")
                .build();
    }

}
