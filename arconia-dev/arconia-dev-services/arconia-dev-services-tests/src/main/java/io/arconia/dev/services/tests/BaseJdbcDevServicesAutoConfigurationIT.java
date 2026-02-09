package io.arconia.dev.services.tests;

import org.testcontainers.containers.JdbcDatabaseContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for integration tests of JDBC dev services auto-configuration.
 */
public abstract class BaseJdbcDevServicesAutoConfigurationIT extends BaseDevServicesAutoConfigurationIT{

    /**
     * The specific container bean class for the Dev Service to test.
     */
    @Override
    protected abstract Class<? extends JdbcDatabaseContainer<?>> getContainerClass();

    /**
     * Build common JDBC configuration properties for a service.
     */
    protected String[] commonJdbcConfigurationProperties() {
        String prefix = "arconia.dev.services." + getServiceName();
        return new String[] {
                prefix + ".username=mytest",
                prefix + ".password=mytest",
                prefix + ".db-name=mytest",
                prefix + ".init-script-paths=sql/init.sql"
        };
    }

    /**
     * Assert common JDBC configuration properties were applied correctly.
     * Container must be started before calling.
     */
    protected void assertThatJdbcConfigurationIsApplied(JdbcDatabaseContainer<?> container) {
        assertThat(container.getUsername()).isEqualTo("mytest");
        assertThat(container.getPassword()).isEqualTo("mytest");
        assertThat(container.getDatabaseName()).isEqualTo("mytest");
    }

}
