package io.arconia.dev.service.lgtm;

import java.time.Duration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.grafana.LgtmStackContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.opentelemetry.autoconfigure.sdk.ConditionalOnOpenTelemetry;

/**
 * Auto-configuration for OpenTelemetry LGTM Dev Service.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnOpenTelemetry
@ConditionalOnProperty(prefix = LgtmDevServiceProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(LgtmDevServiceProperties.class)
public class LgtmDevServiceAutoConfiguration {

    @Bean
    @RestartScope
    @ServiceConnection
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "spring.devtools.restart", name = "enabled", havingValue = "true", matchIfMissing = true)
    LgtmStackContainer lgtmContainer(LgtmDevServiceProperties properties) {
        return new LgtmStackContainer(DockerImageName.parse(properties.getImageName())
                .asCompatibleSubstituteFor("grafana/otel-lgtm"))
                .withStartupTimeout(Duration.ofMinutes(2))
                .withReuse(properties.isReusable());
    }

    @Bean
    @ServiceConnection
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "spring.devtools.restart", name = "enabled", havingValue = "false")
    LgtmStackContainer lgtmContainerNoRestartScope(LgtmDevServiceProperties properties) {
        return new LgtmStackContainer(DockerImageName.parse(properties.getImageName())
                .asCompatibleSubstituteFor("grafana/otel-lgtm"))
                .withStartupTimeout(Duration.ofMinutes(2))
                .withReuse(properties.isReusable());
    }


}
