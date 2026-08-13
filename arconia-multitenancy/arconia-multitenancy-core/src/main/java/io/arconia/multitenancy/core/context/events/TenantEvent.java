package io.arconia.multitenancy.core.context.events;

import org.springframework.context.ApplicationEvent;
import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;

/**
 * Abstract superclass for all tenant-related events.
 *
 * <p>
 * The hierarchy is sealed. Applications that need their own tenant-related events should
 * extend {@link ApplicationEvent} directly, since nothing in the framework dispatches on
 * this type.
 */
@Incubating
public abstract sealed class TenantEvent extends ApplicationEvent
        permits TenantContextAttachedEvent, TenantContextClosedEvent {

    private final String tenantIdentifier;

    protected TenantEvent(String tenantIdentifier, Object source) {
        super(source);
        Assert.hasText(tenantIdentifier, "tenantIdentifier cannot be null or empty");
        this.tenantIdentifier = tenantIdentifier;
    }

    public String getTenantIdentifier() {
        return tenantIdentifier;
    }

}
