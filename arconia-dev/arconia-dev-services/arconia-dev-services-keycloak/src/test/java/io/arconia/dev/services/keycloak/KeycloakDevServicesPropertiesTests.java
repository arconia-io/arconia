package io.arconia.dev.services.keycloak;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.keycloak.KeycloakDevServicesProperties.Realm.User;
import io.arconia.dev.services.tests.BaseDevServicesPropertiesTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Unit tests for {@link KeycloakDevServicesProperties}.
 */
class KeycloakDevServicesPropertiesTests extends BaseDevServicesPropertiesTests<KeycloakDevServicesProperties> {

    @Override
    protected KeycloakDevServicesProperties createProperties() {
        return new KeycloakDevServicesProperties();
    }

    @Override
    protected DefaultValues getExpectedDefaults() {
        return DefaultValues.builder()
                .imageName(ArconiaKeycloakContainer.COMPATIBLE_IMAGE_NAME)
                .shared(true)
                .startupTimeout(Duration.ofMinutes(2))
                .build();
    }

    @Test
    void shouldCreateInstanceWithKeycloakDefaultValues() {
        KeycloakDevServicesProperties properties = new KeycloakDevServicesProperties();

        assertThat(properties.getAdminUsername()).isEqualTo("arconia");
        assertThat(properties.getAdminPassword()).isEqualTo("arconia");
        assertThat(properties.getRealm().getImportPaths()).isEmpty();
        assertThat(properties.getRealm().getName()).isEmpty();
        assertThat(properties.getRealm().isCreate()).isTrue();
        assertThat(properties.getRealm().getUsers()).isEmpty();
        assertThat(properties.getRealm().getRoles()).isEmpty();
        assertThat(properties.getHostAccessiblePorts()).isEmpty();
    }

    @Test
    void shouldBindKeycloakProperties() {
        KeycloakDevServicesProperties properties = new KeycloakDevServicesProperties();

        Map<String, String> values = new HashMap<>();
        values.put("admin-username", "keycloak");
        values.put("admin-password", "s3cret");
        values.put("realm.import-paths[0]", "classpath:keycloak/realm.json");
        values.put("realm.import-paths[1]", "file:/tmp/other-realm.json");
        values.put("realm.name", "my-realm");
        values.put("realm.create", "false");
        values.put("realm.users.alice", "s3cret");
        values.put("realm.users.bob.password", "hunter2");
        values.put("realm.roles.editor[0]", "alice");
        values.put("realm.roles.reader[0]", "alice");
        values.put("realm.roles.reader[1]", "bob");
        values.put("host-accessible-ports[0]", "8080");

        bind(properties, values);

        assertThat(properties.getAdminUsername()).isEqualTo("keycloak");
        assertThat(properties.getAdminPassword()).isEqualTo("s3cret");
        assertThat(properties.getRealm().getImportPaths())
                .containsExactly("classpath:keycloak/realm.json", "file:/tmp/other-realm.json");
        assertThat(properties.getRealm().getName()).isEqualTo("my-realm");
        assertThat(properties.getRealm().isCreate()).isFalse();
        assertThat(properties.getRealm().getUsers()).hasSize(2);
        assertThat(properties.getRealm().getUsers().get("alice").getPassword()).isEqualTo("s3cret");
        assertThat(properties.getRealm().getUsers().get("bob").getPassword()).isEqualTo("hunter2");
        assertThat(properties.getRealm().getRoles())
                .containsOnly(entry("editor", List.of("alice")), entry("reader", List.of("alice", "bob")));
        assertThat(properties.getHostAccessiblePorts()).containsExactly(8080);
    }

    @Test
    void shouldCreateClientInstanceWithDefaultValues() {
        KeycloakDevServicesProperties.Client client = new KeycloakDevServicesProperties().getClient();

        assertThat(client.isEnabled()).isTrue();
        assertThat(client.getId()).isEqualTo("arconia-app");
        assertThat(client.getSecret()).isEqualTo("arconia-secret");
        assertThat(client.getRegistrationId()).isEqualTo("keycloak");
        assertThat(client.getScopes()).containsExactly("openid", "profile", "email");
    }

