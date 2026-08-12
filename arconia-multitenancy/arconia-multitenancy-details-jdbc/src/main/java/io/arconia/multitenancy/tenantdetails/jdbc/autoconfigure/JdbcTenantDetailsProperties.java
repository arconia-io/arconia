package io.arconia.multitenancy.tenantdetails.jdbc.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.init.DatabaseInitializationProperties;

import static io.arconia.multitenancy.tenantdetails.jdbc.autoconfigure.JdbcTenantDetailsProperties.CONFIG_PREFIX;

@ConfigurationProperties(CONFIG_PREFIX)
public class JdbcTenantDetailsProperties extends DatabaseInitializationProperties {

    static final String CONFIG_PREFIX = "arconia.multitenancy.details.jdbc";

    private static final String DEFAULT_SCHEMA_LOCATION = "classpath:io/arconia/"
            + "multitenancy/tenantdetails/jdbc/autoconfigure/schema-@@platform@@.sql";

    @Override
    public String getDefaultSchemaLocation() {
        return DEFAULT_SCHEMA_LOCATION;
    }

}
