package io.arconia.dev.services.tests;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.DockerClientFactory;

/**
 * The condition behind {@link DisabledIfDockerUnavailable}: a test runs only when
 * Testcontainers can reach a container runtime.
 */
class DisabledIfDockerUnavailableCondition implements ExecutionCondition {

    private static final ConditionEvaluationResult ENABLED = ConditionEvaluationResult.enabled("Docker available");

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        try {
            DockerClientFactory.instance().client();
            return ENABLED;
        }
        catch (Throwable ex) {
            return ConditionEvaluationResult.disabled("Docker unavailable", ex.getMessage());
        }
    }

}
