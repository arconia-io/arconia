package io.arconia.dev.services.docling;

import ai.docling.testcontainers.serve.DoclingServeContainer;
import ai.docling.testcontainers.serve.config.DoclingServeContainerConfig;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Bean;

import io.arconia.boot.bootstrap.BootstrapMode;
import io.arconia.dev.services.core.config.DevServicesBeanRegistrations;

/**
 * Auto-configuration for Docling Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnBooleanProperty(prefix = "arconia.dev.services.docling", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(DoclingDevServicesProperties.class)
public final class DoclingDevServicesAutoConfiguration {

    @Bean
    @ServiceConnection
    @ConditionalOnMissingBean
    DoclingServeContainer doclingServeContainer(DoclingDevServicesProperties properties) {
        return new ArconiaDoclingServeContainer(DoclingServeContainerConfig.builder()
                    .image(properties.getImageName())
                    .enableUi(shouldEnableUi(properties))
                    .containerEnv(properties.getEnvironment())
                    .startupTimeout(properties.getStartupTimeout())
                    .build(), properties)
                .withReuse(properties.getShared().asBoolean());
    }

    private static boolean shouldEnableUi(DoclingDevServicesProperties properties) {
        if (BootstrapMode.DEV == BootstrapMode.detect()) {
            return properties.isEnableUi();
        }
        return false;
    }

    @Bean
    static BeanFactoryPostProcessor doclingServeContainerPostProcessor() {
        return DevServicesBeanRegistrations.beanFactoryPostProcessor(DoclingServeContainer.class);
    }

}
