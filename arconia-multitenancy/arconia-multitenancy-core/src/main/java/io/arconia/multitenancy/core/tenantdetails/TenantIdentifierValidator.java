package io.arconia.multitenancy.core.tenantdetails;

import io.arconia.core.support.Incubating;
import io.arconia.multitenancy.core.exceptions.TenantVerificationException;

/**
 * Strategy for validating the syntax of a tenant identifier before any attempt is made
 * to load the corresponding tenant.
 */
@Incubating
@FunctionalInterface
public interface TenantIdentifierValidator {

    /**
     * Validates the syntax of the given tenant identifier.
     * @throws TenantVerificationException if the identifier is not valid
     */
    void validate(String tenantIdentifier);

}
