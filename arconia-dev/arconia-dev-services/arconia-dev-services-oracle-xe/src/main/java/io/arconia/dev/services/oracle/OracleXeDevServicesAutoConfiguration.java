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
import io.arconia.dev.services.oracle.OracleXeDevServicesAutoConfiguration.OracleXeDevServicesRegistrar;

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
                    .container(container -> container
                            .type(ArconiaOracleXeContainer.class)
                            .supplier(() -> new ArconiaOracleXeContainer(properties))
                    ));
        }

    }

}
