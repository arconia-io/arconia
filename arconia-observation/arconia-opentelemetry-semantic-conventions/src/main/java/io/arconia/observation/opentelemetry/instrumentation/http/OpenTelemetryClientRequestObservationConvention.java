package io.arconia.observation.opentelemetry.instrumentation.http;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.net.URI;
import java.util.Set;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.opentelemetry.semconv.ErrorAttributes;
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.HttpAttributes.HttpRequestMethodValues;
import io.opentelemetry.semconv.ServerAttributes;
import io.opentelemetry.semconv.UrlAttributes;

import org.jspecify.annotations.Nullable;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.observation.ClientRequestObservationContext;
import org.springframework.http.client.observation.DefaultClientRequestObservationConvention;
import org.springframework.web.client.ResourceAccessException;

import io.arconia.observation.opentelemetry.instrumentation.error.OpenTelemetryErrorTypes;

/**
 * Observation convention for HTTP client requests following the OpenTelemetry Semantic Conventions,
 * applied to {@code RestClient} and {@code RestTemplate}.
 * <p>
 * The {@code outcome} and {@code uri} attributes from Spring Framework are kept, since the OpenTelemetry
 * Semantic Conventions don't define a stable equivalent for them.
 *
 * @see <a href="https://opentelemetry.io/docs/specs/semconv/http/http-spans/#http-client-span">HTTP Client Spans</a>
 * @see <a href="https://opentelemetry.io/docs/specs/semconv/http/http-metrics/#metric-httpclientrequestduration">HTTP Client Metrics</a>
 */
public final class OpenTelemetryClientRequestObservationConvention extends DefaultClientRequestObservationConvention {

    private static final String NAME = "http.client.request.duration";

    private static final String REDACTED_USER_INFO = UrlQuerySanitizer.REDACTED + ":" + UrlQuerySanitizer.REDACTED;

    /**
     * The methods the conventions define as known by default. The {@code QUERY} value is still in development,
     * so the stable semantic conventions library has no constant for it.
     */
    private static final Set<String> KNOWN_METHODS = Set.of(HttpRequestMethodValues.CONNECT, HttpRequestMethodValues.DELETE,
            HttpRequestMethodValues.GET, HttpRequestMethodValues.HEAD, HttpRequestMethodValues.OPTIONS,
            HttpRequestMethodValues.PATCH, HttpRequestMethodValues.POST, HttpRequestMethodValues.PUT,
            HttpRequestMethodValues.TRACE, "QUERY");

    private final OpenTelemetryHttpOptions options;

    public OpenTelemetryClientRequestObservationConvention(OpenTelemetryHttpOptions options) {
        super(NAME);
        this.options = options;
    }

    /**
     * Names the span after the request method, or {@code HTTP} when the method is not a known one.
     */
    @Override
    public @Nullable String getContextualName(ClientRequestObservationContext context) {
        if (context.getCarrier() == null) {
            return null;
        }
        String method = getMethodValue(context);
        return method != null ? method : "HTTP";
    }

    // LOW CARDINALITY

    /**
     * Records each attribute only when its value is available, as the conventions require.
     */
    @Override
    public KeyValues getLowCardinalityKeyValues(ClientRequestObservationContext context) {
        List<KeyValue> keyValues = new ArrayList<>(7);
        keyValues.add(exception(context));
        method(keyValues, context);
        statusCode(keyValues, context);
        keyValues.add(outcome(context));
        serverAddress(keyValues, context);
        serverPort(keyValues, context);
        keyValues.add(uri(context));
        return KeyValues.of(keyValues);
    }

    /**
     * Reports the response status code as the error type when the response signals an error,
     * otherwise the class name of the error, if any. The {@link ResourceAccessException}
     * that Spring Framework uses to wrap any I/O error is unwrapped, since it doesn't help classify the failure,
     * as the conventions allow.
     */
    @Override
    protected KeyValue exception(ClientRequestObservationContext context) {
        Integer status = getStatusCode(context);
        if (status != null && status >= 400) {
            return KeyValue.of(ErrorAttributes.ERROR_TYPE.getKey(), String.valueOf(status));
        }
        Throwable error = context.getError();
        if (error != null) {
            if (error instanceof ResourceAccessException && error.getCause() != null) {
                error = error.getCause();
            }
            return KeyValue.of(ErrorAttributes.ERROR_TYPE.getKey(), OpenTelemetryErrorTypes.of(error));
        }
        return KeyValue.of(ErrorAttributes.ERROR_TYPE.getKey(), KeyValue.NONE_VALUE);
    }

