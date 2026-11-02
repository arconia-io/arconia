package io.arconia.multitenancy.core.context;

import org.jspecify.annotations.Nullable;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.util.Assert;

import io.arconia.core.annotations.Incubating;

/**
 * A {@link TaskDecorator} that propagates the tenant context to the thread running a
 * task.
 * <p>
 * The tenant bound when the task is submitted is bound again on the thread running it.
 * When an MDC key name is configured, the tenant identifier is also put in the SLF4J
 * {@link MDC} for the duration of the task, and the previous value is restored
 * afterwards. A task submitted with no tenant bound runs unchanged.
 */
@Incubating
public final class TenantContextPropagatingTaskDecorator implements TaskDecorator {

    @Nullable
    private final String mdcKeyName;

    private TenantContextPropagatingTaskDecorator(@Nullable String mdcKeyName) {
        Assert.isTrue(mdcKeyName == null || !mdcKeyName.isBlank(), "mdcKeyName cannot be empty");
        this.mdcKeyName = mdcKeyName;
    }

    /**
     * Name of the MDC key holding the tenant identifier while a task runs, or
     * {@code null} when the MDC is left untouched.
     */
    @Nullable
    public String getMdcKeyName() {
        return mdcKeyName;
    }

    @Override
    public Runnable decorate(Runnable runnable) {
        var tenantIdentifier = TenantContext.getTenantIdentifier();
        if (tenantIdentifier == null) {
            return runnable;
        }
        var mdcKeyName = this.mdcKeyName;
        Runnable task = mdcKeyName != null ? () -> runWithMdc(runnable, mdcKeyName, tenantIdentifier) : runnable;
        return () -> TenantContext.where(tenantIdentifier).run(task);
    }

    /**
     * Restores the previous value rather than removing the key, so that a task running
     * on the submitting thread, as with a caller-runs rejection policy, leaves the MDC of
     * the enclosing scope intact.
     */
    private static void runWithMdc(Runnable runnable, String mdcKeyName, String tenantIdentifier) {
        var previous = MDC.get(mdcKeyName);
        MDC.put(mdcKeyName, tenantIdentifier);
        try {
            runnable.run();
        } finally {
            if (previous != null) {
                MDC.put(mdcKeyName, previous);
            } else {
                MDC.remove(mdcKeyName);
            }
        }
    }

    /**
     * Creates a new builder for {@link TenantContextPropagatingTaskDecorator}.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        @Nullable
        private String mdcKeyName;

        private Builder() {}

        /**
         * Name of the MDC key holding the tenant identifier while a task runs. When not
         * set, the MDC is left untouched.
         */
        public Builder mdcKeyName(@Nullable String mdcKeyName) {
            this.mdcKeyName = mdcKeyName;
            return this;
        }

        /**
         * Builds the {@link TenantContextPropagatingTaskDecorator} instance.
         */
        public TenantContextPropagatingTaskDecorator build() {
            return new TenantContextPropagatingTaskDecorator(mdcKeyName);
        }

    }

}
