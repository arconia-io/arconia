package io.arconia.multitenancy.web.resolution;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import io.arconia.multitenancy.web.resolution.TenantResolutionFailure.Outcome;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ProblemDetailTenantResolutionFailureHandler}.
 */
class ProblemDetailTenantResolutionFailureHandlerTests {

    private final ProblemDetailTenantResolutionFailureHandler handler = new ProblemDetailTenantResolutionFailureHandler();

    @ParameterizedTest
    @CsvSource({ "MISSING, 400, Bad Request", "INVALID, 400, Bad Request", "REJECTED, 400, Bad Request",
            "UNAVAILABLE, 503, Service Unavailable" })
    void whenFailureThenProblemDetailFollowsOutcome(Outcome outcome, int status, String title) throws Exception {
        var response = new MockHttpServletResponse();

        handler.handle(new MockHttpServletRequest("GET", "/products"), response,
                new TenantResolutionFailure(outcome, "Something about the tenant", null));

        assertThat(response.getStatus()).isEqualTo(status);
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(response.getCharacterEncoding()).isEqualToIgnoringCase(StandardCharsets.UTF_8.name());
        JSONAssert.assertEquals("""
                {
                  "title": "%s",
                  "status": %d,
                  "detail": "Something about the tenant",
                  "instance": "/products"
                }
                """.formatted(title, status), response.getContentAsString(), JSONCompareMode.STRICT);
    }

    @Test
    void whenRequestUriIsNotAValidUriThenInstanceIsOmitted() throws Exception {
        var request = new MockHttpServletRequest("GET", "/products");
        request.setRequestURI("/products/{bad uri}");
        var response = new MockHttpServletResponse();

        handler.handle(request, response,
                new TenantResolutionFailure(Outcome.MISSING, "A tenant identifier must be specified", null));

        JSONAssert.assertEquals("""
                {
                  "title": "Bad Request",
                  "status": 400,
                  "detail": "A tenant identifier must be specified"
                }
                """, response.getContentAsString(), JSONCompareMode.STRICT);
    }

    @Test
    void whenNullArgumentsThenThrow() {
        assertThatThrownBy(() -> handler.handle(null, new MockHttpServletResponse(),
                new TenantResolutionFailure(Outcome.MISSING, "message", null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("request cannot be null");
    }

}
