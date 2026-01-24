package io.arconia.dev.services.keycloak;

import java.util.Collections;
import java.util.List;

import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataLocationResolver;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;

/**
 * Resolves the logical `arconia-keycloak:` config-data location into a
 * {@link KeycloakConfigDataResource} so the loader can start Keycloak
 * and contribute runtime properties.
 */
public class KeycloakConfigDataLocationResolver implements ConfigDataLocationResolver<KeycloakConfigDataResource> {

    private static final String PREFIX = "arconia-keycloak:";

    @Override
    public boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
        return location != null && location.getValue() != null && location.getValue().startsWith(PREFIX);
    }

    @Override
    public List<KeycloakConfigDataResource> resolve(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
        boolean optional = location.isOptional();
        return Collections.singletonList(new KeycloakConfigDataResource(optional));
    }

    @Override
    public List<KeycloakConfigDataResource> resolveProfileSpecific(ConfigDataLocationResolverContext context, ConfigDataLocation location, org.springframework.boot.context.config.Profiles profiles) {
        return resolve(context, location);
    }

}
