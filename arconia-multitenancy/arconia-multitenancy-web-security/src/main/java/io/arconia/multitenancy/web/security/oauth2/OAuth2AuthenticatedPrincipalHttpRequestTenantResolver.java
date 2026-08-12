package io.arconia.multitenancy.web.security.oauth2;

import io.arconia.multitenancy.web.context.resolvers.HttpRequestTenantResolver;

import jakarta.servlet.http.HttpServletRequest;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.util.Assert;

/**
 * Resolves the tenant identifier from a claim attribute in an OAuth token.
 *
 * Register an instance of this bean in either an OAuth client or an OAuth resource server.
 */
public class OAuth2AuthenticatedPrincipalHttpRequestTenantResolver
        implements HttpRequestTenantResolver {

    private final String tenantClaimName;

    public  OAuth2AuthenticatedPrincipalHttpRequestTenantResolver(String tenantClaimName) {
        Assert.notNull(tenantClaimName, "tenantClaimName cannot be null");
        this.tenantClaimName = tenantClaimName;
    }

    public OAuth2AuthenticatedPrincipalHttpRequestTenantResolver() {
        this("tenant");
    }

    @Override
    public @Nullable String resolveTenantIdentifier(HttpServletRequest request) {
        var auth = SecurityContextHolder
                .getContextHolderStrategy()
                .getContext()
                .getAuthentication();
        Assert.notNull(auth, "no authentication found");
        if (auth.getPrincipal() instanceof OAuth2AuthenticatedPrincipal oidcUser) {
            return (String) oidcUser.getAttributes().get(this.tenantClaimName);
        }
        throw new IllegalStateException("couldn't find a valid tenant");
    }
}
