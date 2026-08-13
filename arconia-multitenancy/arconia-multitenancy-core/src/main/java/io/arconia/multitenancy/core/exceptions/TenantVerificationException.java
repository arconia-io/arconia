package io.arconia.multitenancy.core.exceptions;

import io.arconia.core.support.Incubating;

/**
 * Thrown when tenant verification fails.
 */
@Incubating
public class TenantVerificationException extends IllegalStateException {

    public TenantVerificationException() {
        super("Tenant verification failed");
    }

    public TenantVerificationException(String message) {
        super(message);
    }

    public TenantVerificationException(String message, Throwable cause) {
        super(message, cause);
    }

}
