package io.arconia.observation.opentelemetry.instrumentation.http;

import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.opentelemetry.semconv.ClientAttributes;
import io.opentelemetry.semconv.ErrorAttributes;
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.NetworkAttributes;
import io.opentelemetry.semconv.ServerAttributes;
import io.opentelemetry.semconv.UrlAttributes;
import io.opentelemetry.semconv.UserAgentAttributes;

import org.jspecify.annotations.Nullable;
import org.springframework.http.server.observation.OpenTelemetryServerRequestObservationConvention;
import org.springframework.http.server.observation.ServerRequestObservationContext;


/**
 * Provides the missing OpenTelemetry-compatible observation for HTTP requests.
 */
public final class ExtendedOpenTelemetryServerRequestObservationConvention extends OpenTelemetryServerRequestObservationConvention {

    private static final String ROOT_ROUTE = "/";

    /**
     * The methods the conventions define as known by default, missing from Spring's {@code HttpMethod}.
     */
    private static final Set<String> ADDITIONAL_KNOWN_METHODS = Set.of(HttpAttributes.HttpRequestMethodValues.CONNECT, "QUERY");

    private final OpenTelemetryHttpOptions options;

    private final List<String> queryParametersToRedact;

    private final Map<String, String> requestHeaderAttributeKeys;

    private final Map<String, String> responseHeaderAttributeKeys;

    public ExtendedOpenTelemetryServerRequestObservationConvention(OpenTelemetryHttpOptions options) {
        this.options = options;
        this.queryParametersToRedact = options.queryParametersToRedact();
        this.requestHeaderAttributeKeys = options.requestHeaderAttributeKeys();
        this.responseHeaderAttributeKeys = options.responseHeaderAttributeKeys();
    }

    /**
     * Names the span after the root route when the request matched the root pattern,
     * which Spring represents as an empty pattern.
     */
    @Override
    public String getContextualName(ServerRequestObservationContext context) {
        String pattern = context.getPathPattern();
        if (context.getCarrier() != null && pattern != null && pattern.isEmpty()) {
            String method = getMethodValue(context);
            return "%s %s".formatted(method != null ? method : "HTTP", ROOT_ROUTE);
        }
        return super.getContextualName(context);
    }

    // LOW CARDINALITY

    @Override
    public KeyValues getLowCardinalityKeyValues(ServerRequestObservationContext context) {
        var keyValues = KeyValues.of(exception(context), method(context), outcome(context));
        keyValues = statusCode(keyValues, context);
        keyValues = route(keyValues, context);
        keyValues = networkProtocolVersion(keyValues, context);
        keyValues = urlScheme(keyValues, context);
        return keyValues;
    }

    private KeyValues statusCode(KeyValues keyValues, ServerRequestObservationContext context) {
        if (context.getResponse() != null) {
            return keyValues.and(HttpAttributes.HTTP_RESPONSE_STATUS_CODE.getKey(), Integer.toString(context.getResponse().getStatus()));
        }
        return keyValues;
    }

    private KeyValues route(KeyValues keyValues, ServerRequestObservationContext context) {
        String pattern = context.getPathPattern();
        if (context.getCarrier() != null && pattern != null) {
            return keyValues.and(HttpAttributes.HTTP_ROUTE.getKey(), pattern.isEmpty() ? ROOT_ROUTE : pattern);
        }
        return keyValues;
    }

    private KeyValues networkProtocolVersion(KeyValues keyValues, ServerRequestObservationContext context) {
        if (context.getCarrier() != null) {
            String protocol = context.getCarrier().getProtocol();
            if (protocol != null && protocol.startsWith("HTTP/")) {
                return keyValues.and(NetworkAttributes.NETWORK_PROTOCOL_VERSION.getKey(), toProtocolVersion(protocol.substring(5)));
            }
        }
        return keyValues;
    }

    private static String toProtocolVersion(String version) {
        return switch (version) {
            case "2.0" -> "2";
            case "3.0" -> "3";
            default -> version;
        };
    }

    private KeyValues urlScheme(KeyValues keyValues, ServerRequestObservationContext context) {
        if (context.getCarrier() != null) {
            return keyValues.and(UrlAttributes.URL_SCHEME.getKey(), context.getCarrier().getScheme());
        }
        return keyValues;
    }

    @Override
    protected KeyValue exception(ServerRequestObservationContext context) {
        if (context.getResponse() != null) {
            int status = context.getResponse().getStatus();
            return status >= 500 && status < 600
                    ? KeyValue.of(ErrorAttributes.ERROR_TYPE.getKey(), String.valueOf(status))
                    : KeyValue.of(ErrorAttributes.ERROR_TYPE.getKey(), KeyValue.NONE_VALUE);
        }
        if (context.getError() != null) {
            return KeyValue.of(ErrorAttributes.ERROR_TYPE.getKey(), context.getError().getClass().getName());
        }
        return super.exception(context);
    }

