package io.arconia.dev.services.oidc;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

import io.arconia.core.support.Incubating;

/**
 * Details for connecting to an OpenID Connect provider dev service.
 * <p>
 * These describe the provider, not how an application authenticates against it: client
 * identifiers and secrets are application configuration, owned by each dev service.
 */
@Incubating
public interface OidcConnectionDetails extends ConnectionDetails {

    /**
     * The issuer URI, as it appears in the tokens the provider issues.
     */
    String getIssuerUri();

    /**
     * The URI of the JSON Web Key Set the provider signs its tokens with.
     */
    String getJwkSetUri();

}
