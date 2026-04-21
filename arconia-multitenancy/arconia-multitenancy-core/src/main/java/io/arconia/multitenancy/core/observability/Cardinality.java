package io.arconia.multitenancy.core.observability;

/**
 * The cardinality of a tenant identifier key value in observations.
 *
 * <ul>
 * <li>{@link #HIGH} — the tenant identifier appears only in traces.
 * <li>{@link #LOW} — the tenant identifier appears in both metrics and traces.
 * </ul>
 */
public enum Cardinality {

    /**
     * High-cardinality: the tenant identifier is added as a high-cardinality key value,
     * appearing only in traces.
     */
    HIGH,

    /**
     * Low-cardinality: the tenant identifier is added as a low-cardinality key value,
     * appearing in both metrics and traces.
     */
    LOW

}
