package io.arconia.observation.autoconfigure;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.boot.diagnostics.FailureAnalyzer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MultipleAiObservationConventionsFailureAnalyzer}.
 */
class MultipleAiObservationConventionsFailureAnalyzerTests {

    private final MultipleAiObservationConventionsFailureAnalyzer analyzer =
            new MultipleAiObservationConventionsFailureAnalyzer();

    @Test
    void shouldBeRegisteredAsFailureAnalyzer() {
        // Some Spring Boot analyzers need a bean factory to be instantiated and are skipped.
        SpringFactoriesLoader.FailureHandler ignoreFailures = (factoryType, factoryImplementationName, failure) -> { };
        assertThat(SpringFactoriesLoader.forDefaultResourceLocation().load(FailureAnalyzer.class, null, ignoreFailures))
                .anySatisfy(failureAnalyzer -> assertThat(failureAnalyzer)
                        .isInstanceOf(MultipleAiObservationConventionsFailureAnalyzer.class));
    }

    @Test
    void shouldProvideInstructionsWhenConflictDetected() {
        var exception = new MultipleAiObservationConventionsException(List.of("openinference", "opentelemetry"));

        FailureAnalysis analysis = analyzer.analyze(exception, exception);

        assertThat(analysis).isNotNull();
        assertThat(analysis.getDescription()).contains("openinference", "opentelemetry");
        assertThat(analysis.getAction()).contains("arconia.observations.conventions.openinference.ai.enabled=false");
    }

}
