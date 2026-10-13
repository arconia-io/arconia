package io.arconia.dev.services.keycloak;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.keycloak.KeycloakOAuth2ResourceServerDevServicesAutoConfiguration.KeycloakResourceServerPropertyRegistrar;

/**
 * Auto-configuration wiring an OAuth2 Resource Server to the Keycloak Dev Service.
 * <p>
 * The wiring is expressed as properties. Every property describing the tokens the dev service issues
 * is overridden, since a value left over from the application's own configuration would describe
 * a different issuer. The properties describing how the application interprets a token,
 * such as the claim holding its authorities, are left alone.
 */
@AutoConfiguration(after = KeycloakDevServicesAutoConfiguration.class,
        beforeName = "org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration")
@ConditionalOnDevServicesEnabled("keycloak")
@ConditionalOnClass(BearerTokenAuthenticationToken.class)
@ConditionalOnProperty(prefix = KeycloakDevServicesProperties.CONFIG_PREFIX + ".resource-server", name = "enabled",
        havingValue = "true", matchIfMissing = true)
@Import(KeycloakResourceServerPropertyRegistrar.class)
public final class KeycloakOAuth2ResourceServerDevServicesAutoConfiguration {

    private static final String JWT_PROPERTY_PREFIX = "spring.security.oauth2.resourceserver.jwt.";

    /**
     * The algorithm Keycloak signs the tokens of a realm with, unless the realm says otherwise.
     */
    private static final String KEYCLOAK_DEFAULT_SIGNATURE_ALGORITHM = "RS256";

    static class KeycloakResourceServerPropertyRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            addDynamicProperty(JWT_PROPERTY_PREFIX + "issuer-uri", () -> connectionDetails().getIssuerUri());
            addDynamicProperty(JWT_PROPERTY_PREFIX + "jwk-set-uri", () -> connectionDetails().getJwkSetUri());
            addDynamicProperty(JWT_PROPERTY_PREFIX + "jws-algorithms", () -> KEYCLOAK_DEFAULT_SIGNATURE_ALGORITHM);
            // Cleared rather than left alone: the tokens issued by the dev service cannot carry
            // the audience an application expects in production, so validating it would reject
            // every one of them. An empty value takes precedence over a list configured elsewhere.
            addDynamicProperty(JWT_PROPERTY_PREFIX + "audiences", () -> "");
        }

        private KeycloakConnectionDetails connectionDetails() {
            return getBeanFactory().getBean(KeycloakConnectionDetails.class);
        }

    }

}
