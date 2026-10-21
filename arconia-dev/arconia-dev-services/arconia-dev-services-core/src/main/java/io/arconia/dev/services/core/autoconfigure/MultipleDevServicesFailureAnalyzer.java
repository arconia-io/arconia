package io.arconia.dev.services.core.autoconfigure;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalyzer;

import io.arconia.dev.services.core.registration.MultipleDevServicesException;

/**
 * A {@link FailureAnalyzer} that provides actionable feedback
 * when multiple dev services in the same category are detected.
 */
class MultipleDevServicesFailureAnalyzer extends AbstractFailureAnalyzer<MultipleDevServicesException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, MultipleDevServicesException cause) {
        return new FailureAnalysis(
                "Multiple %s dev services detected: %s.".formatted(cause.getCategory(), cause.getServiceNames()),
                """
                Disable all but one %s dev service by setting its enabled property to false.
                For example, to disable the '%s' dev service, set arconia.dev.services.%s.enabled=false \
                (a dev service that is not part of Arconia uses the configuration prefix it documents)."""
                        .formatted(cause.getCategory(), cause.getServiceNames().getFirst(), cause.getServiceNames().getFirst()),
                cause);
    }

}
