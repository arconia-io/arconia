package io.arconia.dev.services.mongodb.atlas;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.autoconfigure.DevServicesAutoConfiguration;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.mongodb.atlas.MongoDbAtlasDevServicesAutoConfiguration.MongoDbAtlasDevServicesRegistrar;

/**
 * Auto-configuration for MongoDB Atlas Dev Services.
 */
@AutoConfiguration(after = DevServicesAutoConfiguration.class, before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("mongodb-atlas")
@EnableConfigurationProperties(MongoDbAtlasDevServicesProperties.class)
@Import(MongoDbAtlasDevServicesRegistrar.class)
public final class MongoDbAtlasDevServicesAutoConfiguration {

    static class MongoDbAtlasDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(MongoDbAtlasDevServicesProperties.CONFIG_PREFIX, MongoDbAtlasDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("mongodb-atlas")
                    .description("MongoDB Atlas Dev Service")
                    .container(container -> container
                            .type(ArconiaMongoDbAtlasLocalContainer.class)
                            .supplier(() -> new ArconiaMongoDbAtlasLocalContainer(properties)
                                    .withEnv(properties.getEnvironment())
                                    .withNetworkAliases(properties.getNetworkAliases().toArray(new String[]{}))
                                    .withStartupTimeout(properties.getStartupTimeout())
                                    .withReuse(isDevMode() && properties.isShared()))
                    ));
        }

    }

}
