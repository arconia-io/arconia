package io.arconia.dev.services.tests;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.api.config.JdbcDevServicesProperties;

import static io.arconia.dev.services.api.config.JdbcDevServicesProperties.DEFAULT_DB_NAME;
import static io.arconia.dev.services.api.config.JdbcDevServicesProperties.DEFAULT_PASSWORD;
import static io.arconia.dev.services.api.config.JdbcDevServicesProperties.DEFAULT_USERNAME;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base test class for testing {@link JdbcDevServicesProperties} implementations.
 *
 * @param <T> the specific {@link JdbcDevServicesProperties} implementation type
 */
public abstract class BaseJdbcDevServicesPropertiesTests<T extends JdbcDevServicesProperties> extends BaseDevServicesPropertiesTests<T> {

    @Test
    void shouldCreateInstanceWithJdbcDefaultValues() {
        T properties = createProperties();

        assertThat(properties.getUsername()).isEqualTo(DEFAULT_USERNAME);
        assertThat(properties.getPassword()).isEqualTo(DEFAULT_PASSWORD);
        assertThat(properties.getDbName()).isEqualTo(DEFAULT_DB_NAME);
        assertThat(properties.getInitScriptPaths()).isEmpty();
    }

    @Test
    void shouldUpdateCommonJdbcProperties() {
        T properties = createProperties();

        properties.setUsername("mytest");
        properties.setPassword("mytest");
        properties.setDbName("mytest");
        properties.setInitScriptPaths(List.of("init.sql"));

        assertThat(properties.getUsername()).isEqualTo("mytest");
        assertThat(properties.getPassword()).isEqualTo("mytest");
        assertThat(properties.getDbName()).isEqualTo("mytest");
        assertThat(properties.getInitScriptPaths()).containsExactly("init.sql");
    }

}
