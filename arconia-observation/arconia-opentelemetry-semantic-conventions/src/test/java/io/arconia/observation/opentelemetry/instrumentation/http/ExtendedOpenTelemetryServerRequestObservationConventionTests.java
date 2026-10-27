package io.arconia.observation.opentelemetry.instrumentation.http;

import java.util.List;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.junit.jupiter.api.Test;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ExtendedOpenTelemetryServerRequestObservationConvention}.
 */
class ExtendedOpenTelemetryServerRequestObservationConventionTests {

    private final OpenTelemetryHttpOptions defaultOptions = new OpenTelemetryHttpOptions();

    private final ExtendedOpenTelemetryServerRequestObservationConvention convention =
            new ExtendedOpenTelemetryServerRequestObservationConvention(defaultOptions);

    // LOW CARDINALITY — error.type

    @Test
    void errorTypeIsStatusCodeForServerErrorWithoutException() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(503);
        ServerRequestObservationContext context = new ServerRequestObservationContext(new MockHttpServletRequest(), response);

        KeyValues keyValues = convention.getLowCardinalityKeyValues(context);

        assertThat(keyValues).contains(KeyValue.of("error.type", "503"));
    }

    @Test
    void errorTypeIsStatusCodeForServerErrorWithException() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(500);
        ServerRequestObservationContext context = new ServerRequestObservationContext(new MockHttpServletRequest(), response);
        context.setError(new IllegalStateException("boom"));

        KeyValues keyValues = convention.getLowCardinalityKeyValues(context);

        assertThat(keyValues).contains(KeyValue.of("error.type", "500"));
    }

    @Test
    void errorTypeIsExceptionTypeForExceptionWithoutServerErrorStatus() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        ServerRequestObservationContext context = new ServerRequestObservationContext(new MockHttpServletRequest(), response);
        context.setError(new IllegalStateException("boom"));

        KeyValues keyValues = convention.getLowCardinalityKeyValues(context);

        assertThat(keyValues).contains(KeyValue.of("error.type", "java.lang.IllegalStateException"));
    }

    @Test
    void errorTypeIsCanonicalNameOfNestedExceptionWithoutServerErrorStatus() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(404);
        ServerRequestObservationContext context = new ServerRequestObservationContext(new MockHttpServletRequest(), response);
        context.setError(new OrderNotFoundException());

        KeyValues keyValues = convention.getLowCardinalityKeyValues(context);

        assertThat(keyValues).contains(KeyValue.of("error.type",
                "io.arconia.observation.opentelemetry.instrumentation.http.ExtendedOpenTelemetryServerRequestObservationConventionTests.OrderNotFoundException"));
    }

    @Test
    void errorTypeIsNoneForClientError() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(404);
        ServerRequestObservationContext context = new ServerRequestObservationContext(new MockHttpServletRequest(), response);

        KeyValues keyValues = convention.getLowCardinalityKeyValues(context);

        assertThat(keyValues).contains(KeyValue.of("error.type", KeyValue.NONE_VALUE));
    }

    // LOW CARDINALITY — network.protocol.version

    @Test
    void networkProtocolVersionForHttp11() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setProtocol("HTTP/1.1");
        ServerRequestObservationContext context = new ServerRequestObservationContext(request, new MockHttpServletResponse());

        KeyValues keyValues = convention.getLowCardinalityKeyValues(context);

        assertThat(keyValues).contains(KeyValue.of("network.protocol.version", "1.1"));
    }

    @Test
    void networkProtocolVersionForHttp10() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setProtocol("HTTP/1.0");
        ServerRequestObservationContext context = new ServerRequestObservationContext(request, new MockHttpServletResponse());

        KeyValues keyValues = convention.getLowCardinalityKeyValues(context);

        assertThat(keyValues).contains(KeyValue.of("network.protocol.version", "1.0"));
    }

    @Test
    void networkProtocolVersionForHttp2() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setProtocol("HTTP/2");
        ServerRequestObservationContext context = new ServerRequestObservationContext(request, new MockHttpServletResponse());

        KeyValues keyValues = convention.getLowCardinalityKeyValues(context);

        assertThat(keyValues).contains(KeyValue.of("network.protocol.version", "2"));
    }

    @Test
    void networkProtocolVersionForHttp2ReportedWithMinorVersion() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setProtocol("HTTP/2.0");
        ServerRequestObservationContext context = new ServerRequestObservationContext(request, new MockHttpServletResponse());

        KeyValues keyValues = convention.getLowCardinalityKeyValues(context);

        assertThat(keyValues).contains(KeyValue.of("network.protocol.version", "2"));
    }

    @Test
    void networkProtocolVersionForHttp3ReportedWithMinorVersion() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setProtocol("HTTP/3.0");
        ServerRequestObservationContext context = new ServerRequestObservationContext(request, new MockHttpServletResponse());

        KeyValues keyValues = convention.getLowCardinalityKeyValues(context);

        assertThat(keyValues).contains(KeyValue.of("network.protocol.version", "3"));
    }

    @Test
    void networkProtocolVersionForHttp3() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setProtocol("HTTP/3");
        ServerRequestObservationContext context = new ServerRequestObservationContext(request, new MockHttpServletResponse());

        KeyValues keyValues = convention.getLowCardinalityKeyValues(context);

        assertThat(keyValues).contains(KeyValue.of("network.protocol.version", "3"));
    }

    @Test
    void networkProtocolVersionAbsentWhenProtocolIsNotHttp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setProtocol("BLAH");
        ServerRequestObservationContext context = new ServerRequestObservationContext(request, new MockHttpServletResponse());

        KeyValues keyValues = convention.getLowCardinalityKeyValues(context);

        assertThat(keyValues).noneMatch(kv -> "network.protocol.version".equals(kv.getKey()));
    }

    @Test
    void networkProtocolVersionAbsentWhenProtocolIsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setProtocol(null);
        ServerRequestObservationContext context = new ServerRequestObservationContext(request, new MockHttpServletResponse());

        KeyValues keyValues = convention.getLowCardinalityKeyValues(context);

        assertThat(keyValues).noneMatch(kv -> "network.protocol.version".equals(kv.getKey()));
    }

    // LOW CARDINALITY — http.route, http.response.status_code, url.scheme

    @Test
    void routeFromPathPattern() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users/42");
        ServerRequestObservationContext context = new ServerRequestObservationContext(request, new MockHttpServletResponse());
        context.setPathPattern("/users/{id}");

        assertThat(convention.getLowCardinalityKeyValues(context)).contains(KeyValue.of("http.route", "/users/{id}"));
        assertThat(convention.getContextualName(context)).isEqualTo("GET /users/{id}");
    }

    @Test
    void rootRouteForEmptyPathPattern() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        ServerRequestObservationContext context = new ServerRequestObservationContext(request, new MockHttpServletResponse());
        context.setPathPattern("");

        assertThat(convention.getLowCardinalityKeyValues(context)).contains(KeyValue.of("http.route", "/"));
        assertThat(convention.getContextualName(context)).isEqualTo("GET /");
    }

    @Test
    void routeAbsentWhenNoPathPatternMatched() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/missing");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(404);
        ServerRequestObservationContext context = new ServerRequestObservationContext(request, response);

        assertThat(convention.getLowCardinalityKeyValues(context)).noneMatch(kv -> "http.route".equals(kv.getKey()));
        assertThat(convention.getContextualName(context)).isEqualTo("GET");
    }

    @Test
    void routeAbsentForRedirectionWithoutPathPattern() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/old");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(302);
        ServerRequestObservationContext context = new ServerRequestObservationContext(request, response);

        assertThat(convention.getLowCardinalityKeyValues(context)).noneMatch(kv -> "http.route".equals(kv.getKey()));
    }

    @Test
    void statusCodeAndSchemeFromRequestAndResponse() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(201);
        ServerRequestObservationContext context = new ServerRequestObservationContext(request, response);

        assertThat(convention.getLowCardinalityKeyValues(context)).contains(
                KeyValue.of("http.response.status_code", "201"),
                KeyValue.of("url.scheme", "http"));
    }

    // HIGH CARDINALITY — client.address

    @Test
    void clientAddressFromRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.42");
        ServerRequestObservationContext context = new ServerRequestObservationContext(request, new MockHttpServletResponse());

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);

        assertThat(keyValues).contains(KeyValue.of("client.address", "192.168.1.42"));
    }

    // HIGH CARDINALITY — server.address and server.port

    @Test
    void serverAddressAndPortFromRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("api.example.com");
        request.setServerPort(8443);
        ServerRequestObservationContext context = new ServerRequestObservationContext(request, new MockHttpServletResponse());

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);

        assertThat(keyValues).contains(KeyValue.of("server.address", "api.example.com"));
        assertThat(keyValues).contains(KeyValue.of("server.port", "8443"));
    }

    // HIGH CARDINALITY — user_agent.original

    @Test
    void userAgentFromRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "Mozilla/5.0");
        ServerRequestObservationContext context = new ServerRequestObservationContext(request, new MockHttpServletResponse());

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);

        assertThat(keyValues).contains(KeyValue.of("user_agent.original", "Mozilla/5.0"));
    }

    @Test
    void userAgentAbsentWhenHeaderMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        ServerRequestObservationContext context = new ServerRequestObservationContext(request, new MockHttpServletResponse());

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);

        assertThat(keyValues).doesNotContain(KeyValue.of("user_agent.original", "UNKNOWN"));
        assertThat(keyValues.stream().noneMatch(kv -> "user_agent.original".equals(kv.getKey()))).isTrue();
    }

    // HIGH CARDINALITY — url.query

    @Test
    void urlQueryCapturedByDefault() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString("foo=bar&sig=secret");
        ServerRequestObservationContext context = new ServerRequestObservationContext(request, new MockHttpServletResponse());

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);

        assertThat(keyValues).contains(KeyValue.of("url.query", "foo=bar&sig=REDACTED"));
    }

    @Test
    void urlQueryExcludedWhenDisabled() {
        OpenTelemetryHttpOptions options = new OpenTelemetryHttpOptions();
        options.setCaptureUrlQuery(false);
        var customConvention = new ExtendedOpenTelemetryServerRequestObservationConvention(options);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString("foo=bar&baz=qux");
        ServerRequestObservationContext context = new ServerRequestObservationContext(request, new MockHttpServletResponse());

        KeyValues keyValues = customConvention.getHighCardinalityKeyValues(context);

        assertThat(keyValues.stream().noneMatch(kv -> "url.query".equals(kv.getKey()))).isTrue();
    }

    @Test
    void urlQueryIncludedWhenEnabled() {
        var options = new OpenTelemetryHttpOptions();
        options.setCaptureUrlQuery(true);
        var conventionWithQuery = new ExtendedOpenTelemetryServerRequestObservationConvention(options);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString("foo=bar&baz=qux");
        ServerRequestObservationContext context = new ServerRequestObservationContext(request, new MockHttpServletResponse());

        KeyValues keyValues = conventionWithQuery.getHighCardinalityKeyValues(context);

        assertThat(keyValues).contains(KeyValue.of("url.query", "foo=bar&baz=qux"));
    }

    @Test
    void urlQueryRedactsSensitiveValuesWhenEnabled() {
        var options = new OpenTelemetryHttpOptions();
        options.setCaptureUrlQuery(true);
        var conventionWithQuery = new ExtendedOpenTelemetryServerRequestObservationConvention(options);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString("file=report.pdf&X-Amz-Signature=abc123");
        ServerRequestObservationContext context = new ServerRequestObservationContext(request, new MockHttpServletResponse());

        KeyValues keyValues = conventionWithQuery.getHighCardinalityKeyValues(context);

        assertThat(keyValues).contains(KeyValue.of("url.query", "file=report.pdf&X-Amz-Signature=REDACTED"));
    }

    @Test
    void urlQueryRedactsConfiguredSensitiveParameters() {
        var options = new OpenTelemetryHttpOptions();
        options.setCaptureUrlQuery(true);
        options.setSensitiveQueryParameters(List.of("token"));
        var conventionWithQuery = new ExtendedOpenTelemetryServerRequestObservationConvention(options);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString("token=abc&X-Amz-Signature=def");
        ServerRequestObservationContext context = new ServerRequestObservationContext(request, new MockHttpServletResponse());

        KeyValues keyValues = conventionWithQuery.getHighCardinalityKeyValues(context);

        assertThat(keyValues).contains(KeyValue.of("url.query", "token=REDACTED&X-Amz-Signature=def"));
    }

    @Test
    void urlQueryAbsentWhenEnabledButNoQueryString() {
        var options = new OpenTelemetryHttpOptions();
        options.setCaptureUrlQuery(true);
        var conventionWithQuery = new ExtendedOpenTelemetryServerRequestObservationConvention(options);

        MockHttpServletRequest request = new MockHttpServletRequest();
        ServerRequestObservationContext context = new ServerRequestObservationContext(request, new MockHttpServletResponse());

        KeyValues keyValues = conventionWithQuery.getHighCardinalityKeyValues(context);

        assertThat(keyValues.stream().noneMatch(kv -> "url.query".equals(kv.getKey()))).isTrue();
    }

    // HIGH CARDINALITY — parent attributes preserved

    @Test
    void parentHighCardinalityAttributesPreserved() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        ServerRequestObservationContext context = new ServerRequestObservationContext(request, new MockHttpServletResponse());

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);

        assertThat(keyValues).contains(KeyValue.of("url.path", "/api/users"));
    }

    // http.request.method / http.request.method_original

    @Test
    void methodOriginalAbsentForKnownMethod() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        ServerRequestObservationContext context = new ServerRequestObservationContext(request, new MockHttpServletResponse());

        assertThat(convention.getLowCardinalityKeyValues(context)).contains(KeyValue.of("http.request.method", "GET"));
        assertThat(convention.getHighCardinalityKeyValues(context))
                .noneMatch(kv -> "http.request.method_original".equals(kv.getKey()));
    }

    @Test
    void methodOriginalPresentForUnknownMethod() {
        MockHttpServletRequest request = new MockHttpServletRequest("PROPFIND", "/api/users");
        ServerRequestObservationContext context = new ServerRequestObservationContext(request, new MockHttpServletResponse());

        assertThat(convention.getLowCardinalityKeyValues(context)).contains(KeyValue.of("http.request.method", "_OTHER"));
        assertThat(convention.getHighCardinalityKeyValues(context))
                .contains(KeyValue.of("http.request.method_original", "PROPFIND"));
    }

    @Test
    void connectIsKnownMethod() {
        MockHttpServletRequest request = new MockHttpServletRequest("CONNECT", "/");
        ServerRequestObservationContext context = new ServerRequestObservationContext(request, new MockHttpServletResponse());

        assertThat(convention.getLowCardinalityKeyValues(context)).contains(KeyValue.of("http.request.method", "CONNECT"));
        assertThat(convention.getHighCardinalityKeyValues(context))
                .noneMatch(kv -> "http.request.method_original".equals(kv.getKey()));
        assertThat(convention.getContextualName(context)).isEqualTo("CONNECT");
    }

    @Test
    void queryIsKnownMethod() {
        MockHttpServletRequest request = new MockHttpServletRequest("QUERY", "/api/users");
        ServerRequestObservationContext context = new ServerRequestObservationContext(request, new MockHttpServletResponse());

        assertThat(convention.getLowCardinalityKeyValues(context)).contains(KeyValue.of("http.request.method", "QUERY"));
        assertThat(convention.getHighCardinalityKeyValues(context))
                .noneMatch(kv -> "http.request.method_original".equals(kv.getKey()));
        assertThat(convention.getContextualName(context)).isEqualTo("QUERY");
    }

    static class OrderNotFoundException extends RuntimeException {
    }

}
