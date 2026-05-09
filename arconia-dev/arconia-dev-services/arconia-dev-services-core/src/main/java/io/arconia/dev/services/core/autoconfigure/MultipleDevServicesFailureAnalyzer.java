package io.arconia.dev.services.core.autoconfigure;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalyzer;

/**
 * A {@link FailureAnalyzer} that provides actionable feedback
 * when multiple dev services in the same category are detected.
 */
public class MultipleDevServicesFailureAnalyzer extends AbstractFailureAnalyzer<MultipleDevServicesException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, MultipleDevServicesException cause) {
        return new FailureAnalysis(
                "Multiple %s dev services detected: %s.".formatted(cause.getCategory(), cause.getServiceNames()),
                """
                Disable all but one %s dev service by setting the enabled property to false
                (e.g., arconia.dev.services.%s.enabled=false)."""
                        .formatted(cause.getCategory(), cause.getServiceNames().getFirst()),
                cause);
    }

}
