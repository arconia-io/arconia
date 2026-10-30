package io.arconia.observation.conventions;

/**
 * Identifies an AI observation convention module. Each module that provides AI observation conventions
 * registers a bean implementing this interface, so that the application fails at startup when more than
 * one is active.
 */
public interface AiObservationConventionsProvider {

    /**
     * The name of the AI observation conventions provided by this module (e.g., {@code "opentelemetry"},
     * {@code "openinference"}). Used for conflict detection and error reporting: when a conflict is detected,
     * the module is expected to be disabled by the {@code arconia.observations.conventions.<name>.ai.enabled}
     * property, which the failure analysis points at.
     */
    String name();

    /**
     * Create an {@link AiObservationConventionsProvider} with the given name.
     */
    static AiObservationConventionsProvider of(String name) {
        return () -> name;
    }

}
