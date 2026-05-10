package io.arconia.observation.opentelemetry.autoconfigure;

import io.opentelemetry.semconv.SchemaUrls;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.arconia.opentelemetry.autoconfigure.resource.OpenTelemetryResourceBuilderCustomizer;

/**
 * Auto-configuration for OpenTelemetry Semantic Conventions.
 */
@AutoConfiguration
@EnableConfigurationProperties(OpenTelemetryConventionsProperties.class)
public final class OpenTelemetryConventionsAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(OpenTelemetryResourceBuilderCustomizer.class)
    static final class OpenTelemetryResourceConfiguration {

        @Bean
        OpenTelemetryResourceBuilderCustomizer conventionsCustomizer() {
            return builder -> builder.setSchemaUrl(SchemaUrls.V1_41_0);
        }

    }

}
