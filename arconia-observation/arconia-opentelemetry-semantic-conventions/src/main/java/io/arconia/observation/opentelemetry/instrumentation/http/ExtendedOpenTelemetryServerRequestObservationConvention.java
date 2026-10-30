package io.arconia.observation.opentelemetry.instrumentation.http;

import java.util.List;
import java.util.ArrayList;
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

import io.arconia.observation.opentelemetry.instrumentation.error.OpenTelemetryErrorTypes;

/**
 * Extends OpenTelemetryServerRequestObservationConvention to provide the missing OpenTelemetry-compatible observation for HTTP requests.
 */
public final class ExtendedOpenTelemetryServerRequestObservationConvention extends OpenTelemetryServerRequestObservationConvention {

    private static final String ROOT_ROUTE = "/";

    /**
     * The methods the conventions define as known by default, missing from Spring's {@code HttpMethod}.
     * The {@code QUERY} value is still in development, so the stable semantic conventions library has no constant for it.
     */
    private static final Set<String> ADDITIONAL_KNOWN_METHODS = Set.of(HttpAttributes.HttpRequestMethodValues.CONNECT, "QUERY");

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
        List<KeyValue> keyValues = new ArrayList<>(7);
        keyValues.add(exception(context));
        keyValues.add(method(context));
        keyValues.add(outcome(context));
        statusCode(keyValues, context);
        route(keyValues, context);
        networkProtocolVersion(keyValues, context);
        urlScheme(keyValues, context);
        return KeyValues.of(keyValues);
    }

    private void statusCode(List<KeyValue> keyValues, ServerRequestObservationContext context) {
        if (context.getResponse() != null) {
            keyValues.add(KeyValue.of(HttpAttributes.HTTP_RESPONSE_STATUS_CODE.getKey(), Integer.toString(context.getResponse().getStatus())));
        }
    }

    private void route(List<KeyValue> keyValues, ServerRequestObservationContext context) {
        String pattern = context.getPathPattern();
        if (context.getCarrier() != null && pattern != null) {
            keyValues.add(KeyValue.of(HttpAttributes.HTTP_ROUTE.getKey(), pattern.isEmpty() ? ROOT_ROUTE : pattern));
        }
    }

    private void networkProtocolVersion(List<KeyValue> keyValues, ServerRequestObservationContext context) {
        if (context.getCarrier() != null) {
            String protocol = context.getCarrier().getProtocol();
            if (protocol != null && protocol.startsWith("HTTP/")) {
                keyValues.add(KeyValue.of(NetworkAttributes.NETWORK_PROTOCOL_VERSION.getKey(), toProtocolVersion(protocol.substring(5))));
            }
        }
    }

    /**
     * Reports HTTP/2 and HTTP/3 as {@code 2} and {@code 3}, as in the conventions, since servlet containers
     * such as Tomcat report them as {@code HTTP/2.0} and {@code HTTP/3.0}.
     */
    private static String toProtocolVersion(String version) {
        return switch (version) {
            case "2.0" -> "2";
            case "3.0" -> "3";
            default -> version;
        };
    }

    private void urlScheme(List<KeyValue> keyValues, ServerRequestObservationContext context) {
        if (context.getCarrier() != null) {
            keyValues.add(KeyValue.of(UrlAttributes.URL_SCHEME.getKey(), context.getCarrier().getScheme()));
        }
    }

    /**
     * Reports the response status code as the error type when the response signals a server error, also when
     * an exception was thrown, and no error type for any other response. Spring MVC hands the observation the
     * exceptions it already handled, such as a validation failure answered with a 400 status: the request
     * completed with a status code, which is all the conventions want recorded for it. The class name of the
     * error is reported only when the request failed before a response was produced.
     */
    @Override
    protected KeyValue exception(ServerRequestObservationContext context) {
        if (context.getResponse() != null) {
            int status = context.getResponse().getStatus();
            return status >= 500 && status < 600
                    ? KeyValue.of(ErrorAttributes.ERROR_TYPE.getKey(), String.valueOf(status))
                    : KeyValue.of(ErrorAttributes.ERROR_TYPE.getKey(), KeyValue.NONE_VALUE);
        }
        if (context.getError() != null) {
            return KeyValue.of(ErrorAttributes.ERROR_TYPE.getKey(), OpenTelemetryErrorTypes.of(context.getError()));
        }
        return super.exception(context);
    }

    /**
     * Treats {@code CONNECT} and {@code QUERY} as known methods, which the conventions define as known by default
     * but Spring's {@code HttpMethod} doesn't list.
     */
    @Override
    protected @Nullable String getMethodValue(ServerRequestObservationContext context) {
        if (context.getCarrier() != null && ADDITIONAL_KNOWN_METHODS.contains(context.getCarrier().getMethod())) {
            return context.getCarrier().getMethod();
        }
        return super.getMethodValue(context);
    }

    private void urlPath(List<KeyValue> keyValues, ServerRequestObservationContext context) {
        if (context.getCarrier() != null) {
            keyValues.add(KeyValue.of(UrlAttributes.URL_PATH.getKey(), context.getCarrier().getRequestURI()));
        }
    }

    /**
     * Records the original method only when it differs from {@code http.request.method},
     * that is when the method is not a known one and is reported as {@code _OTHER}.
     */
    private void methodOriginal(List<KeyValue> keyValues, ServerRequestObservationContext context) {
        if (context.getCarrier() != null && getMethodValue(context) == null) {
            keyValues.add(KeyValue.of(HttpAttributes.HTTP_REQUEST_METHOD_ORIGINAL.getKey(), context.getCarrier().getMethod()));
        }
    }

    // HIGH CARDINALITY

    @Override
    public KeyValues getHighCardinalityKeyValues(ServerRequestObservationContext context) {
        List<KeyValue> keyValues = new ArrayList<>(7);
        urlPath(keyValues, context);
        methodOriginal(keyValues, context);
        clientAddress(keyValues, context);
        serverAddress(keyValues, context);
        serverPort(keyValues, context);
        if (options.isCaptureUrlQuery()) {
            urlQuery(keyValues, context);
        }
        userAgent(keyValues, context);
        return KeyValues.of(keyValues);
    }

    private void clientAddress(List<KeyValue> keyValues, ServerRequestObservationContext context) {
        if (context.getCarrier() != null) {
            keyValues.add(KeyValue.of(ClientAttributes.CLIENT_ADDRESS.getKey(), context.getCarrier().getRemoteAddr()));
        }
    }

    private void serverAddress(List<KeyValue> keyValues, ServerRequestObservationContext context) {
        if (context.getCarrier() != null) {
            keyValues.add(KeyValue.of(ServerAttributes.SERVER_ADDRESS.getKey(), context.getCarrier().getServerName()));
        }
    }

    private void serverPort(List<KeyValue> keyValues, ServerRequestObservationContext context) {
        if (context.getCarrier() != null) {
            keyValues.add(KeyValue.of(ServerAttributes.SERVER_PORT.getKey(), Integer.toString(context.getCarrier().getServerPort())));
        }
    }

    private void urlQuery(List<KeyValue> keyValues, ServerRequestObservationContext context) {
        if (context.getCarrier() != null) {
            String queryString = context.getCarrier().getQueryString();
            if (queryString != null) {
                keyValues.add(KeyValue.of(UrlAttributes.URL_QUERY.getKey(), UrlQuerySanitizer.sanitize(queryString, options.getSensitiveQueryParameters())));
            }
        }
    }

    private void userAgent(List<KeyValue> keyValues, ServerRequestObservationContext context) {
        if (context.getCarrier() != null) {
            String userAgent = context.getCarrier().getHeader("User-Agent");
            if (userAgent != null) {
                keyValues.add(KeyValue.of(UserAgentAttributes.USER_AGENT_ORIGINAL.getKey(), userAgent));
            }
        }
    }

}
