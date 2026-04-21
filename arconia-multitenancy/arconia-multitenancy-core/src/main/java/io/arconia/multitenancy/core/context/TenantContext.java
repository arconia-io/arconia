package io.arconia.multitenancy.core.context;

import org.jspecify.annotations.Nullable;

import io.arconia.core.support.Incubating;
import io.arconia.multitenancy.core.exceptions.TenantNotFoundException;

/**
 * Provides access to the current tenant identifier via a {@link ScopedValue}.
 *
 * <p>
 * The tenant context is established by binding a value through {@link #where(String)}
 * and executing code within that scope:
 *
 * <pre>{@code
 * TenantContext.where("acme").run(() -> {
 *     // TenantContext.getTenantIdentifier() returns "acme"
 * });
 * }</pre>
 *
 * <p>
 * The binding is automatically removed when the scope exits.
 */
@Incubating
public final class TenantContext {

    private static final ScopedValue<String> TENANT_IDENTIFIER = ScopedValue.newInstance();

    private TenantContext() {}

    /**
     * Creates a {@link ScopedValue.Carrier} that binds the given tenant identifier for
     * the duration of a {@code run()} or {@code call()} scope.
     */
    public static ScopedValue.Carrier where(String tenantIdentifier) {
        return ScopedValue.where(TENANT_IDENTIFIER, tenantIdentifier);
    }

    /**
     * Returns the tenant identifier bound in the current scope, or {@code null} if no
     * tenant is bound.
     */
    @Nullable
    public static String getTenantIdentifier() {
        return TENANT_IDENTIFIER.isBound() ? TENANT_IDENTIFIER.get() : null;
    }

    /**
     * Returns the tenant identifier bound in the current scope.
     * @throws TenantNotFoundException if no tenant is bound
     */
    public static String getRequiredTenantIdentifier() {
        return TENANT_IDENTIFIER
            .orElseThrow(() -> new TenantNotFoundException("No tenant found in the current context"));
    }

}
