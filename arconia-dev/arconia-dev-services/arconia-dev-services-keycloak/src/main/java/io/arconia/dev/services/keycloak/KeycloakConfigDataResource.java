package io.arconia.dev.services.keycloak;

import org.springframework.boot.context.config.ConfigDataResource;

import java.util.Objects;

public final class KeycloakConfigDataResource extends ConfigDataResource {

    private final boolean optional;

    public KeycloakConfigDataResource() {
        this(false);
    }

    public KeycloakConfigDataResource(boolean optional) {
        super(optional);
        this.optional = optional;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KeycloakConfigDataResource that = (KeycloakConfigDataResource) o;
        return optional == that.optional;
    }

    @Override
    public int hashCode() {
        return Objects.hash(optional);
    }

    @Override
    public String toString() {
        return "ArconiaKeycloakConfigDataResource[optional=" + optional + "]";
    }

}
