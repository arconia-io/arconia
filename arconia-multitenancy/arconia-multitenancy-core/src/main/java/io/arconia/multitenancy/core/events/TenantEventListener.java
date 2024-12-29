package io.arconia.multitenancy.core.events;

import org.springframework.context.ApplicationListener;

/**
 * A listener for {@link TenantEvent}s.
 */
@FunctionalInterface
public interface TenantEventListener extends ApplicationListener<TenantEvent> {

}
