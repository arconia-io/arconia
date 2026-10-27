package io.arconia.observation.opentelemetry.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.observation.ClientRequestObservationConvention;
import org.springframework.http.server.observation.OpenTelemetryServerRequestObservationConvention;
import org.springframework.http.server.observation.ServerRequestObservationConvention;

import io.arconia.observation.opentelemetry.instrumentation.http.ExtendedOpenTelemetryServerRequestObservationConvention;
import io.arconia.observation.opentelemetry.instrumentation.http.OpenTelemetryClientRequestObservationConvention;

/**
 * Auto-configuration for OpenTelemetry Semantic Conventions for HTTP.
 *
 * @see <a href="https://opentelemetry.io/docs/specs/semconv/http/">OpenTelemetry Semantic Conventions for HTTP</a>
 */
@AutoConfiguration(beforeName = {
        "org.springframework.boot.webmvc.autoconfigure.WebMvcObservationAutoConfiguration",
        "org.springframework.boot.restclient.autoconfigure.RestClientObservationAutoConfiguration",
        "org.springframework.boot.restclient.autoconfigure.RestTemplateObservationAutoConfiguration"
})
@ConditionalOnClass(OpenTelemetryServerRequestObservationConvention.class)
@ConditionalOnBooleanProperty(prefix = OpenTelemetryConventionsProperties.CONFIG_PREFIX, value = "http.enabled", matchIfMissing = true)
@EnableConfigurationProperties(OpenTelemetryConventionsProperties.class)
public final class HttpConventionsAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    static final class HttpServerConventionsConfiguration {

        @Bean
        @ConditionalOnMissingBean(ServerRequestObservationConvention.class)
        ExtendedOpenTelemetryServerRequestObservationConvention openTelemetryServerRequestObservationConvention(OpenTelemetryConventionsProperties properties) {
            return new ExtendedOpenTelemetryServerRequestObservationConvention(properties.getHttp());
        }

    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.boot.restclient.observation.ObservationRestClientCustomizer")
    static final class HttpClientConventionsConfiguration {

        @Bean
        @ConditionalOnMissingBean(ClientRequestObservationConvention.class)
        OpenTelemetryClientRequestObservationConvention openTelemetryClientRequestObservationConvention(OpenTelemetryConventionsProperties properties) {
            return new OpenTelemetryClientRequestObservationConvention(properties.getHttp());
        }

    }

}
