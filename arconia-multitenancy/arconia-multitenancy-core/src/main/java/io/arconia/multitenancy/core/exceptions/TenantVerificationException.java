package io.arconia.multitenancy.core.exceptions;

/**
 * Thrown when tenant verification fails.
 */
public class TenantVerificationException extends IllegalStateException {

    public TenantVerificationException() {
        super("Tenant verification failed");
    }

    public TenantVerificationException(String message) {
        super(message);
    }

}
