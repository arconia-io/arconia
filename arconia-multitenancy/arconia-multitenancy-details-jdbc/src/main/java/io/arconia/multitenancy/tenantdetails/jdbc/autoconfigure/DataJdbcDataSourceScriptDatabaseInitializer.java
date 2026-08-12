package io.arconia.multitenancy.tenantdetails.jdbc.autoconfigure;

import org.springframework.boot.jdbc.init.PropertiesBasedDataSourceScriptDatabaseInitializer;

import javax.sql.DataSource;

class DataJdbcDataSourceScriptDatabaseInitializer
        extends PropertiesBasedDataSourceScriptDatabaseInitializer<MultitenancyDataJdbcProperties> {

    public DataJdbcDataSourceScriptDatabaseInitializer(DataSource dataSource,
                                                       MultitenancyDataJdbcProperties properties) {
        super(dataSource, properties);
    }

}
