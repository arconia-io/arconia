package io.arconia.multitenancy.core.events;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;

/**
 * An implementation of {@link TenantEventPublisher} that uses Spring's event publishing
 * support.
 */
@Incubating(since = "0.1.0")
public class DefaultTenantEventPublisher implements TenantEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public DefaultTenantEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        Assert.notNull(applicationEventPublisher, "applicationEventPublisher cannot be null");
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publishTenantEvent(TenantEvent tenantEvent) {
        Assert.notNull(tenantEvent, "tenantEvent cannot be null");
        applicationEventPublisher.publishEvent(tenantEvent);
    }

}
