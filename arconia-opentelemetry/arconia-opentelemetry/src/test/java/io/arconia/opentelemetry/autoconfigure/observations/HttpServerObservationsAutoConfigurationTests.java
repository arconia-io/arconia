package io.arconia.opentelemetry.autoconfigure.observations;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationPredicate;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link HttpServerObservationsAutoConfiguration}.
 */
class HttpServerObservationsAutoConfigurationTests {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(HttpServerObservationsAutoConfiguration.class));

    @Test
    void predicateAvailableInServletWebApplication() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(ObservationPredicate.class));
    }

    @Test
    void predicateAvailableWhenOpenTelemetryDisabled() {
        contextRunner
            .withPropertyValues("arconia.otel.enabled=false")
            .run(context -> assertThat(context).hasSingleBean(ObservationPredicate.class));
    }

    @Test
    void autoConfigurationNotActivatedInNonWebApplication() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(HttpServerObservationsAutoConfiguration.class))
            .run(context -> assertThat(context).doesNotHaveBean(ObservationPredicate.class));
    }

    @Test
    void autoConfigurationNotActivatedInReactiveWebApplication() {
        new ReactiveWebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(HttpServerObservationsAutoConfiguration.class))
            .run(context -> assertThat(context).doesNotHaveBean(ObservationPredicate.class));
    }

    @Test
    void autoConfigurationNotActivatedWithoutServerRequestObservationContext() {
        contextRunner
            .withClassLoader(new FilteredClassLoader(ServerRequestObservationContext.class))
            .run(context -> assertThat(context).doesNotHaveBean(ObservationPredicate.class));
    }

    @Test
    void defaultIgnorePathsAreNotObserved() {
        contextRunner.run(context -> {
            ObservationPredicate predicate = context.getBean(ObservationPredicate.class);
            assertThat(isObserved(predicate, "/actuator/health")).isFalse();
            assertThat(isObserved(predicate, "/actuator/health/readiness")).isFalse();
            assertThat(isObserved(predicate, "/livez")).isFalse();
            assertThat(isObserved(predicate, "/readyz")).isFalse();
            assertThat(isObserved(predicate, "/actuator/info")).isTrue();
        });
    }

    @Test
    void additionalIgnorePathsAreMergedWithDefaults() {
        contextRunner
            .withPropertyValues("arconia.observations.http.server.additional-ignore-paths=/internal/**")
            .run(context -> {
                ObservationPredicate predicate = context.getBean(ObservationPredicate.class);
                assertThat(isObserved(predicate, "/internal/jobs")).isFalse();
                assertThat(isObserved(predicate, "/livez")).isFalse();
            });
    }

    @Test
    void customIgnorePathsReplaceDefaults() {
        contextRunner
            .withPropertyValues("arconia.observations.http.server.ignore-paths=/health")
            .run(context -> {
                ObservationPredicate predicate = context.getBean(ObservationPredicate.class);
                assertThat(isObserved(predicate, "/health")).isFalse();
                assertThat(isObserved(predicate, "/livez")).isTrue();
            });
    }

    @Test
    void emptyIgnorePathsObserveEverything() {
        contextRunner
            .withPropertyValues("arconia.observations.http.server.ignore-paths=")
            .run(context -> {
                assertThat(context.getBean(HttpServerObservationsProperties.class).getIgnorePaths()).isEmpty();
                ObservationPredicate predicate = context.getBean(ObservationPredicate.class);
                assertThat(isObserved(predicate, "/actuator/health")).isTrue();
                assertThat(isObserved(predicate, "/livez")).isTrue();
            });
    }

    private static boolean isObserved(ObservationPredicate predicate, String requestUri) {
        var request = new MockHttpServletRequest("GET", requestUri);
        Observation.Context context = new ServerRequestObservationContext(request, new MockHttpServletResponse());
        return predicate.test("http.server.requests", context);
    }

}
