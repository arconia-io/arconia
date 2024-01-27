package io.arconia.core.multitenancy.events;

import org.springframework.context.ApplicationEvent;
import org.springframework.util.Assert;

/**
 * Abstract superclass for all tenant-related events.
 *
 * @author Thomas Vitale
 */
public abstract class TenantEvent extends ApplicationEvent {

    private final String tenantIdentifier;

    public TenantEvent(String tenantIdentifier, Object source) {
        super(source);
        Assert.hasText(tenantIdentifier, "tenantIdentifier cannot be empty");
        this.tenantIdentifier = tenantIdentifier;
    }

    public String getTenantIdentifier() {
        return tenantIdentifier;
    }

}
