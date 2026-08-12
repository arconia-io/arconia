package io.arconia.multitenancy.web.context.resolvers;

import jakarta.servlet.http.HttpServletRequest;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.oauth2.core.ClaimAccessor;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;

/**
 * Strategy used to resolve the current tenant from a claim in the OAuth2 token that
 * authenticated the current HTTP request.
 *
 * It requires the request to be authenticated already, so the filter resolving the
 * current tenant must run after the Spring Security filter chain.
 */
@Incubating
public final class OAuth2TenantResolver implements HttpRequestTenantResolver {

    public static final String DEFAULT_CLAIM_NAME = "tenant_id";

    private final String tenantClaimName;

    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder
        .getContextHolderStrategy();

    public OAuth2TenantResolver() {
        this.tenantClaimName = DEFAULT_CLAIM_NAME;
    }

    public OAuth2TenantResolver(String tenantClaimName) {
        Assert.hasText(tenantClaimName, "tenantClaimName cannot be null or empty");
        this.tenantClaimName = tenantClaimName;
    }

    @Override
    @Nullable
    public String resolveTenantIdentifier(HttpServletRequest request) {
        Assert.notNull(request, "request cannot be null");
        var authentication = securityContextHolderStrategy.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        var claim = extractClaim(authentication.getPrincipal());
        return claim != null ? claim : extractClaim(authentication.getCredentials());
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

}
