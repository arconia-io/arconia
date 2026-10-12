package io.arconia.docling.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import io.arconia.docling.autoconfigure.DoclingProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LoggingClientHttpRequestInterceptor}.
 */
class LoggingClientHttpRequestInterceptorTests {

    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    private final Logger logger = (Logger) LoggerFactory.getLogger(DoclingServeClient.class);

    @BeforeEach
    void setUp() {
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    void logsRequestWithMaskedApiKey() throws IOException {
        var interceptor = new LoggingClientHttpRequestInterceptor(true, false, false);
        var request = new MockClientHttpRequest(HttpMethod.POST, URI.create("http://localhost:5001/v1/convert/source"));
        request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        request.getHeaders().set(DoclingProperties.API_KEY_HEADER_NAME, "super-secret-key");

        interceptor.intercept(request, "{\"a\":1}".getBytes(StandardCharsets.UTF_8), jsonResponse("{\"status\":\"ok\"}"));

        assertThat(appender.list).singleElement().satisfies(event -> {
            assertThat(event.getFormattedMessage())
                    .startsWith("Request:")
                    .contains("POST")
                    .contains("http://localhost:5001/v1/convert/source")
                    .contains("[X-Api-Key: su...ey]")
                    .doesNotContain("super-secret-key")
                    .contains("{\"a\":1}");
        });
    }

    @Test
    void logsJsonResponsePrettyPrinted() throws IOException {
        var interceptor = new LoggingClientHttpRequestInterceptor(false, true, true);
        var request = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://localhost:5001/health"));

        interceptor.intercept(request, new byte[0], jsonResponse("{\"status\":\"ok\"}"));

        assertThat(appender.list).singleElement().satisfies(event -> {
            assertThat(event.getFormattedMessage())
                    .startsWith("Response:")
                    .contains("200")
                    .contains("\"status\" : \"ok\"");
        });
    }

    @Test
    void doesNotLogBinaryResponseBody() throws IOException {
        var interceptor = new LoggingClientHttpRequestInterceptor(false, true, false);
        var request = new MockClientHttpRequest(HttpMethod.POST, URI.create("http://localhost:5001/v1/convert/source"));

        interceptor.intercept(request, new byte[0], (req, body) -> {
            var response = new MockClientHttpResponse(new byte[] {0x50, 0x4B}, HttpStatus.OK);
            response.getHeaders().setContentType(MediaType.parseMediaType("application/zip"));
            return response;
        });

        assertThat(appender.list).singleElement().satisfies(event ->
                assertThat(event.getFormattedMessage()).contains("- body: <binary>"));
    }

    @Test
    void logsNothingWhenDisabled() throws IOException {
        var interceptor = new LoggingClientHttpRequestInterceptor(false, false, false);
        var request = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://localhost:5001/health"));

        interceptor.intercept(request, new byte[0], jsonResponse("{}"));

        assertThat(appender.list).isEmpty();
    }

    private static ClientHttpRequestExecution jsonResponse(String json) {
        return (req, body) -> {
            ClientHttpResponse response = new MockClientHttpResponse(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), HttpStatus.OK);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            return response;
        };
    }

}
