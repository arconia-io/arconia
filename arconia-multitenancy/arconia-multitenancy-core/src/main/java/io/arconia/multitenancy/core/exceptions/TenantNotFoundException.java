package io.arconia.multitenancy.core.exceptions;

import io.arconia.core.support.Incubating;

/**
 * Thrown when no tenant information is found in a given context.
 */
@Incubating
public class TenantNotFoundException extends IllegalStateException {

    public TenantNotFoundException() {
        super("No tenant found in the current context");
    }

    public TenantNotFoundException(String message) {
        super(message);
    }

    public TenantNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
