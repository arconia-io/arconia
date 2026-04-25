package io.arconia.observation.conventions;

/**
 * Marker interface for observation convention modules. Each module that provides
 * observation conventions (e.g., OpenInference) should register a bean implementing
 * this interface. This enables auto-discovery and conflict detection at startup.
 */
public interface ObservationConventionsProvider {

    /**
     * The name of the observation conventions provided by this module.
     * This value is used for conflict detection and should match the value
     * expected by the {@code arconia.observations.conventions.type} property.
     */
    String name();

}
