package io.arconia.observation.opentelemetry.instrumentation.http;

import java.util.Collection;

/**
 * Redacts the values of sensitive parameters in a URL query string, preserving the parameter keys,
 * as the OpenTelemetry Semantic Conventions recommend for the {@code url.query} and {@code url.full} attributes.
 *
 * @see <a href="https://opentelemetry.io/docs/specs/semconv/registry/attributes/url/">OpenTelemetry Semantic Conventions for URL</a>
 */
final class UrlQuerySanitizer {

    static final String REDACTED = "REDACTED";

    private UrlQuerySanitizer() {
    }

    /**
     * Returns the query string with the values of the given sensitive parameters replaced by {@value #REDACTED},
     * matching parameter keys case-sensitively.
     */
    static String sanitize(String query, Collection<String> sensitiveParameters) {
        if (query.isEmpty() || sensitiveParameters.isEmpty()) {
            return query;
        }
        StringBuilder sanitized = new StringBuilder(query.length());
        for (String parameter : query.split("&", -1)) {
            if (!sanitized.isEmpty()) {
                sanitized.append('&');
            }
            int separator = parameter.indexOf('=');
            if (separator >= 0 && sensitiveParameters.contains(parameter.substring(0, separator))) {
                sanitized.append(parameter, 0, separator + 1).append(REDACTED);
            }
            else {
                sanitized.append(parameter);
            }
        }
        return sanitized.toString();
    }

}
