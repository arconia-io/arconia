package io.arconia.observation.opentelemetry.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.server.observation.OpenTelemetryServerRequestObservationConvention;
import org.springframework.http.server.observation.ServerRequestObservationConvention;

import io.arconia.observation.autoconfigure.ObservationProperties;
import io.arconia.observation.opentelemetry.instrumentation.http.ExtendedOpenTelemetryServerRequestObservationConvention;

/**
 * Auto-configuration for OpenTelemetry Semantic Conventions for HTTP.
 *
 * @see <a href="https://opentelemetry.io/docs/specs/semconv/http/">OpenTelemetry Semantic Conventions for HTTP</a>
 */
@AutoConfiguration(
        beforeName = "org.springframework.boot.webmvc.autoconfigure.WebMvcObservationAutoConfiguration"
)
@ConditionalOnClass(OpenTelemetryServerRequestObservationConvention.class)
@ConditionalOnProperty(prefix = ObservationProperties.CONFIG_PREFIX, name = "conventions.type", havingValue = "opentelemetry", matchIfMissing = true)
@ConditionalOnBooleanProperty(prefix = OpenTelemetryConventionsProperties.CONFIG_PREFIX, value = "http.enabled", matchIfMissing = true)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(OpenTelemetryConventionsProperties.class)
public final class HttpConventionsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ServerRequestObservationConvention.class)
    ExtendedOpenTelemetryServerRequestObservationConvention openTelemetryServerRequestObservationConvention(OpenTelemetryConventionsProperties properties) {
        return new ExtendedOpenTelemetryServerRequestObservationConvention(properties.getHttp());
    }

}
