package io.arconia.docling.client;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import ai.docling.serve.api.validation.ValidationException;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.HttpClientErrorException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ValidationErrorHandler}.
 */
class ValidationErrorHandlerTests {

    private final ValidationErrorHandler handler = new ValidationErrorHandler();

    private final MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.POST, URI.create("http://localhost:5001/v1/convert/source"));

    @Test
    void throwsValidationExceptionWithDetails() {
        var response = response("""
                {"detail": [{"loc": ["body", "sources", 0, "url"], "msg": "Input should be a valid URL", "type": "url_parsing", "input": "not-a-url", "ctx": {"expected_schemes": "http"}}]}
                """);

        assertThatThrownBy(() -> handler.handle(request, response))
                .isInstanceOfSatisfying(ValidationException.class, ex -> {
                    assertThat(ex.getMessage())
                            .contains("POST")
                            .contains("http://localhost:5001/v1/convert/source")
                            .contains("Input should be a valid URL");
                    assertThat(ex.getValidationError()).isNotNull();
                    assertThat(ex.getValidationError().getErrorDetails()).singleElement().satisfies(detail -> {
                        assertThat(detail.getMessage()).isEqualTo("Input should be a valid URL");
                        assertThat(detail.getType()).isEqualTo("url_parsing");
                        assertThat(detail.getLocations()).containsExactly("body", "sources", 0, "url");
                        assertThat(detail.getContext().getExpectedSchemes()).isEqualTo("http");
                        assertThat(detail.getInput()).isNull();
                    });
                });
    }

    @Test
    void fallsBackToClientErrorWhenBodyHasNoDetails() {
        var response = response("{\"message\": \"rejected by the gateway\"}");

        assertThatThrownBy(() -> handler.handle(request, response))
                .isInstanceOfSatisfying(HttpClientErrorException.UnprocessableContent.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
                    assertThat(ex.getResponseBodyAsString()).contains("rejected by the gateway");
                });
    }

    @Test
    void fallsBackToClientErrorWhenBodyIsNotJson() {
        var response = new MockClientHttpResponse("<html>Unprocessable</html>".getBytes(StandardCharsets.UTF_8), HttpStatus.UNPROCESSABLE_CONTENT);
        response.getHeaders().setContentType(MediaType.TEXT_HTML);

        assertThatThrownBy(() -> handler.handle(request, response))
                .isInstanceOfSatisfying(HttpClientErrorException.class, ex ->
                        assertThat(ex.getResponseBodyAsString()).isEqualTo("<html>Unprocessable</html>"));
    }

    private static MockClientHttpResponse response(String json) {
        var response = new MockClientHttpResponse(json.getBytes(StandardCharsets.UTF_8), HttpStatus.UNPROCESSABLE_CONTENT);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return response;
    }

}
