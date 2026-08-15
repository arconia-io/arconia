package io.arconia.dev.services.tests;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.api.config.JdbcDevServicesProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base test class for testing {@link JdbcDevServicesProperties} implementations.
 *
 * @param <T> the specific {@link JdbcDevServicesProperties} implementation type
 */
public abstract class BaseJdbcDevServicesPropertiesTests<T extends JdbcDevServicesProperties> extends BaseDevServicesPropertiesTests<T> {

    private static final String EXPECTED_USERNAME = "arconia";
    private static final String EXPECTED_PASSWORD = "arconia";
    private static final String EXPECTED_DB_NAME = "arconia";

    @Test
    void shouldCreateInstanceWithJdbcDefaultValues() {
        T properties = createProperties();

        assertThat(properties.getUsername()).isEqualTo(EXPECTED_USERNAME);
        assertThat(properties.getPassword()).isEqualTo(EXPECTED_PASSWORD);
        assertThat(properties.getDbName()).isEqualTo(EXPECTED_DB_NAME);
        assertThat(properties.getInitScriptPaths()).isEmpty();
    }

    @Test
    void shouldBindCommonJdbcProperties() {
        T properties = createProperties();

        bind(properties, Map.of(
                "username", "mytest",
                "password", "mytest",
                "db-name", "mytest",
                "init-script-paths[0]", "init.sql"
        ));

        assertThat(properties.getUsername()).isEqualTo("mytest");
        assertThat(properties.getPassword()).isEqualTo("mytest");
        assertThat(properties.getDbName()).isEqualTo("mytest");
        assertThat(properties.getInitScriptPaths()).containsExactly("init.sql");
    }

}
