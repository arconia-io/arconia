package io.arconia.core.multitenancy.events;

import org.springframework.context.ApplicationEvent;
import org.springframework.util.Assert;

/**
 * Abstract superclass for all tenant-related events.
 *
 * @author Thomas Vitale
 */
public abstract class TenantEvent extends ApplicationEvent {

    private final String tenantId;

    public TenantEvent(String tenantId, Object source) {
        super(source);
        Assert.hasText(tenantId, "tenantId cannot be empty");
        this.tenantId = tenantId;
    }

    public String getTenantId() {
        return tenantId;
    }

}
