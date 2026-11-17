package io.arconia.opentelemetry.autoconfigure.observations;

import java.util.HashSet;

import io.micrometer.observation.ObservationPredicate;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.server.observation.ServerRequestObservationContext;

/**
 * Auto-configuration for the observations of HTTP server requests. It applies whether
 * or not OpenTelemetry is enabled, since it acts on the Micrometer observations.
 */
@AutoConfiguration
@ConditionalOnClass({ HttpServletRequest.class, ServerRequestObservationContext.class })
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(HttpServerObservationsProperties.class)
public final class HttpServerObservationsAutoConfiguration {

    @Bean
    ObservationPredicate ignorePathsObservationPredicate(HttpServerObservationsProperties properties) {
        var ignorePaths = new HashSet<>(properties.getIgnorePaths());
        ignorePaths.addAll(properties.getAdditionalIgnorePaths());
        return new IgnorePathsObservationPredicate(ignorePaths);
    }

}
