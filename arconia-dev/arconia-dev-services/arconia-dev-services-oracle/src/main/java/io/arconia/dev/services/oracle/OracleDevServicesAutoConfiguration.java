package io.arconia.dev.services.oracle;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.registration.DevServiceCategories;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.core.registration.DiscoveredContainer;
import io.arconia.dev.services.core.registration.JdbcDiscoveredConnectionDetails;
import io.arconia.dev.services.oracle.OracleDevServicesAutoConfiguration.OracleDevServicesRegistrar;

/**
 * Auto-configuration for Oracle Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("oracle")
@EnableConfigurationProperties(OracleDevServicesProperties.class)
@Import(OracleDevServicesRegistrar.class)
public final class OracleDevServicesAutoConfiguration {

    static final String COMPATIBLE_IMAGE_NAME = "gvenzl/oracle-free";

    static final int ORACLE_PORT = 1521;

    static OracleContainer createContainer(OracleDevServicesProperties properties) {
        return new OracleContainer(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
    }

    static class OracleDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(OracleDevServicesProperties.CONFIG_PREFIX, OracleDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("oracle")
                    .description("Oracle Dev Service")
                    .category(DevServiceCategories.JDBC)
                    .properties(properties)
                    .container(OracleContainer.class, () -> createContainer(properties))
                    .port(ORACLE_PORT)
                    .discovery(JdbcConnectionDetails.class,
                            container -> discoveredConnectionDetails(container, properties)));
        }

        static JdbcDiscoveredConnectionDetails discoveredConnectionDetails(DiscoveredContainer container, OracleDevServicesProperties properties) {
            String jdbcUrl = "jdbc:oracle:thin:@%s:%d/%s".formatted(container.host(),
                    container.mappedPort(ORACLE_PORT), properties.getDbName());
            return new JdbcDiscoveredConnectionDetails(jdbcUrl, properties.getUsername(), properties.getPassword());
        }

    }

}
