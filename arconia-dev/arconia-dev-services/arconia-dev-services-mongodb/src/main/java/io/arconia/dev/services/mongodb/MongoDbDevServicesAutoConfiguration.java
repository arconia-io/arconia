package io.arconia.dev.services.mongodb;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.autoconfigure.DevServicesAutoConfiguration;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.mongodb.MongoDbDevServicesAutoConfiguration.MongoDbDevServicesRegistrar;

/**
 * Auto-configuration for MongoDB Dev Services.
 */
@AutoConfiguration(after = DevServicesAutoConfiguration.class, before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("mongodb")
@EnableConfigurationProperties(MongoDbDevServicesProperties.class)
@Import(MongoDbDevServicesRegistrar.class)
public final class MongoDbDevServicesAutoConfiguration {

    static class MongoDbDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(MongoDbDevServicesProperties.CONFIG_PREFIX, MongoDbDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("mongodb")
                    .description("MongoDB Dev Service")
                    .container(container -> container
                            .type(ArconiaMongoDbContainer.class)
                            .supplier(() -> new ArconiaMongoDbContainer(properties)
                                    .withEnv(properties.getEnvironment())
                                    .withNetworkAliases(properties.getNetworkAliases().toArray(new String[]{}))
                                    .withStartupTimeout(properties.getStartupTimeout())
                                    .withReuse(isDevMode() && properties.isShared()))
                    ));
        }

    }

}
