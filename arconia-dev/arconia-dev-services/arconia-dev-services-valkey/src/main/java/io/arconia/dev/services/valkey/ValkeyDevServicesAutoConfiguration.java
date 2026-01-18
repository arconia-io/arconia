package io.arconia.dev.services.valkey;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.config.DevServicesBeanRegistrations;
import io.arconia.testcontainers.valkey.ValkeyContainer;

/**
 * Auto-configuration for Valkey Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnBooleanProperty(prefix = "arconia.dev.services.valkey", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(ValkeyDevServicesProperties.class)
public final class ValkeyDevServicesAutoConfiguration {

    private static final String COMPATIBLE_IMAGE_NAME = "ghcr.io/valkey-io/valkey";

    @Bean
    @ServiceConnection("redis")
    @ConditionalOnMissingBean
    ValkeyContainer valkeyContainer(ValkeyDevServicesProperties properties) {
        return new ArconiaValkeyContainer(DockerImageName.parse(properties.getImageName())
                .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME), properties)
                .withEnv(properties.getEnvironment())
                .withStartupTimeout(properties.getStartupTimeout())
                .withReuse(properties.getShared().asBoolean());
    }

    @Bean
    static BeanFactoryPostProcessor valkeyContainerPostProcessor() {
        return DevServicesBeanRegistrations.beanFactoryPostProcessor(ValkeyContainer.class);
    }

}
