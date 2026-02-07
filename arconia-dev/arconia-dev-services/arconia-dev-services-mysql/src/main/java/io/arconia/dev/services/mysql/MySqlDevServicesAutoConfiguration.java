package io.arconia.dev.services.mysql;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.autoconfigure.DevServicesAutoConfiguration;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.mysql.MySqlDevServicesAutoConfiguration.MySqlDevServicesRegistrar;

/**
 * Auto-configuration for MySQL Dev Services.
 */
@AutoConfiguration(after = DevServicesAutoConfiguration.class, before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("mysql")
@EnableConfigurationProperties(MySqlDevServicesProperties.class)
@Import(MySqlDevServicesRegistrar.class)
public final class MySqlDevServicesAutoConfiguration {

    static class MySqlDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(MySqlDevServicesProperties.CONFIG_PREFIX, MySqlDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("mysql")
                    .description("MySQL Dev Service")
                    .container(container -> container
                            .type(ArconiaMySqlContainer.class)
                            .supplier(() -> new ArconiaMySqlContainer(properties))
                    ));
        }

    }

}
