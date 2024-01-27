package io.arconia.core.multitenancy.exceptions;

/**
 * Thrown when the source of the operation didn't provide any tenant information.
 *
 * @author Thomas Vitale
 */
public class TenantResolutionException extends IllegalStateException {

    public TenantResolutionException() {
        super("A tenant must be specified for the current operation");
    }

    public TenantResolutionException(String message) {
        super(message);
    }

}
