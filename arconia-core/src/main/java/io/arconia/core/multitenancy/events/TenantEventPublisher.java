package io.arconia.core.multitenancy.events;

/**
 * A contract for publishing {@link TenantEvent}s.
 */
@FunctionalInterface
public interface TenantEventPublisher {

    void publishTenantEvent(TenantEvent tenantEvent);

}
