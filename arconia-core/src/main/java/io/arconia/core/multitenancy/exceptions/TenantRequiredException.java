package io.arconia.core.multitenancy.exceptions;

/**
 * Thrown when the source of the operation didn't provide any tenant information.
 *
 * @author Thomas Vitale
 */
public class TenantRequiredException extends IllegalStateException {

    public TenantRequiredException() {
        super("A tenant must be specified for the current operation");
    }

    public TenantRequiredException(String message) {
        super(message);
    }

}
