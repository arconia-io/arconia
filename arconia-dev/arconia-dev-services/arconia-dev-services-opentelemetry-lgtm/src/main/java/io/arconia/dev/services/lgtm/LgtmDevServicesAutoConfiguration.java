package io.arconia.dev.services.lgtm;

import java.time.Duration;

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
import org.testcontainers.grafana.LgtmStackContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.lgtm.LgtmDevServicesAutoConfiguration.ConfigurationWithRestart;
import io.arconia.dev.services.lgtm.LgtmDevServicesAutoConfiguration.ConfigurationWithoutRestart;
import io.arconia.opentelemetry.autoconfigure.sdk.ConditionalOnOpenTelemetry;

/**
 * Auto-configuration for OpenTelemetry LGTM Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnOpenTelemetry
@ConditionalOnProperty(prefix = LgtmDevServicesProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(LgtmDevServicesProperties.class)
@Import({ConfigurationWithRestart.class, ConfigurationWithoutRestart.class})
public class LgtmDevServicesAutoConfiguration {

    public static final String COMPATIBLE_IMAGE_NAME = "grafana/otel-lgtm";

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RestartScope.class)
    public static class ConfigurationWithRestart {

        @Bean
        @RestartScope
        @ServiceConnection
        @ConditionalOnMissingBean
        LgtmStackContainer lgtmContainer(LgtmDevServicesProperties properties) {
            return new LgtmStackContainer(DockerImageName.parse(properties.getImageName())
                    .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME))
                    .withStartupTimeout(Duration.ofMinutes(2))
                    .withReuse(properties.isReusable());
        }

    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("org.springframework.boot.devtools.restart.RestartScope")
    public static class ConfigurationWithoutRestart {

        @Bean
        @ServiceConnection
        @ConditionalOnMissingBean
        LgtmStackContainer lgtmContainerNoRestartScope(LgtmDevServicesProperties properties) {
            return new LgtmStackContainer(DockerImageName.parse(properties.getImageName())
                    .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME))
                    .withStartupTimeout(Duration.ofMinutes(2))
                    .withReuse(properties.isReusable());
        }

    }

}
