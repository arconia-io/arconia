package io.arconia.observation.autoconfigure;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.diagnostics.FailureAnalysis;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MultipleObservationConventionsFailureAnalyzer}.
 */
class MultipleObservationConventionsFailureAnalyzerTests {

    private final MultipleObservationConventionsFailureAnalyzer analyzer =
            new MultipleObservationConventionsFailureAnalyzer();

    @Test
    void shouldProduceActionableFailureAnalysis() {
        var exception = new MultipleObservationConventionsException(List.of("openinference", "opentelemetry"));

        FailureAnalysis analysis = analyzer.analyze(exception, exception);

        assertThat(analysis).isNotNull();
        assertThat(analysis.getDescription()).contains("openinference", "opentelemetry");
        assertThat(analysis.getAction()).contains("arconia.observations.conventions.type");
    }

}
