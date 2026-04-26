package io.arconia.observation.opentelemetry.autoconfigure;

import io.opentelemetry.semconv.SchemaUrls;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.arconia.observation.autoconfigure.ObservationProperties;
import io.arconia.observation.conventions.ObservationConventionsProvider;
import io.arconia.opentelemetry.autoconfigure.resource.OpenTelemetryResourceBuilderCustomizer;

/**
 * Auto-configuration for OpenTelemetry Semantic Conventions.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = ObservationProperties.CONFIG_PREFIX, name = "conventions.type", havingValue = "opentelemetry", matchIfMissing = true)
@EnableConfigurationProperties(OpenTelemetryConventionsProperties.class)
public final class OpenTelemetryConventionsAutoConfiguration {

    @Bean
    ObservationConventionsProvider openTelemetryConventionsProvider() {
        return () -> "opentelemetry";
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(OpenTelemetryResourceBuilderCustomizer.class)
    static final class OpenTelemetryResourceConfiguration {

        @Bean
        OpenTelemetryResourceBuilderCustomizer conventionsCustomizer() {
            return builder -> builder.setSchemaUrl(SchemaUrls.V1_40_0);
        }

    }

}
