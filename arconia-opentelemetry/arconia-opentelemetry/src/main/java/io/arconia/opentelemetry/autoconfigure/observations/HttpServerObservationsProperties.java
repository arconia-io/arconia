package io.arconia.opentelemetry.autoconfigure.observations;

import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the observations of HTTP server requests.
 */
@ConfigurationProperties(prefix = HttpServerObservationsProperties.CONFIG_PREFIX)
public class HttpServerObservationsProperties {

    public static final String CONFIG_PREFIX = "arconia.observations.http.server";

    /**
     * Comma-separated list of HTTP request paths for which no observations are recorded,
     * including those started while handling the request. By default, the health
     * endpoint and the additional paths of its liveness and readiness probes.
     */
    private Set<String> ignorePaths = Set.of("/actuator/health/**", "/livez", "/readyz");

    /**
     * Additional comma-separated list of HTTP request paths for which no observations
     * are recorded, including those started while handling the request.
     */
    private Set<String> additionalIgnorePaths = Set.of();

    public Set<String> getIgnorePaths() {
        return ignorePaths;
    }

    public void setIgnorePaths(Set<String> ignorePaths) {
        this.ignorePaths = ignorePaths;
    }

    public Set<String> getAdditionalIgnorePaths() {
        return additionalIgnorePaths;
    }

    public void setAdditionalIgnorePaths(Set<String> additionalIgnorePaths) {
        this.additionalIgnorePaths = additionalIgnorePaths;
    }

}
