package io.arconia.dev.services.oracle;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.autoconfigure.DevServicesAutoConfiguration;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.oracle.OracleDevServicesAutoConfiguration.OracleDevServicesRegistrar;

/**
 * Auto-configuration for Oracle Dev Services.
 */
@AutoConfiguration(after = DevServicesAutoConfiguration.class, before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("oracle")
@EnableConfigurationProperties(OracleDevServicesProperties.class)
@Import(OracleDevServicesRegistrar.class)
public final class OracleDevServicesAutoConfiguration {

    static class OracleDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(OracleDevServicesProperties.CONFIG_PREFIX, OracleDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("oracle")
                    .description("Oracle Dev Service")
                    .container(container -> container
                            .type(ArconiaOracleContainer.class)
                            .supplier(() -> new ArconiaOracleContainer(properties)
                                    .withEnv(properties.getEnvironment())
                                    .withNetworkAliases(properties.getNetworkAliases().toArray(new String[]{}))
                                    .withStartupTimeout(properties.getStartupTimeout())
                                    .withReuse(isDevMode() && properties.isShared())
                                    .withUsername(properties.getUsername())
                                    .withPassword(properties.getPassword())
                                    .withDatabaseName(properties.getDbName())
                                    .withInitScripts(properties.getInitScriptPaths()))
                    ));
        }

    }

}
