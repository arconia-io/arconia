package io.arconia.multitenancy.core.resolution;

import io.arconia.core.support.Incubating;

/**
 * Thrown when a tenant identifier is present but unusable: it violates the identifier
 * policy enforced by a {@link TenantIdentifierValidator}, or the source it was resolved
 * from carries conflicting or incomplete tenant information.
 * <p>
 * The message may be logged and returned to the caller, so it should describe the rule
 * that was violated rather than embed the raw value.
 */
@Incubating
public class InvalidTenantIdentifierException extends TenantVerificationException {

    public InvalidTenantIdentifierException(String message) {
        super(message);
    }

    public InvalidTenantIdentifierException(String message, Throwable cause) {
        super(message, cause);
    }

}
