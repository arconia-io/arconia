package io.arconia.multitenancy.web.context.resolvers;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link OAuth2TenantResolver}.
 */
class OAuth2TenantResolverTests {

    private static final String TENANT_ID = "acme";

    private final OAuth2TenantResolver oauth2TenantResolver = OAuth2TenantResolver.builder().build();

    private final MockHttpServletRequest request = new MockHttpServletRequest();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void whenNullCustomClaimThenThrow() {
        assertThatThrownBy(() -> OAuth2TenantResolver.builder().tenantClaimName(null).build()).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantClaimName cannot be null or empty");
    }

    @Test
    void whenEmptyCustomClaimThenThrow() {
        assertThatThrownBy(() -> OAuth2TenantResolver.builder().tenantClaimName("").build()).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenantClaimName cannot be null or empty");
    }

    @Test
    void whenNullRequestThenThrow() {
        assertThatThrownBy(() -> oauth2TenantResolver.resolveTenantIdentifier(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("request cannot be null");
    }

    @Test
    void whenJwtResourceServerThenResolveFromAccessToken() {
        authenticate(new JwtAuthenticationToken(jwt(Map.of("tenant_id", TENANT_ID))));

        assertThat(oauth2TenantResolver.resolveTenantIdentifier(request)).isEqualTo(TENANT_ID);
    }

    @Test
    void whenOpaqueTokenResourceServerThenResolveFromIntrospectedPrincipal() {
        var attributes = Map.<String, Object>of("sub", "user", "tenant_id", TENANT_ID);
        var principal = new DefaultOAuth2AuthenticatedPrincipal(attributes, AuthorityUtils.NO_AUTHORITIES);
        var accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "token", Instant.now(),
                Instant.now().plusSeconds(60));
        authenticate(new BearerTokenAuthentication(principal, accessToken, AuthorityUtils.NO_AUTHORITIES));

        assertThat(oauth2TenantResolver.resolveTenantIdentifier(request)).isEqualTo(TENANT_ID);
    }

    @Test
    void whenOidcClientThenResolveFromIdToken() {
        var idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("sub", "user", "tenant_id", TENANT_ID));
        var oidcUser = new DefaultOidcUser(AuthorityUtils.NO_AUTHORITIES, idToken);
        authenticate(new OAuth2AuthenticationToken(oidcUser, List.of(), "keycloak"));

        assertThat(oauth2TenantResolver.resolveTenantIdentifier(request)).isEqualTo(TENANT_ID);
    }

    @Test
    void whenOAuth2ClientThenResolveFromUserAttributes() {
        var attributes = Map.<String, Object>of("sub", "user", "tenant_id", TENANT_ID);
        var oauth2User = new DefaultOAuth2User(AuthorityUtils.NO_AUTHORITIES, attributes, "sub");
        authenticate(new OAuth2AuthenticationToken(oauth2User, List.of(), "github"));

        assertThat(oauth2TenantResolver.resolveTenantIdentifier(request)).isEqualTo(TENANT_ID);
    }

    @Test
    void whenCustomClaimIsUsed() {
        var resolver = OAuth2TenantResolver.builder().tenantClaimName("https://arconia.io/tenant").build();
        authenticate(new JwtAuthenticationToken(jwt(Map.of("https://arconia.io/tenant", TENANT_ID))));

        assertThat(resolver.resolveTenantIdentifier(request)).isEqualTo(TENANT_ID);
    }

    @Test
    void whenPrincipalReplacedThenResolveFromCredentials() {
        var token = jwt(Map.of("tenant_id", TENANT_ID));
        authenticate(new JwtAuthenticationToken(token, "customPrincipal", AuthorityUtils.NO_AUTHORITIES));

        assertThat(oauth2TenantResolver.resolveTenantIdentifier(request)).isEqualTo(TENANT_ID);
    }

    @Test
    void whenNoAuthenticationThenReturnNull() {
        assertThat(oauth2TenantResolver.resolveTenantIdentifier(request)).isNull();
    }

    @Test
    void whenClaimMissingThenReturnNull() {
        authenticate(new JwtAuthenticationToken(jwt(Map.of("sub", "user"))));

        assertThat(oauth2TenantResolver.resolveTenantIdentifier(request)).isNull();
    }

    @Test
    void whenNotAnOAuth2PrincipalThenReturnNull() {
        authenticate(new TestingAuthenticationToken("user", "credentials"));

        assertThat(oauth2TenantResolver.resolveTenantIdentifier(request)).isNull();
    }

    private static Jwt jwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .claims(existing -> existing.putAll(claims))
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .build();
    }

    private static void authenticate(Authentication authentication) {
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

}
