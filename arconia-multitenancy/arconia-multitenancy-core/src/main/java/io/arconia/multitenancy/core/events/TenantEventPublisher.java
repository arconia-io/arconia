package io.arconia.multitenancy.core.events;

/**
 * A contract for publishing {@link TenantEvent}s.
 */
@FunctionalInterface
public interface TenantEventPublisher {

    void publishTenantEvent(TenantEvent tenantEvent);

}
