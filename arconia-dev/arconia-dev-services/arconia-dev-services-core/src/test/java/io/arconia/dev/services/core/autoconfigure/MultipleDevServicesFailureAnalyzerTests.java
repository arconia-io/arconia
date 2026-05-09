package io.arconia.dev.services.core.autoconfigure;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.diagnostics.FailureAnalysis;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MultipleDevServicesFailureAnalyzer}.
 */
class MultipleDevServicesFailureAnalyzerTests {

    private final MultipleDevServicesFailureAnalyzer analyzer = new MultipleDevServicesFailureAnalyzer();

    @Test
    void shouldProduceActionableFailureAnalysis() {
        var exception = new MultipleDevServicesException("opentelemetry", List.of("lgtm", "openlit"));

        FailureAnalysis analysis = analyzer.analyze(exception, exception);

        assertThat(analysis).isNotNull();
        assertThat(analysis.getDescription()).contains("opentelemetry", "lgtm", "openlit");
        assertThat(analysis.getAction()).contains("arconia.dev.services", "opentelemetry");
    }

}
