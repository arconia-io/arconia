package io.arconia.observation.opentelemetry.instrumentation.http;

import java.util.Collection;
import java.util.StringJoiner;

/**
 * Redacts the values of sensitive parameters in a URL query string, preserving the parameter keys,
 * as the OpenTelemetry Semantic Conventions recommend for the {@code url.query} and {@code url.full} attributes.
 *
 * @see <a href="https://opentelemetry.io/docs/specs/semconv/registry/attributes/url/">OpenTelemetry Semantic Conventions for URL</a>
 */
final class UrlQuerySanitizer {

    static final String REDACTED = "REDACTED";

    private UrlQuerySanitizer() {}

    /**
     * Returns the query string with the values of the given sensitive parameters replaced by {@value #REDACTED},
     * matching parameter keys case-sensitively.
     */
    static String sanitize(String query, Collection<String> sensitiveParameters) {
        if (sensitiveParameters.isEmpty()) {
            return query;
        }
        StringJoiner sanitized = new StringJoiner("&");
        for (String parameter : query.split("&", -1)) {
            int separator = parameter.indexOf('=');
            if (separator >= 0 && sensitiveParameters.contains(parameter.substring(0, separator))) {
                sanitized.add(parameter.substring(0, separator + 1) + REDACTED);
            } else {
                sanitized.add(parameter);
            }
        }
        return sanitized.toString();
    }

}
