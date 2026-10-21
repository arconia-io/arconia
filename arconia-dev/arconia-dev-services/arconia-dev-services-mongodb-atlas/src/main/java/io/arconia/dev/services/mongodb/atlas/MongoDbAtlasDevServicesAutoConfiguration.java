package io.arconia.dev.services.mongodb.atlas;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.mongodb.autoconfigure.MongoConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.testcontainers.mongodb.MongoDBAtlasLocalContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.registration.DevServiceCategories;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.mongodb.atlas.MongoDbAtlasDevServicesAutoConfiguration.MongoDbAtlasDevServicesRegistrar;

/**
 * Auto-configuration for MongoDB Atlas Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("mongodb-atlas")
@EnableConfigurationProperties(MongoDbAtlasDevServicesProperties.class)
@Import(MongoDbAtlasDevServicesRegistrar.class)
public final class MongoDbAtlasDevServicesAutoConfiguration {

    static final String COMPATIBLE_IMAGE_NAME = "mongodb/mongodb-atlas-local";

    static final int MONGODB_PORT = 27017;

    static MongoDBAtlasLocalContainer createContainer(MongoDbAtlasDevServicesProperties properties) {
        return new MongoDBAtlasLocalContainer(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
    }

    static class MongoDbAtlasDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(MongoDbAtlasDevServicesProperties.CONFIG_PREFIX, MongoDbAtlasDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("mongodb-atlas")
                    .description("MongoDB Atlas Dev Service")
                    .category(DevServiceCategories.MONGODB)
                    .properties(properties)
                    .container(MongoDBAtlasLocalContainer.class, () -> createContainer(properties))
                    .port(MONGODB_PORT)
                    .discovery(MongoConnectionDetails.class, MongoDbAtlasDiscoveredConnectionDetails::new));
        }

    }

}
