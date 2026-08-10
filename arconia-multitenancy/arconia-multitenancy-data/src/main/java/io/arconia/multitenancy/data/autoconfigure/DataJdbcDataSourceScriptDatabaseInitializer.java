package io.arconia.multitenancy.data.autoconfigure;

import org.springframework.boot.jdbc.init.PropertiesBasedDataSourceScriptDatabaseInitializer;

import javax.sql.DataSource;

class DataJdbcDataSourceScriptDatabaseInitializer
        extends PropertiesBasedDataSourceScriptDatabaseInitializer<MultitenancyDataJdbcProperties> {

    public DataJdbcDataSourceScriptDatabaseInitializer(DataSource dataSource,
                                                       MultitenancyDataJdbcProperties properties) {
        super(dataSource, properties);
    }

}
