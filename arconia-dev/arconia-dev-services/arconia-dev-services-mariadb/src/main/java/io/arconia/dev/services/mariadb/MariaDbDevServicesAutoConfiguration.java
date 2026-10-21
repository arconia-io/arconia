package io.arconia.dev.services.mariadb;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.mariadb.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.registration.DevServiceCategories;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.core.registration.DiscoveredContainer;
import io.arconia.dev.services.core.registration.JdbcDiscoveredConnectionDetails;
import io.arconia.dev.services.mariadb.MariaDbDevServicesAutoConfiguration.MariaDbDevServicesRegistrar;

/**
 * Auto-configuration for MariaDB Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("mariadb")
@EnableConfigurationProperties(MariaDbDevServicesProperties.class)
@Import(MariaDbDevServicesRegistrar.class)
public final class MariaDbDevServicesAutoConfiguration {

    static final String COMPATIBLE_IMAGE_NAME = "mariadb";

    static final int MARIADB_PORT = 3306;

    static MariaDBContainer createContainer(MariaDbDevServicesProperties properties) {
        var container = new MariaDBContainer(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        // Testcontainers uses a shared wait strategy instance across all containers.
        // MariaDBContainer doesn't set a wait strategy of its own, so when we customize
        // the startup timeout, it will be applied to all containers. Hence, we must
        // provide an explicit wait strategy.
        container.waitingFor(Wait.defaultWaitStrategy());
        return container;
    }

    static class MariaDbDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(MariaDbDevServicesProperties.CONFIG_PREFIX, MariaDbDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("mariadb")
                    .description("MariaDB Dev Service")
                    .category(DevServiceCategories.JDBC)
                    .properties(properties)
                    .container(MariaDBContainer.class, () -> createContainer(properties))
                    .port(MARIADB_PORT)
                    .discovery(JdbcConnectionDetails.class,
                            container -> discoveredConnectionDetails(container, properties)));
        }

        static JdbcDiscoveredConnectionDetails discoveredConnectionDetails(DiscoveredContainer container, MariaDbDevServicesProperties properties) {
            String jdbcUrl = "jdbc:mariadb://%s:%d/%s".formatted(container.host(),
                    container.mappedPort(MARIADB_PORT), properties.getDbName());
            return new JdbcDiscoveredConnectionDetails(jdbcUrl, properties.getUsername(), properties.getPassword());
        }

    }

}