    private void method(List<KeyValue> keyValues, ClientRequestObservationContext context) {
        if (context.getCarrier() != null) {
            String method = getMethodValue(context);
            keyValues.add(KeyValue.of(HttpAttributes.HTTP_REQUEST_METHOD.getKey(), method != null ? method : HttpRequestMethodValues.OTHER));
        }
    }

    private void statusCode(List<KeyValue> keyValues, ClientRequestObservationContext context) {
        Integer status = getStatusCode(context);
        if (status != null) {
            keyValues.add(KeyValue.of(HttpAttributes.HTTP_RESPONSE_STATUS_CODE.getKey(), String.valueOf(status)));
        }
    }

    private void serverAddress(List<KeyValue> keyValues, ClientRequestObservationContext context) {
        if (context.getCarrier() != null && context.getCarrier().getURI().getHost() != null) {
            keyValues.add(KeyValue.of(ServerAttributes.SERVER_ADDRESS.getKey(), context.getCarrier().getURI().getHost()));
        }
    }

    /**
     * Records the server port, falling back to the default port of the URL scheme when the URL doesn't specify one.
     */
    private void serverPort(List<KeyValue> keyValues, ClientRequestObservationContext context) {
        if (context.getCarrier() != null) {
            URI uri = context.getCarrier().getURI();
            int port = uri.getPort();
            if (port == -1) {
                port = switch (uri.getScheme() != null ? uri.getScheme() : "") {
                    case "http" -> 80;
                    case "https" -> 443;
                    default -> -1;
                };
            }
            if (port != -1) {
                keyValues.add(KeyValue.of(ServerAttributes.SERVER_PORT.getKey(), Integer.toString(port)));
            }
        }
    }

    // HIGH CARDINALITY

    @Override
    public KeyValues getHighCardinalityKeyValues(ClientRequestObservationContext context) {
        List<KeyValue> keyValues = new ArrayList<>(2);
        methodOriginal(keyValues, context);
        urlFull(keyValues, context);
        return KeyValues.of(keyValues);
    }

    /**
     * Records the original method only when it differs from {@code http.request.method},
     * that is when the method is not a known one and is reported as {@code _OTHER}.
     */
    private void methodOriginal(List<KeyValue> keyValues, ClientRequestObservationContext context) {
        if (context.getCarrier() != null && getMethodValue(context) == null) {
            keyValues.add(KeyValue.of(HttpAttributes.HTTP_REQUEST_METHOD_ORIGINAL.getKey(), context.getCarrier().getMethod().name()));
        }
    }

    private void urlFull(List<KeyValue> keyValues, ClientRequestObservationContext context) {
        if (context.getCarrier() != null) {
            keyValues.add(KeyValue.of(UrlAttributes.URL_FULL.getKey(), sanitizeUrl(context.getCarrier().getURI())));
        }
    }

    /**
     * Returns the URL with the credentials and the values of sensitive query parameters redacted, as the conventions require.
     */
    private String sanitizeUrl(URI uri) {
        String url = uri.toASCIIString();
        if (uri.getRawUserInfo() == null && uri.getRawQuery() == null) {
            return url;
        }
        URI asciiUri = URI.create(url);
        StringBuilder sanitized = new StringBuilder(url.length());
        if (asciiUri.getScheme() != null) {
            sanitized.append(asciiUri.getScheme()).append(':');
        }
        String authority = asciiUri.getRawAuthority();
        if (authority != null) {
            sanitized.append("//");
            if (asciiUri.getRawUserInfo() != null) {
                sanitized.append(REDACTED_USER_INFO).append('@').append(authority, authority.lastIndexOf('@') + 1, authority.length());
            }
            else {
                sanitized.append(authority);
            }
        }
        if (asciiUri.getRawPath() != null) {
            sanitized.append(asciiUri.getRawPath());
        }
        if (asciiUri.getRawQuery() != null) {
            sanitized.append('?').append(UrlQuerySanitizer.sanitize(asciiUri.getRawQuery(), options.getSensitiveQueryParameters()));
        }
        if (asciiUri.getRawFragment() != null) {
            sanitized.append('#').append(asciiUri.getRawFragment());
        }
        return sanitized.toString();
    }

    /**
     * Returns the request method if it's a known one, otherwise {@code null}.
     */
    protected @Nullable String getMethodValue(ClientRequestObservationContext context) {
        if (context.getCarrier() != null) {
            String method = context.getCarrier().getMethod().name();
            if (KNOWN_METHODS.contains(method)) {
                return method;
            }
        }
        return null;
    }

    private static @Nullable Integer getStatusCode(ClientRequestObservationContext context) {
        ClientHttpResponse response = context.getResponse();
        if (response != null) {
            try {
                return response.getStatusCode().value();
            }
            catch (IOException ex) {
                // The status code is not available
            }
        }
        return null;
    }

}
