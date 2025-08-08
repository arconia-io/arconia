package io.arconia.dev.services.docling;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.docling.DoclingDevServicesAutoConfiguration.ConfigurationWithRestart;
import io.arconia.dev.services.docling.DoclingDevServicesAutoConfiguration.ConfigurationWithoutRestart;

/**
 * Auto-configuration for Docling Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnProperty(prefix = DoclingDevServicesProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(DoclingDevServicesProperties.class)
@Import({ConfigurationWithRestart.class, ConfigurationWithoutRestart.class})
public final class DoclingDevServicesAutoConfiguration {

    public static final String COMPATIBLE_IMAGE_NAME = "ghcr.io/docling-project/docling-serve";
    public static final int DEFAULT_PORT = 5001;

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RestartScope.class)
    public static final class ConfigurationWithRestart {

        @Bean
        @RestartScope
        @ServiceConnection("docling")
        GenericContainer<?> doclingContainer(DoclingDevServicesProperties properties) {
            return new GenericContainer<>(DockerImageName.parse(properties.getImageName())
                    .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME))
                    .withExposedPorts(DEFAULT_PORT)
                    .withEnv(computeEnvironment(properties))
                    .withStartupTimeout(properties.getStartupTimeout())
                    .withReuse(properties.getShared().asBoolean());
        }

    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("org.springframework.boot.devtools.restart.RestartScope")
    public static final class ConfigurationWithoutRestart {

        @Bean
        @ServiceConnection("docling")
        GenericContainer<?> doclingContainer(DoclingDevServicesProperties properties) {
            return new GenericContainer<>(DockerImageName.parse(properties.getImageName())
                    .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME))
                    .withExposedPorts(DEFAULT_PORT)
                    .withEnv(computeEnvironment(properties))
                    .withStartupTimeout(properties.getStartupTimeout())
                    .withReuse(properties.getShared().asBoolean());
        }

    }

    private static Map<String, String> computeEnvironment(DoclingDevServicesProperties properties) {
        Map<String,String> environment = new HashMap<>(properties.getEnvironment());
        environment.put("DOCLING_SERVE_ENABLE_UI", properties.isEnableUi() ? "1": "0");
        return environment;
    }

}
