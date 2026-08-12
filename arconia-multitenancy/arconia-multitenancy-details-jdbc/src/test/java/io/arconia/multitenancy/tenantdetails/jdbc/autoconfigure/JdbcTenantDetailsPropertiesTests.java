package io.arconia.multitenancy.tenantdetails.jdbc.autoconfigure;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.sql.init.DatabaseInitializationMode;
import org.springframework.core.io.DefaultResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JdbcTenantDetailsProperties}.
 */
class JdbcTenantDetailsPropertiesTests {

    @Test
    void defaultValues() {
        var properties = new JdbcTenantDetailsProperties();

        assertThat(properties.getSchema()).isEqualTo(JdbcTenantDetailsProperties.DEFAULT_SCHEMA_LOCATION);
        assertThat(properties.getPlatform()).isNull();
        assertThat(properties.getInitializeSchema()).isEqualTo(DatabaseInitializationMode.EMBEDDED);
        assertThat(properties.isContinueOnError()).isTrue();
    }

    @Test
    void defaultSchemaLocationResolvesForSupportedPlatforms() {
        var properties = new JdbcTenantDetailsProperties();
        var resourceLoader = new DefaultResourceLoader();

        for (var platform : List.of("h2", "postgresql")) {
            var location = properties.getSchema().replace("@@platform@@", platform);
            assertThat(resourceLoader.getResource(location).isReadable())
                .as("schema script for platform '%s' at %s", platform, location)
                .isTrue();
        }
    }

}
