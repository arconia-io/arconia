package io.arconia.observation.opentelemetry.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.micrometer.metrics.MaximumAllowableTagsMeterFilter;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.client.observation.ClientRequestObservationConvention;
import org.springframework.http.server.observation.OpenTelemetryServerRequestObservationConvention;
import org.springframework.http.server.observation.ServerRequestObservationConvention;

import io.arconia.observation.opentelemetry.instrumentation.http.ExtendedOpenTelemetryServerRequestObservationConvention;
import io.arconia.observation.opentelemetry.instrumentation.http.OpenTelemetryClientRequestObservationConvention;
import io.arconia.observation.opentelemetry.instrumentation.http.OpenTelemetryHttpSpanStatusObservationHandler;

/**
 * Auto-configuration for OpenTelemetry Semantic Conventions for HTTP.
 *
 * @see <a href="https://opentelemetry.io/docs/specs/semconv/http/">OpenTelemetry Semantic Conventions for HTTP</a>
 */
@AutoConfiguration(afterName = {
        "org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration"
}, beforeName = {
        "org.springframework.boot.webmvc.autoconfigure.WebMvcObservationAutoConfiguration",
        "org.springframework.boot.restclient.autoconfigure.RestClientObservationAutoConfiguration",
        "org.springframework.boot.restclient.autoconfigure.RestTemplateObservationAutoConfiguration"
})
@ConditionalOnClass(OpenTelemetryServerRequestObservationConvention.class)
@ConditionalOnBooleanProperty(prefix = OpenTelemetryConventionsProperties.CONFIG_PREFIX, value = "http.enabled", matchIfMissing = true)
@EnableConfigurationProperties(OpenTelemetryConventionsProperties.class)
public final class HttpConventionsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "io.micrometer.tracing.otel.bridge.OtelSpan")
    OpenTelemetryHttpSpanStatusObservationHandler openTelemetryHttpSpanStatusObservationHandler() {
        return new OpenTelemetryHttpSpanStatusObservationHandler();
    }

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

        /**
         * Spring Boot limits the values of the {@code uri} tag only for the metric named after
         * {@code management.observations.http.client.requests.name}, so the same limit is applied
         * to the metric the conventions define.
         */
        @Configuration(proxyBeanMethods = false)
        @ConditionalOnClass(MetricsProperties.class)
        @ConditionalOnBean(MeterRegistry.class)
        @EnableConfigurationProperties(MetricsProperties.class)
        static final class HttpClientMetricsConfiguration {

            @Bean
            @Order(0)
            MaximumAllowableTagsMeterFilter openTelemetryHttpClientUriTagFilter(MetricsProperties metricsProperties) {
                return new MaximumAllowableTagsMeterFilter(OpenTelemetryClientRequestObservationConvention.METRIC_NAME,
                        "uri", metricsProperties.getWeb().getClient().getMaxUriTags(), "Are you using 'uriVariables'?");
            }

        }

    }

}