    @Test
    void shouldBindClientProperties() {
        KeycloakDevServicesProperties properties = new KeycloakDevServicesProperties();

        Map<String, String> values = new HashMap<>();
        values.put("client.enabled", "false");
        values.put("client.id", "my-app");
        values.put("client.secret", "my-secret");
        values.put("client.registration-id", "my-registration");
        values.put("client.scopes[0]", "openid");
        values.put("client.scopes[1]", "custom:scope");

        bind(properties, values);

        assertThat(properties.getClient().isEnabled()).isFalse();
        assertThat(properties.getClient().getId()).isEqualTo("my-app");
        assertThat(properties.getClient().getSecret()).isEqualTo("my-secret");
        assertThat(properties.getClient().getRegistrationId()).isEqualTo("my-registration");
        assertThat(properties.getClient().getScopes()).containsExactly("openid", "custom:scope");
    }

    @Test
    void shouldBindEmptySecretForPublicClient() {
        KeycloakDevServicesProperties properties = new KeycloakDevServicesProperties();

        bind(properties, Map.of("client.secret", ""));

        assertThat(properties.getClient().getSecret()).isEmpty();
    }

    @Test
    void shouldBindAUserGivenAsItsPasswordAlone() {
        // The compact form is what keeps a user that needs nothing but a password to one line.
        // It works because Spring's conversion service looks for a static of(String) factory
        // when a scalar has to become an object, which is a quiet dependency worth pinning.
        KeycloakDevServicesProperties properties = new KeycloakDevServicesProperties();

        bind(properties, Map.of("realm.users.alice", "s3cret"));

        User alice = properties.getRealm().getUsers().get("alice");
        assertThat(alice).isNotNull();
        assertThat(alice.getPassword()).isEqualTo("s3cret");
        // Left empty rather than guessed at: the factory derives them from the username.
        assertThat(alice.getFirstName()).isEmpty();
        assertThat(alice.getLastName()).isEmpty();
        assertThat(alice.getEmail()).isEmpty();
    }

    @Test
    void shouldBindAUserGivenItsFullIdentity() {
        KeycloakDevServicesProperties properties = new KeycloakDevServicesProperties();

        Map<String, String> values = new HashMap<>();
        values.put("realm.users.bob.password", "hunter2");
        values.put("realm.users.bob.first-name", "Bjørn");
        values.put("realm.users.bob.last-name", "Vinterberg");
        values.put("realm.users.bob.email", "bjorn@example.com");

        bind(properties, values);

        User bob = properties.getRealm().getUsers().get("bob");
        assertThat(bob).isNotNull();
        assertThat(bob.getPassword()).isEqualTo("hunter2");
        assertThat(bob.getFirstName()).isEqualTo("Bjørn");
        assertThat(bob.getLastName()).isEqualTo("Vinterberg");
        assertThat(bob.getEmail()).isEqualTo("bjorn@example.com");
    }

    @Test
    void shouldBindBothUserFormsTogether() {
        // Mixing the two forms in one map is the case that would break if the compact form were
        // handled by anything other than conversion, since the binder decides per entry.
        KeycloakDevServicesProperties properties = new KeycloakDevServicesProperties();

        Map<String, String> values = new HashMap<>();
        values.put("realm.users.alice", "s3cret");
        values.put("realm.users.bob.password", "hunter2");
        values.put("realm.users.bob.email", "bjorn@example.com");

        bind(properties, values);

        assertThat(properties.getRealm().getUsers()).hasSize(2);
        assertThat(properties.getRealm().getUsers().get("alice").getPassword()).isEqualTo("s3cret");
        assertThat(properties.getRealm().getUsers().get("alice").getEmail()).isEmpty();
        assertThat(properties.getRealm().getUsers().get("bob").getEmail()).isEqualTo("bjorn@example.com");
    }

}
