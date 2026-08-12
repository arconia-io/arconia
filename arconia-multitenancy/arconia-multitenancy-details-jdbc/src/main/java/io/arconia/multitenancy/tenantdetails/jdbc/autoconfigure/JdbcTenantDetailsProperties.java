package io.arconia.multitenancy.tenantdetails.jdbc.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.init.DatabaseInitializationProperties;

/**
 * Configuration properties for JDBC-based tenant details.
 */
@ConfigurationProperties(prefix = JdbcTenantDetailsProperties.CONFIG_PREFIX)
public class JdbcTenantDetailsProperties extends DatabaseInitializationProperties {

    public static final String CONFIG_PREFIX = "arconia.multitenancy.details.jdbc";

    static final String DEFAULT_SCHEMA_LOCATION = "classpath:io/arconia/"
            + "multitenancy/tenantdetails/jdbc/autoconfigure/schema-@@platform@@.sql";

    /**
     * Whether tenant details are loaded from a relational database.
     */
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String getDefaultSchemaLocation() {
        return DEFAULT_SCHEMA_LOCATION;
    }

}
