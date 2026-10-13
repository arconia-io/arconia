package io.arconia.dev.services.oidc;

import org.springframework.core.env.Environment;

import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;

/**
 * Wires an OAuth2 Resource Server to the OpenID Connect provider described by the
 * {@link OidcConnectionDetails} bean, for a dev service to import from its own auto-configuration.
 * <p>
 * Spring Boot configures the resource server from properties only, so the wiring is expressed as
 * properties too. With {@code ConnectionDetails} support in Spring Boot's resource server
 * auto-configuration, all of this would reduce to publishing the connection details bean.
 */
public class OidcResourceServerPropertiesRegistrar extends DevServicesRegistrar {

    static final String JWT_PROPERTY_PREFIX = "spring.security.oauth2.resourceserver.jwt.";

    /**
     * The algorithm the supported providers sign their tokens with by default.
     */
    private static final String DEFAULT_SIGNATURE_ALGORITHM = "RS256";

    @Override
    protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
        addDynamicProperty(JWT_PROPERTY_PREFIX + "issuer-uri", () -> connectionDetails().getIssuerUri());
        addDynamicProperty(JWT_PROPERTY_PREFIX + "jwk-set-uri", () -> connectionDetails().getJwkSetUri());
        addDynamicProperty(JWT_PROPERTY_PREFIX + "jws-algorithms", () -> DEFAULT_SIGNATURE_ALGORITHM);
        // Cleared rather than left alone: the tokens issued by the dev service cannot carry the
        // audience an application expects in production, so validating it would reject every one
        // of them. An empty value takes precedence over a list configured elsewhere.
        addDynamicProperty(JWT_PROPERTY_PREFIX + "audiences", () -> "");
    }

    private OidcConnectionDetails connectionDetails() {
        return getBeanFactory().getBean(OidcConnectionDetails.class);
    }

}
