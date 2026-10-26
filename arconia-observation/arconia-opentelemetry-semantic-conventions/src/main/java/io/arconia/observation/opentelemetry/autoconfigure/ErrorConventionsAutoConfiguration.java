package io.arconia.observation.opentelemetry.autoconfigure;

import io.micrometer.observation.ObservationFilter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import io.arconia.observation.opentelemetry.instrumentation.error.OpenTelemetryErrorTypeObservationFilter;

/**
 * Auto-configuration for OpenTelemetry Semantic Conventions for errors.
 *
 * @see <a href="https://opentelemetry.io/docs/specs/semconv/registry/attributes/error/">OpenTelemetry Semantic Conventions for Errors</a>
 */
@AutoConfiguration
@ConditionalOnClass(ObservationFilter.class)
@ConditionalOnBooleanProperty(prefix = OpenTelemetryConventionsProperties.CONFIG_PREFIX, value = "error.enabled", matchIfMissing = true)
@EnableConfigurationProperties(OpenTelemetryConventionsProperties.class)
public final class ErrorConventionsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(OpenTelemetryErrorTypeObservationFilter.class)
    OpenTelemetryErrorTypeObservationFilter openTelemetryErrorTypeObservationFilter() {
        return new OpenTelemetryErrorTypeObservationFilter();
    }

}
