package io.arconia.multitenancy.core.tenantdetails;

import io.arconia.core.support.Incubating;
import io.arconia.multitenancy.core.exceptions.TenantVerificationException;

/**
 * Strategy for verifying that a resolved tenant identifier is valid and allowed to
 * proceed.
 */
@Incubating
@FunctionalInterface
public interface TenantVerifier {

    /**
     * Verifies the given tenant identifier.
     * @throws TenantVerificationException if the tenant is invalid or disabled
     */
    void verify(String tenantIdentifier);

}
