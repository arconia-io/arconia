package io.arconia.dev.services.mariadb;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.autoconfigure.DevServicesAutoConfiguration;
import io.arconia.dev.services.core.registration.DevServiceCategories;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.core.registration.DiscoveredContainer;
import io.arconia.dev.services.core.registration.JdbcDiscoveredConnectionDetails;
import io.arconia.dev.services.mariadb.MariaDbDevServicesAutoConfiguration.MariaDbDevServicesRegistrar;

/**
 * Auto-configuration for MariaDB Dev Services.
 */
@AutoConfiguration(after = DevServicesAutoConfiguration.class, before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("mariadb")
@EnableConfigurationProperties(MariaDbDevServicesProperties.class)
@Import(MariaDbDevServicesRegistrar.class)
public final class MariaDbDevServicesAutoConfiguration {

    static class MariaDbDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(MariaDbDevServicesProperties.CONFIG_PREFIX, MariaDbDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("mariadb")
                    .description("MariaDB Dev Service")
                    .category(DevServiceCategories.JDBC)
                    .properties(properties)
                    .container(ArconiaMariaDbContainer.class, () -> new ArconiaMariaDbContainer(properties))
                    .discovery(JdbcConnectionDetails.class,
                            container -> discoveredConnectionDetails(container, properties)));
        }

        static JdbcDiscoveredConnectionDetails discoveredConnectionDetails(DiscoveredContainer container, MariaDbDevServicesProperties properties) {
            String jdbcUrl = "jdbc:mariadb://%s:%d/%s".formatted(container.host(),
                    container.mappedPort(ArconiaMariaDbContainer.MARIADB_PORT), properties.getDbName());
            return new JdbcDiscoveredConnectionDetails(jdbcUrl, properties.getUsername(), properties.getPassword());
        }

    }

}
