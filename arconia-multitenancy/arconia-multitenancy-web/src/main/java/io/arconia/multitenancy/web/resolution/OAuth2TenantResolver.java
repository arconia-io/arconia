package io.arconia.multitenancy.web.resolution;

import jakarta.servlet.http.HttpServletRequest;

import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.oauth2.core.ClaimAccessor;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;
import io.arconia.multitenancy.core.resolution.InvalidTenantIdentifierException;

/**
 * Strategy used to resolve the current tenant from a claim in the OAuth2 token that
 * authenticated the current HTTP request.
 * <p>
 * It requires the request to be authenticated already, so the filter resolving the
 * current tenant must run after the Spring Security filter chain. An unauthenticated
 * request, or one authenticated by other means than OAuth2, resolves no tenant so that
 * another resolver may be consulted. A request authenticated with an OAuth2 token that
 * does not carry the claim is rejected so that the tenant never falls back to a less
 * trusted source once a token has been inspected.
 */
@Incubating
public final class OAuth2TenantResolver implements HttpRequestTenantResolver {

    private static final String DEFAULT_CLAIM_NAME = "tenant_id";

    private final String tenantClaimName;

    private final SecurityContextHolderStrategy securityContextHolderStrategy;

    private final AuthenticationTrustResolver authenticationTrustResolver = new AuthenticationTrustResolverImpl();

    private OAuth2TenantResolver(String tenantClaimName, SecurityContextHolderStrategy securityContextHolderStrategy) {
        Assert.hasText(tenantClaimName, "tenantClaimName cannot be null or empty");
        Assert.notNull(securityContextHolderStrategy, "securityContextHolderStrategy cannot be null");
        this.tenantClaimName = tenantClaimName;
        this.securityContextHolderStrategy = securityContextHolderStrategy;
    }

    /**
     * Name of the OAuth2 token claim from which the current tenant is resolved.
     */
    public String getTenantClaimName() {
        return tenantClaimName;
    }

    @Override
    @Nullable
    public String resolveTenantIdentifier(HttpServletRequest request) {
        Assert.notNull(request, "request cannot be null");
        var authentication = securityContextHolderStrategy.getContext().getAuthentication();
        if (!authenticationTrustResolver.isAuthenticated(authentication)) {
            return null;
        }
        var principal = authentication.getPrincipal();
        var credentials = authentication.getCredentials();
        var claim = extractClaim(principal);
        if (claim == null) {
            claim = extractClaim(credentials);
        }
        if (claim != null) {
            return claim;
        }
        if (isOAuth2(principal) || isOAuth2(credentials)) {
            throw new InvalidTenantIdentifierException(
                    "The OAuth2 token must contain a '%s' claim".formatted(tenantClaimName));
        }
        return null;
    }

    /**
     * Extracts the tenant claim from either an OAuth2 token or an authenticated
     * principal. A JWT in a resource server and an OIDC user in a client application are
     * both {@link ClaimAccessor}, whereas an OAuth2 user and the principal introspected
     * from an opaque token are {@link OAuth2AuthenticatedPrincipal}.
     */
    @Nullable
    private String extractClaim(@Nullable Object candidate) {
        if (candidate instanceof ClaimAccessor claimAccessor) {
            return claimAccessor.getClaimAsString(tenantClaimName);
        }
        if (candidate instanceof OAuth2AuthenticatedPrincipal principal) {
            Object value = principal.getAttribute(tenantClaimName);
            return value != null ? value.toString() : null;
        }
        return null;
    }

    private static boolean isOAuth2(@Nullable Object candidate) {
        return candidate instanceof ClaimAccessor || candidate instanceof OAuth2AuthenticatedPrincipal;
    }

    /**
     * Creates a new builder for {@link OAuth2TenantResolver}.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String tenantClaimName = DEFAULT_CLAIM_NAME;

        private SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder
            .getContextHolderStrategy();

        private Builder() {}

        /**
         * Name of the OAuth2 token claim from which to resolve the current tenant.
         */
        public Builder tenantClaimName(String tenantClaimName) {
            this.tenantClaimName = tenantClaimName;
            return this;
        }

        /**
         * Strategy giving access to the security context of the current request.
         */
        public Builder securityContextHolderStrategy(SecurityContextHolderStrategy securityContextHolderStrategy) {
            this.securityContextHolderStrategy = securityContextHolderStrategy;
            return this;
        }

        /**
         * Builds the {@link OAuth2TenantResolver} instance.
         */
        public OAuth2TenantResolver build() {
            return new OAuth2TenantResolver(tenantClaimName, securityContextHolderStrategy);
        }

    }

}
