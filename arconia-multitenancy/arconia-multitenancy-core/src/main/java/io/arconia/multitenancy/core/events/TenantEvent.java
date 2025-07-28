package io.arconia.multitenancy.core.events;

import org.springframework.context.ApplicationEvent;
import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;

/**
 * Abstract superclass for all tenant-related events.
 */
@Incubating(since = "0.1.0")
public abstract class TenantEvent extends ApplicationEvent {

    private final String tenantIdentifier;

    public TenantEvent(String tenantIdentifier, Object source) {
        super(source);
        Assert.hasText(tenantIdentifier, "tenantIdentifier cannot be null or empty");
        this.tenantIdentifier = tenantIdentifier;
    }

    public String getTenantIdentifier() {
        return tenantIdentifier;
    }

}
