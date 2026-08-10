package io.arconia.multitenancy.data.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.init.DatabaseInitializationProperties;

import static io.arconia.multitenancy.data.autoconfigure.MultitenancyDataJdbcProperties.CONFIG_PREFIX;

@ConfigurationProperties(CONFIG_PREFIX)
class MultitenancyDataJdbcProperties
        extends DatabaseInitializationProperties {

    static final String CONFIG_PREFIX = "arconia.multitenancy.data.jdbc";

    private static final String DEFAULT_SCHEMA_LOCATION = "classpath:io/arconia/"
            + "multitenancy/data/autoconfigure/schema-@@platform@@.sql";

    @Override
    public String getDefaultSchemaLocation() {
        return DEFAULT_SCHEMA_LOCATION;
    }
}
