package io.arconia.observation.autoconfigure;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalyzer;

/**
 * A {@link FailureAnalyzer} that provides actionable feedback
 * when multiple observation convention modules are detected.
 */
public class MultipleObservationConventionsFailureAnalyzer
        extends AbstractFailureAnalyzer<MultipleObservationConventionsException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, MultipleObservationConventionsException cause) {
        return new FailureAnalysis(
                "Multiple observation convention modules detected: %s.".formatted(cause.getConventionNames()),
                """
                Set the 'arconia.observations.conventions.type' property to select one explicitly
                (e.g., arconia.observations.conventions.type=%s).""".formatted(cause.getConventionNames().getFirst()),
                cause);
    }

}
