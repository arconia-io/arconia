package io.arconia.core.multitenancy.events;

import org.springframework.context.ApplicationListener;

/**
 * A listener for {@link TenantEvent}s.
 */
@FunctionalInterface
public interface TenantEventListener extends ApplicationListener<TenantEvent> {

}
