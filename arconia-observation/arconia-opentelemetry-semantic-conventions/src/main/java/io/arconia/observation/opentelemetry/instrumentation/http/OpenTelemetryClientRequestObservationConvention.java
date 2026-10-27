package io.arconia.observation.opentelemetry.instrumentation.http;

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
public class OpenTelemetryClientRequestObservationConvention extends DefaultClientRequestObservationConvention {

    private static final String NAME = "http.client.request.duration";

    private static final String REDACTED_USER_INFO = UrlQuerySanitizer.REDACTED + ":" + UrlQuerySanitizer.REDACTED;

    private static final Set<String> KNOWN_METHODS = Set.of(HttpRequestMethodValues.CONNECT, HttpRequestMethodValues.DELETE,
            HttpRequestMethodValues.GET, HttpRequestMethodValues.HEAD, HttpRequestMethodValues.OPTIONS,
            HttpRequestMethodValues.PATCH, HttpRequestMethodValues.POST, HttpRequestMethodValues.PUT,
            HttpRequestMethodValues.TRACE);

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
        // Make sure that KeyValues entries are already sorted by key name for better performance
        var keyValues = KeyValues.of(exception(context));
        keyValues = method(keyValues, context);
        keyValues = statusCode(keyValues, context);
        keyValues = keyValues.and(outcome(context));
        keyValues = serverAddress(keyValues, context);
        keyValues = serverPort(keyValues, context);
        return keyValues.and(uri(context));
    }

    /**
     * Reports the response status code as the error type when the response signals an error,
     * otherwise the fully qualified class name of the error, if any.
     */
    @Override
    protected KeyValue exception(ClientRequestObservationContext context) {
        Integer status = getStatusCode(context);
        if (status != null && status >= 400) {
            return KeyValue.of(ErrorAttributes.ERROR_TYPE.getKey(), String.valueOf(status));
        }
        if (context.getError() != null) {
            return KeyValue.of(ErrorAttributes.ERROR_TYPE.getKey(), context.getError().getClass().getName());
        }
        return KeyValue.of(ErrorAttributes.ERROR_TYPE.getKey(), KeyValue.NONE_VALUE);
    }

    private KeyValues method(KeyValues keyValues, ClientRequestObservationContext context) {
        if (context.getCarrier() != null) {
            String method = getMethodValue(context);
            return keyValues.and(HttpAttributes.HTTP_REQUEST_METHOD.getKey(), method != null ? method : HttpRequestMethodValues.OTHER);
        }
        return keyValues;
    }

    private KeyValues statusCode(KeyValues keyValues, ClientRequestObservationContext context) {
        Integer status = getStatusCode(context);
        if (status != null) {
            return keyValues.and(HttpAttributes.HTTP_RESPONSE_STATUS_CODE.getKey(), String.valueOf(status));
        }
        return keyValues;
    }

    private KeyValues serverAddress(KeyValues keyValues, ClientRequestObservationContext context) {
        if (context.getCarrier() != null && context.getCarrier().getURI().getHost() != null) {
            return keyValues.and(ServerAttributes.SERVER_ADDRESS.getKey(), context.getCarrier().getURI().getHost());
        }
        return keyValues;
    }

    /**
     * Records the server port, falling back to the default port of the URL scheme when the URL doesn't specify one.
     */
    private KeyValues serverPort(KeyValues keyValues, ClientRequestObservationContext context) {
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
                return keyValues.and(ServerAttributes.SERVER_PORT.getKey(), Integer.toString(port));
            }
        }
        return keyValues;
    }

    // HIGH CARDINALITY

    @Override
    public KeyValues getHighCardinalityKeyValues(ClientRequestObservationContext context) {
        // Make sure that KeyValues entries are already sorted by key name for better performance
        var keyValues = methodOriginal(KeyValues.empty(), context);
        return urlFull(keyValues, context);
    }

    /**
     * Records the original method only when it differs from {@code http.request.method},
     * that is when the method is not a known one and is reported as {@code _OTHER}.
     */
    private KeyValues methodOriginal(KeyValues keyValues, ClientRequestObservationContext context) {
        if (context.getCarrier() != null && getMethodValue(context) == null) {
            return keyValues.and(HttpAttributes.HTTP_REQUEST_METHOD_ORIGINAL.getKey(), context.getCarrier().getMethod().name());
        }
        return keyValues;
    }

    private KeyValues urlFull(KeyValues keyValues, ClientRequestObservationContext context) {
        if (context.getCarrier() != null) {
            return keyValues.and(UrlAttributes.URL_FULL.getKey(), sanitizeUrl(context.getCarrier().getURI()));
        }
        return keyValues;
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
