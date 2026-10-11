package io.arconia.dev.services.tests;

import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.JdbcDatabaseContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base test class for the auto-configuration of a relational database dev service,
 * which provides {@link JdbcConnectionDetails} and binds the JDBC properties.
 */
public abstract class BaseJdbcDevServicesAutoConfigurationTests extends BaseDevServicesAutoConfigurationTests {

    private final Class<? extends JdbcDatabaseContainer<?>> containerClass;

    protected BaseJdbcDevServicesAutoConfigurationTests(Class<?> autoConfiguration, String serviceName,
            Class<? extends JdbcDatabaseContainer<?>> containerClass) {
        super(autoConfiguration, serviceName, containerClass, JdbcConnectionDetails.class);
        this.containerClass = containerClass;
    }

    @Override
    protected Class<? extends JdbcDatabaseContainer<?>> getContainerClass() {
        return containerClass;
    }

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

    @Test
    void jdbcConfigurationApplied() {
        getContextRunner()
                .withPropertyValues(commonJdbcConfigurationProperties())
                .run(context -> {
                    var container = context.getBean(getContainerClass());
                    assertThat(container.getUsername()).isEqualTo("mytest");
                    assertThat(container.getPassword()).isEqualTo("mytest");
                    assertThat(container.getDatabaseName()).isEqualTo("mytest");
                });
    }

    /**
     * Assert the JDBC connection details resolved for a discovered container are
     * the ones the container itself reports.
     */
    @Override
    protected void assertDiscoveredConnectionDetails(AssertableApplicationContext context, GenericContainer<?> discoveredContainer) {
        JdbcDatabaseContainer<?> databaseContainer = (JdbcDatabaseContainer<?>) discoveredContainer;
        JdbcConnectionDetails connectionDetails = context.getBean(JdbcConnectionDetails.class);
        assertThat(connectionDetails.getJdbcUrl()).isEqualTo(databaseContainer.getJdbcUrl());
        assertThat(connectionDetails.getUsername()).isEqualTo(databaseContainer.getUsername());
        assertThat(connectionDetails.getPassword()).isEqualTo(databaseContainer.getPassword());
    }

}
