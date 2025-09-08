package io.arconia.dev.services.phoenix;

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
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.phoenix.PhoenixDevServicesAutoConfiguration.ConfigurationWithRestart;
import io.arconia.dev.services.phoenix.PhoenixDevServicesAutoConfiguration.ConfigurationWithoutRestart;
import io.arconia.opentelemetry.autoconfigure.ConditionalOnOpenTelemetry;

/**
 * Auto-configuration for Arize Phoenix Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnOpenTelemetry
@ConditionalOnBooleanProperty(prefix = "arconia.observability.openinference", name = "enabled", matchIfMissing = true)
@ConditionalOnBooleanProperty(prefix = "arconia.dev.services.phoenix", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(PhoenixDevServicesProperties.class)
@Import({ConfigurationWithRestart.class, ConfigurationWithoutRestart.class})
public final class PhoenixDevServicesAutoConfiguration {

    public static final String COMPATIBLE_IMAGE_NAME = "arizephoenix/phoenix";
    public static final int HTTP_PORT = 6006;
    public static final int GRPC_PORT = 4317;

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RestartScope.class)
    public static final class ConfigurationWithRestart {

        @Bean
        @RestartScope
        @ServiceConnection("phoenix")
        @ConditionalOnMissingBean
        GenericContainer<?> lgtmContainer(PhoenixDevServicesProperties properties) {
            return new GenericContainer<>(DockerImageName.parse(properties.getImageName())
                    .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME))
                    .withExposedPorts(GRPC_PORT, HTTP_PORT)
                    .withEnv(properties.getEnvironment())
                    .withStartupTimeout(properties.getStartupTimeout())
                    .withReuse(properties.getShared().asBoolean())
                    .waitingFor(Wait.forLogMessage(".*Application startup complete.*", 1));
        }

    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("org.springframework.boot.devtools.restart.RestartScope")
    public static final class ConfigurationWithoutRestart {

        @Bean
        @ServiceConnection("phoenix")
        @ConditionalOnMissingBean
        GenericContainer<?> lgtmContainerNoRestartScope(PhoenixDevServicesProperties properties) {
            return new GenericContainer<>(DockerImageName.parse(properties.getImageName())
                    .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME))
                    .withExposedPorts(GRPC_PORT, HTTP_PORT)
                    .withEnv(properties.getEnvironment())
                    .withStartupTimeout(properties.getStartupTimeout())
                    .withReuse(properties.getShared().asBoolean())
                    .waitingFor(Wait.forLogMessage(".*Application startup complete.*", 1));
        }

    }

}
