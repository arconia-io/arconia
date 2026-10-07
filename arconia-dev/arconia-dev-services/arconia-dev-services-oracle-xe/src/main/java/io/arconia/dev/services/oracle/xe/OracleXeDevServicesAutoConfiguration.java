package io.arconia.dev.services.oracle.xe;

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
import io.arconia.dev.services.oracle.xe.OracleXeDevServicesAutoConfiguration.OracleXeDevServicesRegistrar;

/**
 * Auto-configuration for Oracle XE Dev Services.
 */
@AutoConfiguration(after = DevServicesAutoConfiguration.class, before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("oracle-xe")
@EnableConfigurationProperties(OracleXeDevServicesProperties.class)
@Import(OracleXeDevServicesRegistrar.class)
public final class OracleXeDevServicesAutoConfiguration {

    static class OracleXeDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(OracleXeDevServicesProperties.CONFIG_PREFIX, OracleXeDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("oracle-xe")
                    .description("Oracle XE Dev Service")
                    .category(DevServiceCategories.JDBC)
                    .properties(properties)
                    .container(ArconiaOracleXeContainer.class, () -> new ArconiaOracleXeContainer(properties))
                    .discovery(JdbcConnectionDetails.class,
                            container -> discoveredConnectionDetails(container, properties)));
        }

        static JdbcDiscoveredConnectionDetails discoveredConnectionDetails(DiscoveredContainer container, OracleXeDevServicesProperties properties) {
            String jdbcUrl = "jdbc:oracle:thin:@%s:%d/%s".formatted(container.host(),
                    container.mappedPort(ArconiaOracleXeContainer.ORACLE_PORT), properties.getDbName());
            return new JdbcDiscoveredConnectionDetails(jdbcUrl, properties.getUsername(), properties.getPassword());
        }

    }

}
