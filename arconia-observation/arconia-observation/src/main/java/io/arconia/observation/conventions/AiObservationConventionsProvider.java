package io.arconia.observation.conventions;

/**
 * Marker interface for AI observation convention modules. Each module that provides
 * AI observation conventions registers a bean implementing this interface to enable
 * conflict detection at startup.
 */
public interface AiObservationConventionsProvider {

    /**
     * The name of the AI observation conventions provided by this module (e.g., {@code "opentelemetry"},
     * {@code "openinference"}). Used for conflict detection and error reporting.
     */
    String name();

    /**
     * Create an {@link AiObservationConventionsProvider} with the given name.
     */
    static AiObservationConventionsProvider of(String name) {
        return () -> name;
    }

}
