package io.arconia.observation.autoconfigure;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalyzer;

/**
 * A {@link FailureAnalyzer} that provides actionable feedback
 * when multiple AI observation convention modules are detected.
 */
public class MultipleAiObservationConventionsFailureAnalyzer
        extends AbstractFailureAnalyzer<MultipleAiObservationConventionsException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, MultipleAiObservationConventionsException cause) {
        return new FailureAnalysis(
                "Multiple AI observation convention modules detected: %s.".formatted(cause.getConventionNames()),
                """
                Disable all but one AI observation convention module by setting its enabled property to false
                (e.g., arconia.observations.conventions.%s.ai.enabled=false)."""
                        .formatted(cause.getConventionNames().getFirst()),
                cause);
    }

}
