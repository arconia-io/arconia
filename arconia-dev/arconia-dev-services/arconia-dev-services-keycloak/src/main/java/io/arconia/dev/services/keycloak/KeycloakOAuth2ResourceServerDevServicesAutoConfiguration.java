package io.arconia.dev.services.keycloak;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.oidc.OidcResourceServerPropertiesRegistrar;

/**
 * Auto-configuration wiring an OAuth2 Resource Server to the Keycloak Dev Service, through the
 * {@link OidcResourceServerPropertiesRegistrar} shared by the OpenID Connect dev services.
 */
@AutoConfiguration(after = KeycloakDevServicesAutoConfiguration.class,
        beforeName = "org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration")
@ConditionalOnDevServicesEnabled("keycloak")
@ConditionalOnClass(BearerTokenAuthenticationToken.class)
@ConditionalOnProperty(prefix = KeycloakDevServicesProperties.CONFIG_PREFIX + ".resource-server", name = "enabled",
        havingValue = "true", matchIfMissing = true)
@Import(OidcResourceServerPropertiesRegistrar.class)
public final class KeycloakOAuth2ResourceServerDevServicesAutoConfiguration {

}
