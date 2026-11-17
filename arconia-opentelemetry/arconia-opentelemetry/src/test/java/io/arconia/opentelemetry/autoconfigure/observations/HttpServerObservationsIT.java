package io.arconia.opentelemetry.autoconfigure.observations;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import jakarta.servlet.DispatcherType;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link HttpServerObservationsAutoConfiguration} on a Servlet web
 * server with Spring Security, covering the observations started while handling the
 * request and on its error dispatch.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "arconia.otel.exporter.type=none",
        "arconia.observations.http.server.additional-ignore-paths=/internal/**",
        "management.endpoint.health.probes.add-additional-paths=true",
        "management.endpoints.web.exposure.include=health,info"
})
class HttpServerObservationsIT {

    private static final String CHILD_OBSERVATION = "test.child";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @LocalServerPort
    int port;

    @Autowired
    RecordingObservationHandler handler;

    @BeforeEach
    void clearRecordedObservations() {
        handler.started.clear();
    }

    @ParameterizedTest
    @ValueSource(strings = { "/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness", "/livez", "/readyz" })
    void healthEndpointIsNotObserved(String path) throws Exception {
        assertThat(get(path)).isEqualTo(200);
        assertThat(handler.started).isEmpty();
    }

    @Test
    void otherActuatorEndpointIsObserved() throws Exception {
        assertThat(get("/actuator/info")).isEqualTo(200);
        assertThat(handler.names()).contains("http.server.requests").anyMatch(name -> name.startsWith("spring.security."));
    }

    @Test
    void applicationEndpointIsObservedWithChildObservations() throws Exception {
        assertThat(get("/work")).isEqualTo(200);
        assertThat(handler.names()).contains("http.server.requests", CHILD_OBSERVATION)
            .anyMatch(name -> name.startsWith("spring.security."));
    }

    @Test
    void additionalIgnorePathIsNotObserved() throws Exception {
        assertThat(get("/internal/work")).isEqualTo(200);
        assertThat(handler.started).isEmpty();
    }

    @Test
    void failingRequestIsObservedOnErrorDispatch() throws Exception {
        assertThat(get("/fail")).isEqualTo(500);
        assertThat(handler.started).anyMatch(started -> started.dispatcherType() == DispatcherType.ERROR);
    }

    @Test
    void failingRequestOnIgnorePathIsNotObservedOnErrorDispatch() throws Exception {
        assertThat(get("/internal/fail")).isEqualTo(500);
        assertThat(handler.started).isEmpty();
    }

    private int get(String path) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
        return httpClient.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    record StartedObservation(String name, @Nullable DispatcherType dispatcherType) {
    }

    static class RecordingObservationHandler implements ObservationHandler<Observation.Context> {

        final List<StartedObservation> started = new CopyOnWriteArrayList<>();

        @Override
        public void onStart(Observation.Context context) {
            var dispatcherType = RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                    ? attributes.getRequest().getDispatcherType() : null;
            started.add(new StartedObservation(context.getName(), dispatcherType));
        }

        @Override
        public boolean supportsContext(Observation.Context context) {
            return true;
        }

        List<String> names() {
            return started.stream().map(StartedObservation::name).toList();
        }

    }

    @SpringBootApplication
    static class TestApplication {

        @Bean
        RecordingObservationHandler recordingObservationHandler() {
            return new RecordingObservationHandler();
        }

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) {
            return http.authorizeHttpRequests(requests -> requests.anyRequest().permitAll()).build();
        }

        // Stands in for a health indicator querying a database, which is observed.
        @Bean
        HealthIndicator observedHealthIndicator(ObservationRegistry observationRegistry) {
            return () -> Observation.createNotStarted(CHILD_OBSERVATION, observationRegistry)
                .observe(() -> Health.up().build());
        }

        @Bean
        TestController testController(ObservationRegistry observationRegistry) {
            return new TestController(observationRegistry);
        }

    }

    @RestController
    static class TestController {

        private final ObservationRegistry observationRegistry;

        TestController(ObservationRegistry observationRegistry) {
            this.observationRegistry = observationRegistry;
        }

        @GetMapping({ "/work", "/internal/work" })
        String work() {
            return Observation.createNotStarted(CHILD_OBSERVATION, observationRegistry).observe(() -> "done");
        }

        @GetMapping({ "/fail", "/internal/fail" })
        String fail() {
            throw new IllegalStateException("Failure on purpose");
        }

    }

}
