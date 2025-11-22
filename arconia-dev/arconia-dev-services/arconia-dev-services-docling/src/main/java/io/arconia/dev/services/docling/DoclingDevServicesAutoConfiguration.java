package io.arconia.dev.services.docling;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ai.docling.testcontainers.serve.DoclingServeContainer;
import ai.docling.testcontainers.serve.config.DoclingServeContainerConfig;

import io.arconia.boot.bootstrap.BootstrapMode;
import io.arconia.dev.services.docling.DoclingDevServicesAutoConfiguration.ConfigurationWithRestart;
import io.arconia.dev.services.docling.DoclingDevServicesAutoConfiguration.ConfigurationWithoutRestart;

/**
 * Auto-configuration for Docling Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnProperty(prefix = "arconia.dev.services.docling", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(DoclingDevServicesProperties.class)
@Import({ConfigurationWithRestart.class, ConfigurationWithoutRestart.class})
public final class DoclingDevServicesAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RestartScope.class)
    public static final class ConfigurationWithRestart {

        @Bean
        @RestartScope
        @ServiceConnection
        @ConditionalOnMissingBean
        DoclingServeContainer doclingServeContainer(DoclingDevServicesProperties properties) {
            return new DoclingServeContainer(DoclingServeContainerConfig.builder()
                        .image(properties.getImageName())
                        .enableUi(shouldEnableUi(properties))
                        .containerEnv(properties.getEnvironment())
                        .startupTimeout(properties.getStartupTimeout())
                        .build())
                    .withReuse(properties.getShared().asBoolean());
        }

    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("org.springframework.boot.devtools.restart.RestartScope")
    public static final class ConfigurationWithoutRestart {

        @Bean
        @ServiceConnection
        @ConditionalOnMissingBean
        DoclingServeContainer doclingServeContainerWithoutRestartScope(DoclingDevServicesProperties properties) {
            return new DoclingServeContainer(DoclingServeContainerConfig.builder()
                    .image(properties.getImageName())
                    .enableUi(shouldEnableUi(properties))
                    .containerEnv(properties.getEnvironment())
                    .startupTimeout(properties.getStartupTimeout())
                    .build())
                    .withReuse(properties.getShared().asBoolean());
        }

    }

    private static boolean shouldEnableUi(DoclingDevServicesProperties properties) {
        if (BootstrapMode.DEV == BootstrapMode.detect()) {
            return properties.isEnableUi();
        }
        return false;
    }

}
