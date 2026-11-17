package io.arconia.opentelemetry.autoconfigure.observations;

import java.util.Set;

import io.micrometer.observation.Observation;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link IgnorePathsObservationPredicate}.
 */
class IgnorePathsObservationPredicateTests {

    private final IgnorePathsObservationPredicate predicate = new IgnorePathsObservationPredicate(
            Set.of("/actuator/health/**", "/livez"));

    @AfterEach
    void resetRequestAttributes() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void whenNullPathsThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new IgnorePathsObservationPredicate(null))
                .withMessageContaining("ignorePathPatterns cannot be null");
    }

    @Nested
    class HttpServerObservation {

        @Test
        void ignoredPathIsNotObserved() {
            assertThat(predicate.test("http.server.requests", serverContext(request("/livez")))).isFalse();
        }

        @Test
        void ignoredPathWithTrailingPatternMatchesItsRoot() {
            assertThat(predicate.test("http.server.requests", serverContext(request("/actuator/health")))).isFalse();
        }

        @Test
        void ignoredPathWithTrailingPatternMatchesNestedPaths() {
            assertThat(predicate.test("http.server.requests", serverContext(request("/actuator/health/liveness"))))
                .isFalse();
        }

        @Test
        void otherPathIsObserved() {
            assertThat(predicate.test("http.server.requests", serverContext(request("/actuator/info")))).isTrue();
        }

        @Test
        void similarPrefixIsObserved() {
            assertThat(predicate.test("http.server.requests", serverContext(request("/actuator/healthy")))).isTrue();
        }

        @Test
        void ignoredPathUnderContextPathIsNotObserved() {
            var request = request("/app/livez");
            request.setContextPath("/app");
            assertThat(predicate.test("http.server.requests", serverContext(request))).isFalse();
        }

        @Test
        void contextPathIsNotPartOfTheMatchedPath() {
            var request = request("/app/livez");
            request.setContextPath("/app");
            var predicate = new IgnorePathsObservationPredicate(Set.of("/app/livez"));
            assertThat(predicate.test("http.server.requests", serverContext(request))).isTrue();
        }

        @Test
        void encodedCharacterIsMatchedDecodedWithinItsSegment() {
            assertThat(predicate.test("http.server.requests", serverContext(request("/%6Civez")))).isFalse();
        }

        @Test
        void encodedSlashIsNotPathSeparator() {
            var predicate = new IgnorePathsObservationPredicate(Set.of("/actuator/health"));
            assertThat(predicate.test("http.server.requests", serverContext(request("/actuator%2Fhealth")))).isTrue();
        }

        @Test
        void patternWithoutLeadingSlashIsMatched() {
            var predicate = new IgnorePathsObservationPredicate(Set.of("livez"));
            assertThat(predicate.test("http.server.requests", serverContext(request("/livez")))).isFalse();
        }

    }

    @Nested
    class ObservationDuringRequest {

        @Test
        void observationDuringIgnoredRequestIsNotObserved() {
            bind(request("/livez"));
            assertThat(predicate.test("jdbc.query", new Observation.Context())).isFalse();
        }

        @Test
        void observationDuringOtherRequestIsObserved() {
            bind(request("/books"));
            assertThat(predicate.test("jdbc.query", new Observation.Context())).isTrue();
        }

        @Test
        void observationOutsideRequestIsObserved() {
            assertThat(predicate.test("jdbc.query", new Observation.Context())).isTrue();
        }

        @Test
        void errorDispatchOfIgnoredRequestIsNotObserved() {
            var request = request("/error");
            request.setDispatcherType(DispatcherType.ERROR);
            request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/livez");
            bind(request);
            assertThat(predicate.test("spring.security.filterchains", new Observation.Context())).isFalse();
        }

        @Test
        void errorDispatchOfOtherRequestIsObserved() {
            var request = request("/error");
            request.setDispatcherType(DispatcherType.ERROR);
            request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/books");
            bind(request);
            assertThat(predicate.test("spring.security.filterchains", new Observation.Context())).isTrue();
        }

        @Test
        void decisionIsKeptForTheWholeRequest() {
            var request = request("/livez");
            assertThat(predicate.test("http.server.requests", serverContext(request))).isFalse();
            // A later dispatch of the same request to another path keeps the first decision.
            request.setRequestURI("/books");
            bind(request);
            assertThat(predicate.test("jdbc.query", new Observation.Context())).isFalse();
        }

    }

    @Nested
    class NoIgnorePaths {

        @Test
        void everythingIsObserved() {
            var predicate = new IgnorePathsObservationPredicate(Set.of());
            assertThat(predicate.test("http.server.requests", serverContext(request("/livez")))).isTrue();
        }

    }

    private static MockHttpServletRequest request(String requestUri) {
        var request = new MockHttpServletRequest("GET", requestUri);
        request.setRequestURI(requestUri);
        return request;
    }

    private static ServerRequestObservationContext serverContext(MockHttpServletRequest request) {
        return new ServerRequestObservationContext(request, new MockHttpServletResponse());
    }

    private static void bind(MockHttpServletRequest request) {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

}
