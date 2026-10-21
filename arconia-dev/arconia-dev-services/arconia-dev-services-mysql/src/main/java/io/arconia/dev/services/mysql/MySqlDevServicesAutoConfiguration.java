package io.arconia.dev.services.mysql;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.registration.DevServiceCategories;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.core.registration.DiscoveredContainer;
import io.arconia.dev.services.core.registration.JdbcDiscoveredConnectionDetails;
import io.arconia.dev.services.mysql.MySqlDevServicesAutoConfiguration.MySqlDevServicesRegistrar;

/**
 * Auto-configuration for MySQL Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("mysql")
@EnableConfigurationProperties(MySqlDevServicesProperties.class)
@Import(MySqlDevServicesRegistrar.class)
public final class MySqlDevServicesAutoConfiguration {

    static final String COMPATIBLE_IMAGE_NAME = "mysql";

    static MySQLContainer createContainer(MySqlDevServicesProperties properties) {
        var container = new MySQLContainer(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        // Testcontainers uses a shared wait strategy instance across all containers.
        // MySQLContainer doesn't set a wait strategy of its own, so when we customize
        // the startup timeout, it will be applied to all containers. Hence, we must
        // provide an explicit wait strategy.
        container.waitingFor(Wait.defaultWaitStrategy());
        return container;
    }

    static class MySqlDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(MySqlDevServicesProperties.CONFIG_PREFIX, MySqlDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("mysql")
                    .description("MySQL Dev Service")
                    .category(DevServiceCategories.JDBC)
                    .properties(properties)
                    .container(MySQLContainer.class, () -> createContainer(properties))
                    .port(MySQLContainer.MYSQL_PORT)
                    .discovery(JdbcConnectionDetails.class,
                            container -> discoveredConnectionDetails(container, properties)));
        }

        static JdbcDiscoveredConnectionDetails discoveredConnectionDetails(DiscoveredContainer container, MySqlDevServicesProperties properties) {
            String jdbcUrl = "jdbc:mysql://%s:%d/%s".formatted(container.host(),
                    container.mappedPort(MySQLContainer.MYSQL_PORT), properties.getDbName());
            return new JdbcDiscoveredConnectionDetails(jdbcUrl, properties.getUsername(), properties.getPassword());
        }

    }

}
