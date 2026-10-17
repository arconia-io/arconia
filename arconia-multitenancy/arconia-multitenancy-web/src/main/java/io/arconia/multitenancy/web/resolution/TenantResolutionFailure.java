package io.arconia.multitenancy.web.resolution;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;

/**
 * Describes why no tenant context could be established for an HTTP request.
 *
 * @param outcome the kind of failure
 * @param message a description of the failure, safe to log and to return to the caller
 * @param cause the exception behind the failure, if any
 */
@Incubating
public record TenantResolutionFailure(Outcome outcome, String message, @Nullable Throwable cause) {

    public TenantResolutionFailure {
        Assert.notNull(outcome, "outcome cannot be null");
        Assert.hasText(message, "message cannot be null or empty");
    }

    /**
     * The kinds of tenant resolution failure.
     */
    public enum Outcome {

        /**
         * No resolver found a tenant identifier in the request.
         */
        MISSING,

        /**
         * A tenant identifier was present but unusable: it violates the identifier policy,
         * or the request carries conflicting or incomplete tenant information.
         */
        INVALID,

        /**
         * The tenant identifier is well-formed but was rejected by the tenant verifier,
         * for example, because the tenant is unknown or disabled.
         */
        REJECTED,

        /**
         * The tenant could not be verified because the tenant registry failed.
         */
        UNAVAILABLE

    }

}
