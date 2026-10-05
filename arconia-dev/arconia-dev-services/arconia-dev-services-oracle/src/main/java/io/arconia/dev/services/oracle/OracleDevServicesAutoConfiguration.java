package io.arconia.dev.services.oracle;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import io.arconia.dev.services.api.provider.DevServiceCategories;
import io.arconia.dev.services.api.provider.DevServiceProvider;
import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.autoconfigure.DevServicesAutoConfiguration;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.core.registration.DiscoveredContainer;
import io.arconia.dev.services.core.registration.JdbcDiscoveredConnectionDetails;
import io.arconia.dev.services.oracle.OracleDevServicesAutoConfiguration.OracleDevServicesRegistrar;

/**
 * Auto-configuration for Oracle Dev Services.
 */
@AutoConfiguration(after = DevServicesAutoConfiguration.class, before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("oracle")
@EnableConfigurationProperties(OracleDevServicesProperties.class)
@Import(OracleDevServicesRegistrar.class)
public final class OracleDevServicesAutoConfiguration {

    @Bean
    DevServiceProvider oracleDevServiceProvider() {
        return DevServiceProvider.of("oracle", DevServiceCategories.JDBC);
    }

    static class OracleDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(OracleDevServicesProperties.CONFIG_PREFIX, OracleDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("oracle")
                    .description("Oracle Dev Service")
                    .properties(properties)
                    .container(ArconiaOracleContainer.class, () -> new ArconiaOracleContainer(properties))
                    .discovery(JdbcConnectionDetails.class,
                            container -> discoveredConnectionDetails(container, properties)));
        }

        static JdbcDiscoveredConnectionDetails discoveredConnectionDetails(DiscoveredContainer container, OracleDevServicesProperties properties) {
            String jdbcUrl = "jdbc:oracle:thin:@%s:%d/%s".formatted(container.host(),
                    container.mappedPort(ArconiaOracleContainer.ORACLE_PORT), properties.getDbName());
            return new JdbcDiscoveredConnectionDetails(jdbcUrl, properties.getUsername(), properties.getPassword());
        }

    }

}
