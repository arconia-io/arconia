package io.arconia.multitenancy.core.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.event.EventListener;
import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;
import io.arconia.multitenancy.core.context.events.TenantContextAttachedEvent;
import io.arconia.multitenancy.core.context.events.TenantContextClosedEvent;

/**
 * Manages the SLF4J {@link MDC} tenant identifier in response to tenant context events.
 * <p>
 * Both events are expected to be published synchronously, on the thread the tenant
 * context is bound to. An asynchronous {@code ApplicationEventMulticaster} would apply
 * the MDC value to the wrong thread.
 */
@Incubating
public final class MdcTenantEventListener {

    private static final Logger logger = LoggerFactory.getLogger(MdcTenantEventListener.class);

    private static final String DEFAULT_TENANT_IDENTIFIER_KEY = "tenantId";

    private final String tenantIdentifierKey;

    private MdcTenantEventListener(String tenantIdentifierKey) {
        Assert.hasText(tenantIdentifierKey, "tenantIdentifierKey cannot be null or empty");
        this.tenantIdentifierKey = tenantIdentifierKey;
    }

    public String getTenantIdentifierKey() {
        return tenantIdentifierKey;
    }

    @EventListener
    void onAttached(TenantContextAttachedEvent event) {
        logger.trace("Setting current tenant in MDC to: {}", event.getTenantIdentifier());
        MDC.put(tenantIdentifierKey, event.getTenantIdentifier());
    }

    @EventListener
    void onClosed(TenantContextClosedEvent event) {
        logger.trace("Removing current tenant from MDC");
        MDC.remove(tenantIdentifierKey);
    }

    /**
     * Creates a new builder for {@link MdcTenantEventListener}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link MdcTenantEventListener}.
     */
    public static final class Builder {

        private String tenantIdentifierKey = DEFAULT_TENANT_IDENTIFIER_KEY;

        private Builder() {}

        /**
         * Name of the key to use for the tenant identifier in MDC.
         */
        public Builder tenantIdentifierKey(String tenantIdentifierKey) {
            this.tenantIdentifierKey = tenantIdentifierKey;
            return this;
        }

        /**
         * Builds the {@link MdcTenantEventListener} instance.
         */
        public MdcTenantEventListener build() {
            return new MdcTenantEventListener(tenantIdentifierKey);
        }

    }

}
