package io.arconia.multitenancy.tenantdetails.jdbc.autoconfigure;

import javax.sql.DataSource;

import org.springframework.boot.jdbc.init.PropertiesBasedDataSourceScriptDatabaseInitializer;

/**
 *
 */
class JdbcTenantDetailsDataSourceScriptDatabaseInitializer extends PropertiesBasedDataSourceScriptDatabaseInitializer<JdbcTenantDetailsProperties> {

    public JdbcTenantDetailsDataSourceScriptDatabaseInitializer(DataSource dataSource, JdbcTenantDetailsProperties properties) {
        super(dataSource, properties);
    }

}
