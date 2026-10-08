package io.arconia.dev.services.mariadb;

import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.mariadb.MariaDBContainer;

import io.arconia.dev.services.tests.BaseJdbcDevServicesAutoConfigurationIT;

/**
 * Integration tests for {@link MariaDbDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class MariaDbDevServicesAutoConfigurationIT extends BaseJdbcDevServicesAutoConfigurationIT {

    private final ApplicationContextRunner contextRunner = defaultContextRunner(MariaDbDevServicesAutoConfiguration.class);

    @Override
    protected ApplicationContextRunner getContextRunner() {
        return contextRunner;
    }

    @Override
    protected Class<?> getAutoConfigurationClass() {
        return MariaDbDevServicesAutoConfiguration.class;
    }

    @Override
    protected Class<? extends JdbcDatabaseContainer<?>> getContainerClass() {
        return MariaDBContainer.class;
    }

    @Override
    protected String getServiceName() {
        return "mariadb";
    }

    @Override
    protected Class<?> getConnectionDetailsClass() {
        return JdbcConnectionDetails.class;
    }

    @Override
    protected GenericContainer<?> createDiscoverableContainer(String ownerId) {
        var properties = new MariaDbDevServicesProperties();
        return asDiscoverableContainer(new ArconiaMariaDbContainer(properties), properties, ownerId);
    }

}
