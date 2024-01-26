package io.arconia.core.multitenancy.events;

import org.springframework.context.ApplicationListener;

/**
 * A listener for {@link TenantEvent}s.
 *
 * @author Thomas Vitale
 */
@FunctionalInterface
public interface TenantEventListener extends ApplicationListener<TenantEvent> {

}
