package io.arconia.dev.services.phoenix;

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
import io.arconia.opentelemetry.autoconfigure.ConditionalOnOpenTelemetry;
import io.arconia.testcontainers.phoenix.PhoenixContainer;

/**
 * Auto-configuration for Arize Phoenix Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnOpenTelemetry
@ConditionalOnBooleanProperty(prefix = "arconia.dev.services.phoenix", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(PhoenixDevServicesProperties.class)
public final class PhoenixDevServicesAutoConfiguration {

    private static final String COMPATIBLE_IMAGE_NAME = "arizephoenix/phoenix";

    @Bean
    @ServiceConnection("phoenix")
    @ConditionalOnMissingBean
    PhoenixContainer phoenixContainer(PhoenixDevServicesProperties properties) {
        return new ArconiaPhoenixContainer(DockerImageName.parse(properties.getImageName())
                .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME), properties)
                .withEnv(properties.getEnvironment())
                .withStartupTimeout(properties.getStartupTimeout())
                .withReuse(properties.getShared().asBoolean());
    }

    @Bean
    static BeanFactoryPostProcessor phoenixContainerPostProcessor() {
        return DevServicesBeanRegistrations.beanFactoryPostProcessor(PhoenixContainer.class);
    }

}
