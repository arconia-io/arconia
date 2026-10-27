package io.arconia.observation.opentelemetry.instrumentation.http;

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

import io.arconia.observation.opentelemetry.instrumentation.error.OpenTelemetryErrorTypes;

/**
 * Extends OpenTelemetryServerRequestObservationConvention to provide the missing OpenTelemetry-compatible observation for HTTP requests.
 */
public class ExtendedOpenTelemetryServerRequestObservationConvention extends OpenTelemetryServerRequestObservationConvention {

    private static final String ROOT_ROUTE = "/";

    private final OpenTelemetryHttpOptions options;

    public ExtendedOpenTelemetryServerRequestObservationConvention(OpenTelemetryHttpOptions options) {
        this.options = options;
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

    /**
     * Records each attribute only when its value is available, as the conventions require,
     * instead of the placeholder values used by Spring Framework.
     */
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
                return keyValues.and(NetworkAttributes.NETWORK_PROTOCOL_VERSION.getKey(), protocol.substring(5));
            }
        }
        return keyValues;
    }

    private KeyValues urlScheme(KeyValues keyValues, ServerRequestObservationContext context) {
        if (context.getCarrier() != null) {
            return keyValues.and(UrlAttributes.URL_SCHEME.getKey(), context.getCarrier().getScheme());
        }
        return keyValues;
    }

    /**
     * Reports the response status code as the error type when the response signals a server error,
     * also when an exception was thrown, otherwise the canonical class name of the error, if any.
     */
    @Override
    protected KeyValue exception(ServerRequestObservationContext context) {
        if (context.getResponse() != null) {
            int status = context.getResponse().getStatus();
            if (status >= 500 && status < 600) {
                return KeyValue.of(ErrorAttributes.ERROR_TYPE.getKey(), String.valueOf(status));
            }
        }
        if (context.getError() != null) {
            return KeyValue.of(ErrorAttributes.ERROR_TYPE.getKey(), OpenTelemetryErrorTypes.of(context.getError()));
        }
        return super.exception(context);
    }

    /**
     * Treats {@code CONNECT} as a known method, which the conventions list but Spring's {@code HttpMethod} doesn't.
     */
    @Override
    protected @Nullable String getMethodValue(ServerRequestObservationContext context) {
        if (context.getCarrier() != null && HttpAttributes.HttpRequestMethodValues.CONNECT.equals(context.getCarrier().getMethod())) {
            return HttpAttributes.HttpRequestMethodValues.CONNECT;
        }
        return super.getMethodValue(context);
    }

    private KeyValues urlPath(KeyValues keyValues, ServerRequestObservationContext context) {
        if (context.getCarrier() != null) {
            return keyValues.and(UrlAttributes.URL_PATH.getKey(), context.getCarrier().getRequestURI());
        }
        return keyValues;
    }

    /**
     * Records the original method only when it differs from {@code http.request.method},
     * that is when the method is not a known one and is reported as {@code _OTHER}.
     */
    private KeyValues methodOriginal(KeyValues keyValues, ServerRequestObservationContext context) {
        if (context.getCarrier() != null && getMethodValue(context) == null) {
            return keyValues.and(HttpAttributes.HTTP_REQUEST_METHOD_ORIGINAL.getKey(), context.getCarrier().getMethod());
        }
        return keyValues;
    }

    // HIGH CARDINALITY

    @Override
    public KeyValues getHighCardinalityKeyValues(ServerRequestObservationContext context) {
        // Make sure that KeyValues entries are already sorted by key name for better performance
        var keyValues = urlPath(KeyValues.empty(), context);
        keyValues = methodOriginal(keyValues, context);
        keyValues = clientAddress(keyValues, context);
        keyValues = serverAddress(keyValues, context);
        keyValues = serverPort(keyValues, context);
        if (options.isCaptureUrlQuery()) {
            keyValues = urlQuery(keyValues, context);
        }
        keyValues = userAgent(keyValues, context);
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
                return keyValues.and(UrlAttributes.URL_QUERY.getKey(), UrlQuerySanitizer.sanitize(queryString, options.getSensitiveQueryParameters()));
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

}
