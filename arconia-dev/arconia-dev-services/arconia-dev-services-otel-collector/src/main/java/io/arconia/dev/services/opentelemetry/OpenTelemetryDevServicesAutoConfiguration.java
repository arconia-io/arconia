package io.arconia.dev.services.opentelemetry;

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

/**
 * Auto-configuration for OpenTelemetry Collector Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnOpenTelemetry
@ConditionalOnBooleanProperty(prefix = "arconia.dev.services.otel-collector", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(OpenTelemetryDevServicesProperties.class)
public final class OpenTelemetryDevServicesAutoConfiguration {

    @Bean
    @ServiceConnection("otel/opentelemetry-collector")
    @ConditionalOnMissingBean
    ArconiaOpenTelemetryCollectorContainer otelCollectorContainer(OpenTelemetryDevServicesProperties properties) {
        return new ArconiaOpenTelemetryCollectorContainer(DockerImageName.parse(properties.getImageName()))
                .withEnv(properties.getEnvironment())
                .withStartupTimeout(properties.getStartupTimeout())
                .withReuse(properties.getShared().asBoolean());
    }

    @Bean
    static BeanFactoryPostProcessor otelCollectorContainerPostProcessor() {
        return DevServicesBeanRegistrations.beanFactoryPostProcessor(ArconiaOpenTelemetryCollectorContainer.class);
    }

}
