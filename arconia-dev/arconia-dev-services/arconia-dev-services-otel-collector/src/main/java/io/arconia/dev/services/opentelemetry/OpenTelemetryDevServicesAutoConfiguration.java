package io.arconia.dev.services.opentelemetry;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.opentelemetry.OpenTelemetryDevServicesAutoConfiguration.ConfigurationWithRestart;
import io.arconia.dev.services.opentelemetry.OpenTelemetryDevServicesAutoConfiguration.ConfigurationWithoutRestart;
import io.arconia.opentelemetry.autoconfigure.ConditionalOnOpenTelemetry;

/**
 * Auto-configuration for OpenTelemetry Collector Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnOpenTelemetry
@ConditionalOnBooleanProperty(prefix = "arconia.dev.services.otel-collector", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(OpenTelemetryDevServicesProperties.class)
@Import({ConfigurationWithRestart.class, ConfigurationWithoutRestart.class})
public final class OpenTelemetryDevServicesAutoConfiguration {

    public static final int GRPC_PORT = 4317;
    public static final int HTTP_PORT = 4318;

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RestartScope.class)
    public static final class ConfigurationWithRestart {

        @Bean
        @RestartScope
        @ServiceConnection("otel/opentelemetry-collector")
        @ConditionalOnMissingBean
        GenericContainer<?> lgtmContainer(OpenTelemetryDevServicesProperties properties) {
            return new GenericContainer<>(DockerImageName.parse(properties.getImageName()))
                    .withExposedPorts(GRPC_PORT, HTTP_PORT)
                    .withEnv(properties.getEnvironment())
                    .withStartupTimeout(properties.getStartupTimeout())
                    .withReuse(properties.getShared().asBoolean());
        }

    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("org.springframework.boot.devtools.restart.RestartScope")
    public static final class ConfigurationWithoutRestart {

        @Bean
        @ServiceConnection("otel/opentelemetry-collector")
        @ConditionalOnMissingBean
        GenericContainer<?> lgtmContainerNoRestartScope(OpenTelemetryDevServicesProperties properties) {
            return new GenericContainer<>(DockerImageName.parse(properties.getImageName()))
                    .withExposedPorts(GRPC_PORT, HTTP_PORT)
                    .withEnv(properties.getEnvironment())
                    .withStartupTimeout(properties.getStartupTimeout())
                    .withReuse(properties.getShared().asBoolean());
        }

    }

}
