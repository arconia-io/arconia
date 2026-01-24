package io.arconia.dev.services.keycloak;

import dasniko.testcontainers.keycloak.KeycloakContainer;

final class KeycloakDevServicesHolder {

    private static volatile KeycloakContainer container;

    static void set(KeycloakContainer c) {
        container = c;
    }

    static KeycloakContainer getContainer() {
        KeycloakContainer c = container;
        if (c == null) {
            throw new IllegalStateException(
                "Keycloak Dev Services container not initialized"
            );
        }
        return c;
    }

    static KeycloakContainer getIfPresent() {
        return container;
    }

    static void clear() {
        container = null;
    }


    private KeycloakDevServicesHolder() {}
}
