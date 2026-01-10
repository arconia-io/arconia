package io.arconia.dev.services.mongodb.atlas;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mongodb.MongoDBAtlasLocalContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.config.DevServicesBeanRegistrations;

/**
 * Auto-configuration for MongoDB Atlas Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnBooleanProperty(prefix = "arconia.dev.services.mongodb-atlas", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(MongoDbAtlasDevServicesProperties.class)
public final class MongoDbAtlasDevServicesAutoConfiguration {

    private static final String COMPATIBLE_IMAGE_NAME = "mongodb/mongodb-atlas-local";

    @Bean
    @ServiceConnection("mongodb")
    @ConditionalOnMissingBean
    MongoDBAtlasLocalContainer mongoDbAtlasLocalContainer(MongoDbAtlasDevServicesProperties properties) {
        return new ArconiaMongoDbAtlasLocalContainer(DockerImageName.parse(properties.getImageName())
                .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME))
                .withEnv(properties.getEnvironment())
                .withStartupTimeout(properties.getStartupTimeout())
                .withReuse(properties.getShared().asBoolean());
    }

    @Bean
    static BeanFactoryPostProcessor mongoDbAtlasContainerPostProcessor() {
        return DevServicesBeanRegistrations.beanFactoryPostProcessor(MongoDBAtlasLocalContainer.class);
    }

}
