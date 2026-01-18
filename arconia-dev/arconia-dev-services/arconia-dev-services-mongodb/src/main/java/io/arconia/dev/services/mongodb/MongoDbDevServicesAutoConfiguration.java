package io.arconia.dev.services.mongodb;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.config.DevServicesBeanRegistrations;

/**
 * Auto-configuration for MongoDB Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnBooleanProperty(prefix = "arconia.dev.services.mongodb", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(MongoDbDevServicesProperties.class)
public final class MongoDbDevServicesAutoConfiguration {

    private static final String COMPATIBLE_IMAGE_NAME = "mongo";

    @Bean
    @ServiceConnection
    @ConditionalOnMissingBean
    MongoDBContainer mongoDbContainer(MongoDbDevServicesProperties properties) {
        return new ArconiaMongoDbContainer(DockerImageName.parse(properties.getImageName())
                .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME), properties)
                .withEnv(properties.getEnvironment())
                .withStartupTimeout(properties.getStartupTimeout())
                .withReuse(properties.getShared().asBoolean());
    }

    @Bean
    static BeanFactoryPostProcessor mongoDbContainerPostProcessor() {
        return DevServicesBeanRegistrations.beanFactoryPostProcessor(MongoDBContainer.class);
    }

}
