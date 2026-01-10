package io.arconia.dev.services.lgtm;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.grafana.LgtmStackContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.config.DevServicesBeanRegistrations;
import io.arconia.opentelemetry.autoconfigure.ConditionalOnOpenTelemetry;

/**
 * Auto-configuration for Grafana LGTM Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnOpenTelemetry
@ConditionalOnBooleanProperty(prefix = "arconia.dev.services.lgtm", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(LgtmDevServicesProperties.class)
public final class LgtmDevServicesAutoConfiguration {

    private static final String COMPATIBLE_IMAGE_NAME = "grafana/otel-lgtm";

    @Bean
    @ServiceConnection
    @ConditionalOnMissingBean
    LgtmStackContainer lgtmContainer(LgtmDevServicesProperties properties) {
        return new ArconiaLgtmStackContainer(DockerImageName.parse(properties.getImageName())
                .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME))
                .withEnv(properties.getEnvironment())
                .withStartupTimeout(properties.getStartupTimeout())
                .withReuse(properties.getShared().asBoolean());
    }

    @Bean
    static BeanFactoryPostProcessor lgtmContainerPostProcessor() {
        return DevServicesBeanRegistrations.beanFactoryPostProcessor(LgtmStackContainer.class);
    }

}
