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

    private UrlQuerySanitizer() {}

    /**
     * Returns the query string with the values of the given sensitive parameters replaced by {@value #REDACTED},
     * matching parameter keys case-sensitively. The query is scanned in place and returned as it is when
     * nothing needs redacting, which is the case for most requests: this runs on every HTTP request with a query.
     */
    static String sanitize(String query, Collection<String> sensitiveParameters) {
        if (query.isEmpty() || sensitiveParameters.isEmpty()) {
            return query;
        }
        StringBuilder sanitized = null;
        int copied = 0;
        int start = 0;
        while (start <= query.length()) {
            int end = query.indexOf('&', start);
            if (end < 0) {
                end = query.length();
            }
            int separator = indexOf(query, '=', start, end);
            if (separator >= 0 && isSensitive(query, start, separator, sensitiveParameters)) {
                if (sanitized == null) {
                    sanitized = new StringBuilder(query.length());
                }
                sanitized.append(query, copied, separator + 1).append(REDACTED);
                copied = end;
            }
            start = end + 1;
        }
        if (sanitized == null) {
            return query;
        }
        return sanitized.append(query, copied, query.length()).toString();
    }

    /**
     * Whether the parameter key between the given positions is one of the sensitive parameters,
     * compared in place without extracting it.
     */
    private static boolean isSensitive(String query, int keyStart, int keyEnd, Collection<String> sensitiveParameters) {
        int keyLength = keyEnd - keyStart;
        for (String sensitiveParameter : sensitiveParameters) {
            if (sensitiveParameter.length() == keyLength && query.regionMatches(keyStart, sensitiveParameter, 0, keyLength)) {
                return true;
            }
        }
        return false;
    }

    private static int indexOf(String text, char character, int start, int end) {
        int index = text.indexOf(character, start);
        return index >= 0 && index < end ? index : -1;
    }

}