    @Override
    protected @Nullable String getMethodValue(ServerRequestObservationContext context) {
        if (context.getCarrier() != null && ADDITIONAL_KNOWN_METHODS.contains(context.getCarrier().getMethod())) {
            return context.getCarrier().getMethod();
        }
        return super.getMethodValue(context);
    }

    private KeyValues urlPath(KeyValues keyValues, ServerRequestObservationContext context) {
        if (context.getCarrier() != null) {
            return keyValues.and(UrlAttributes.URL_PATH.getKey(), context.getCarrier().getRequestURI());
        }
        return keyValues;
    }

    private KeyValues methodOriginal(KeyValues keyValues, ServerRequestObservationContext context) {
        if (context.getCarrier() != null && getMethodValue(context) == null) {
            return keyValues.and(HttpAttributes.HTTP_REQUEST_METHOD_ORIGINAL.getKey(), context.getCarrier().getMethod());
        }
        return keyValues;
    }

    // HIGH CARDINALITY

    @Override
    public KeyValues getHighCardinalityKeyValues(ServerRequestObservationContext context) {
        var keyValues = KeyValues.empty();
        keyValues = urlPath(keyValues, context);
        keyValues = methodOriginal(keyValues, context);
        keyValues = clientAddress(keyValues, context);
        keyValues = serverAddress(keyValues, context);
        keyValues = serverPort(keyValues, context);
        if (options.isCaptureUrlQuery()) {
            keyValues = urlQuery(keyValues, context);
        }
        keyValues = userAgent(keyValues, context);
        keyValues = requestHeaders(keyValues, context);
        keyValues = responseHeaders(keyValues, context);
        return keyValues;
    }

    private KeyValues clientAddress(KeyValues keyValues, ServerRequestObservationContext context) {
        if (context.getCarrier() != null) {
            return keyValues.and(ClientAttributes.CLIENT_ADDRESS.getKey(), context.getCarrier().getRemoteAddr());
        }
        return keyValues;
    }

    private KeyValues serverAddress(KeyValues keyValues, ServerRequestObservationContext context) {
        if (context.getCarrier() != null) {
            return keyValues.and(ServerAttributes.SERVER_ADDRESS.getKey(), context.getCarrier().getServerName());
        }
        return keyValues;
    }

    private KeyValues serverPort(KeyValues keyValues, ServerRequestObservationContext context) {
        if (context.getCarrier() != null) {
            return keyValues.and(ServerAttributes.SERVER_PORT.getKey(), Integer.toString(context.getCarrier().getServerPort()));
        }
        return keyValues;
    }

    private KeyValues urlQuery(KeyValues keyValues, ServerRequestObservationContext context) {
        if (context.getCarrier() != null) {
            String queryString = context.getCarrier().getQueryString();
            if (queryString != null) {
                return keyValues.and(UrlAttributes.URL_QUERY.getKey(), UrlQuerySanitizer.sanitize(queryString, queryParametersToRedact));
            }
        }
        return keyValues;
    }

    private KeyValues userAgent(KeyValues keyValues, ServerRequestObservationContext context) {
        if (context.getCarrier() != null) {
            String userAgent = context.getCarrier().getHeader("User-Agent");
            if (userAgent != null) {
                return keyValues.and(UserAgentAttributes.USER_AGENT_ORIGINAL.getKey(), userAgent);
            }
        }
        return keyValues;
    }

    private KeyValues requestHeaders(KeyValues keyValues, ServerRequestObservationContext context) {
        if (context.getCarrier() == null) {
            return keyValues;
        }
        for (Map.Entry<String, String> header : requestHeaderAttributeKeys.entrySet()) {
            Enumeration<String> values = context.getCarrier().getHeaders(header.getKey());
            if (values != null && values.hasMoreElements()) {
                keyValues = keyValues.and(header.getValue(), String.join(",", Collections.list(values)));
            }
        }
        return keyValues;
    }

    private KeyValues responseHeaders(KeyValues keyValues, ServerRequestObservationContext context) {
        if (context.getResponse() == null) {
            return keyValues;
        }
        for (Map.Entry<String, String> header : responseHeaderAttributeKeys.entrySet()) {
            Collection<String> values = context.getResponse().getHeaders(header.getKey());
            if (!values.isEmpty()) {
                keyValues = keyValues.and(header.getValue(), String.join(",", values));
            }
        }
        return keyValues;
    }

}
