package io.arconia.multitenancy.tenantdetails.jdbc.autoconfigure;

import javax.sql.DataSource;

import org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializer;
import org.springframework.boot.jdbc.init.PropertiesBasedDataSourceScriptDatabaseInitializer;

/**
 * {@link DataSourceScriptDatabaseInitializer} for the tenant details database.
 */
class JdbcTenantDetailsDataSourceScriptDatabaseInitializer
        extends PropertiesBasedDataSourceScriptDatabaseInitializer<JdbcTenantDetailsProperties> {

    /**
     * Creates a new instance for the given data source and configuration properties.
     */
    public JdbcTenantDetailsDataSourceScriptDatabaseInitializer(DataSource dataSource,
            JdbcTenantDetailsProperties properties) {
        super(dataSource, properties);
    }

}
