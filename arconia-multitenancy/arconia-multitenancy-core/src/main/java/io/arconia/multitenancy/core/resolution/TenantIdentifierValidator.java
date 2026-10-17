package io.arconia.multitenancy.core.resolution;

import io.arconia.core.support.Incubating;

/**
 * Strategy for validating the syntax of a tenant identifier before any attempt is made
 * to load the corresponding tenant.
 */
@Incubating
@FunctionalInterface
public interface TenantIdentifierValidator {

    /**
     * Validates the syntax of the given tenant identifier.
     * @throws InvalidTenantIdentifierException if the identifier is not valid
     */
    void validate(String tenantIdentifier);

}
