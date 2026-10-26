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

/**
 * Extends OpenTelemetryServerRequestObservationConvention to provide the missing OpenTelemetry-compatible observation for HTTP requests.
 */
public class ExtendedOpenTelemetryServerRequestObservationConvention extends OpenTelemetryServerRequestObservationConvention {

    private static final KeyValue NETWORK_PROTOCOL_VERSION_UNKNOWN = KeyValue.of(NetworkAttributes.NETWORK_PROTOCOL_VERSION.getKey(), "UNKNOWN");

    private final OpenTelemetryHttpOptions options;

    public ExtendedOpenTelemetryServerRequestObservationConvention(OpenTelemetryHttpOptions options) {
        this.options = options;
    }

    // LOW CARDINALITY

    @Override
    public KeyValues getLowCardinalityKeyValues(ServerRequestObservationContext context) {
        // Make sure that KeyValues entries are already sorted by key name for better performance
        return KeyValues.of(exception(context), method(context), status(context), pathTemplate(context), networkProtocolVersion(context), outcome(context), scheme(context));
    }

    private KeyValue networkProtocolVersion(ServerRequestObservationContext context) {
        if (context.getCarrier() != null) {
            String protocol = context.getCarrier().getProtocol();
            if (protocol != null && protocol.startsWith("HTTP/")) {
                return KeyValue.of(NetworkAttributes.NETWORK_PROTOCOL_VERSION.getKey(), protocol.substring(5));
            }
        }
        return NETWORK_PROTOCOL_VERSION_UNKNOWN;
    }

    /**
     * Reports the response status code as the error type when the response signals a server error
     * without an exception being thrown, as the conventions require.
     */
    @Override
    protected KeyValue exception(ServerRequestObservationContext context) {
        if (context.getError() == null && context.getResponse() != null) {
            int status = context.getResponse().getStatus();
            if (status >= 500 && status < 600) {
                return KeyValue.of(ErrorAttributes.ERROR_TYPE.getKey(), String.valueOf(status));
            }
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
        var keyValues = KeyValues.of(httpUrl(context));
        keyValues = methodOriginal(keyValues, context);
        keyValues = clientAddress(keyValues, context);
        keyValues = serverAddress(keyValues, context);
        keyValues = serverPort(keyValues, context);
        if (options.isIncludeUrlQuery()) {
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
                return keyValues.and(UrlAttributes.URL_QUERY.getKey(), queryString);
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
