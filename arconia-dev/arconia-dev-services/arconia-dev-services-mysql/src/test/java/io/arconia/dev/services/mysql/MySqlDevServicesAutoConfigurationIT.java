package io.arconia.dev.services.mysql;

import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.mysql.MySQLContainer;

import io.arconia.dev.services.tests.BaseJdbcDevServicesAutoConfigurationIT;

/**
 * Integration tests for {@link MySqlDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class MySqlDevServicesAutoConfigurationIT extends BaseJdbcDevServicesAutoConfigurationIT {

    private final ApplicationContextRunner contextRunner = defaultContextRunner(MySqlDevServicesAutoConfiguration.class);

    @Override
    protected ApplicationContextRunner getContextRunner() {
        return contextRunner;
    }

    @Override
    protected Class<?> getAutoConfigurationClass() {
        return MySqlDevServicesAutoConfiguration.class;
    }

    @Override
    protected Class<? extends JdbcDatabaseContainer<?>> getContainerClass() {
        return MySQLContainer.class;
    }

    @Override
    protected String getServiceName() {
        return "mysql";
    }

    @Override
    protected Class<?> getConnectionDetailsClass() {
        return JdbcConnectionDetails.class;
    }

    @Override
    protected GenericContainer<?> createDiscoverableContainer(String ownerId) {
        var properties = new MySqlDevServicesProperties();
        return asDiscoverableContainer(new ArconiaMySqlContainer(properties), properties, ownerId);
    }

}
