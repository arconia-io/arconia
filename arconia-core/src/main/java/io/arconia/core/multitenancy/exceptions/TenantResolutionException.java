package io.arconia.core.multitenancy.exceptions;

/**
 * Thrown when an error occurred during the tenant resolution process.
 *
 * @author Thomas Vitale
 */
public class TenantResolutionException extends IllegalStateException {

    public TenantResolutionException() {
        super("Error when trying to resolve the current tenant");
    }

    public TenantResolutionException(String message) {
        super(message);
    }

}
