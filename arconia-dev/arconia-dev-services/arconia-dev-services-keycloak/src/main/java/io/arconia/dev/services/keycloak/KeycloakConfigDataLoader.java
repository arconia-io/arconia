package io.arconia.dev.services.keycloak;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.config.ConfigData;
import org.springframework.boot.context.config.ConfigDataLoader;
import org.springframework.boot.context.config.ConfigDataLoaderContext;
import org.springframework.core.env.MapPropertySource;

/**
 * Loads runtime Keycloak properties by starting a Testcontainer and returning
 * aO
 * ConfigData containing a MapPropertySource with the runtime values.
 */
public class KeycloakConfigDataLoader implements ConfigDataLoader<KeycloakConfigDataResource> {

    @Override
    public ConfigData load(ConfigDataLoaderContext context, KeycloakConfigDataResource resource) throws IOException {
        try {
            var properties = context.getBootstrapContext().get(KeycloakDevServicesProperties.class);

            /*
            var bootstrapContext = context.getBootstrapContext();

            KeycloakDevServicesProperties properties = null;
            if (bootstrapContext != null) {
                try {
                    Environment env = bootstrapContext.get(Environment.class);
                    if (env != null) {
                        properties = Binder.get(env)
                                .bind("arconia.dev.services.keycloak", KeycloakDevServicesProperties.class)
                                .orElseGet(KeycloakDevServicesProperties::new);
                    }
                } catch (Exception ignore) {
                    // fallback to defaults below
                }
            }
            if (properties == null) {
                properties = new KeycloakDevServicesProperties();
            }

            */

            if (Boolean.FALSE.equals(properties.isEnabled())) {
                return ConfigData.EMPTY;
            }

            synchronized (KeycloakConfigDataLoader.class) {
                var existing = KeycloakDevServicesHolder.getIfPresent();
                if (existing != null) {
                    MapPropertySource source = createPropertySource(existing, properties);
                    return new ConfigData(Collections.singletonList(source));
                }

                var container = KeycloakContainerFactory.create(properties);
                container.start();

                KeycloakDevServicesHolder.set(container);

                MapPropertySource source = createPropertySource(container, properties);
                return new ConfigData(Collections.singletonList(source));
            }

        } catch (Exception ex) {
            throw new IOException("Failed to load arconia-keycloak config data", ex);
        }
    }

    private static MapPropertySource createPropertySource(dasniko.testcontainers.keycloak.KeycloakContainer container,
            KeycloakDevServicesProperties properties) {
        String authServer = container.getAuthServerUrl();
        String issuer = authServer + "/realms/" + properties.getRealm();

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("arconia.dev.services.keycloak.server-url", authServer);
        props.put("spring.security.oauth2.resourceserver.jwt.issuer-uri", issuer);
        props.put("spring.security.oauth2.client.provider.keycloak.issuer-uri", issuer);

        return new MapPropertySource("arconia-keycloak-runtime", props);
    }
}
