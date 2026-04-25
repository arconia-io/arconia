package io.arconia.observation.opentelemetry.instrumentation.http;

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
    void networkProtocolVersionForHttp3() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setProtocol("HTTP/3");
        ServerRequestObservationContext context = new ServerRequestObservationContext(request, new MockHttpServletResponse());

        KeyValues keyValues = convention.getLowCardinalityKeyValues(context);

        assertThat(keyValues).contains(KeyValue.of("network.protocol.version", "3"));
    }

    @Test
    void networkProtocolVersionUnknownWhenProtocolIsNotHttp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setProtocol("BLAH");
        ServerRequestObservationContext context = new ServerRequestObservationContext(request, new MockHttpServletResponse());

        KeyValues keyValues = convention.getLowCardinalityKeyValues(context);

        assertThat(keyValues).contains(KeyValue.of("network.protocol.version", "UNKNOWN"));
    }

    @Test
    void networkProtocolVersionUnknownWhenProtocolIsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setProtocol(null);
        ServerRequestObservationContext context = new ServerRequestObservationContext(request, new MockHttpServletResponse());

        KeyValues keyValues = convention.getLowCardinalityKeyValues(context);

        assertThat(keyValues).contains(KeyValue.of("network.protocol.version", "UNKNOWN"));
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
    void urlQueryExcludedByDefault() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString("foo=bar&baz=qux");
        ServerRequestObservationContext context = new ServerRequestObservationContext(request, new MockHttpServletResponse());

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);

        assertThat(keyValues.stream().noneMatch(kv -> "url.query".equals(kv.getKey()))).isTrue();
    }

    @Test
    void urlQueryIncludedWhenEnabled() {
        var options = new OpenTelemetryHttpOptions();
        options.setIncludeUrlQuery(true);
        var conventionWithQuery = new ExtendedOpenTelemetryServerRequestObservationConvention(options);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString("foo=bar&baz=qux");
        ServerRequestObservationContext context = new ServerRequestObservationContext(request, new MockHttpServletResponse());

        KeyValues keyValues = conventionWithQuery.getHighCardinalityKeyValues(context);

        assertThat(keyValues).contains(KeyValue.of("url.query", "foo=bar&baz=qux"));
    }

    @Test
    void urlQueryAbsentWhenEnabledButNoQueryString() {
        var options = new OpenTelemetryHttpOptions();
        options.setIncludeUrlQuery(true);
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
        assertThat(keyValues).contains(KeyValue.of("http.request.method_original", "GET"));
    }

}
