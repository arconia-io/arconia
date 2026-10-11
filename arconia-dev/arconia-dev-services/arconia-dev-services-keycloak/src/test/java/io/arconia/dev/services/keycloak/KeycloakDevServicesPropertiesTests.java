package io.arconia.dev.services.keycloak;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import io.arconia.dev.services.keycloak.KeycloakDevServicesProperties.Realm.User;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link KeycloakDevServicesProperties}: the configuration forms that rely on
 * conversion rather than plain binding.
 */
class KeycloakDevServicesPropertiesTests {

    private final KeycloakDevServicesProperties properties = new KeycloakDevServicesProperties();

    @Test
    void bindWhenSecretIsEmptyKeepsPublicClient() {
        bind(Map.of("client.secret", ""));

        assertThat(properties.getClient().getSecret()).isEmpty();
    }

    @Test
    void bindWhenUserGivenAsItsPasswordAloneUsesCompactForm() {
        // The compact form is what keeps a user that needs nothing but a password to one line.
        // It works because Spring's conversion service looks for a static of(String) factory
        // when a scalar has to become an object, which is a quiet dependency worth pinning.
        bind(Map.of("realm.users.alice", "s3cret"));

        User alice = properties.getRealm().getUsers().get("alice");
        assertThat(alice).isNotNull();
        assertThat(alice.getPassword()).isEqualTo("s3cret");
        // Left empty rather than guessed at: the factory derives them from the username.
        assertThat(alice.getFirstName()).isEmpty();
        assertThat(alice.getLastName()).isEmpty();
        assertThat(alice.getEmail()).isEmpty();
    }

    @Test
    void bindWhenUserGivenItsFullIdentityUsesIt() {
        Map<String, String> values = new HashMap<>();
        values.put("realm.users.bob.password", "hunter2");
        values.put("realm.users.bob.first-name", "Bjørn");
        values.put("realm.users.bob.last-name", "Vinterberg");
        values.put("realm.users.bob.email", "bjorn@example.com");

        bind(values);

        User bob = properties.getRealm().getUsers().get("bob");
        assertThat(bob).isNotNull();
        assertThat(bob.getPassword()).isEqualTo("hunter2");
        assertThat(bob.getFirstName()).isEqualTo("Bjørn");
        assertThat(bob.getLastName()).isEqualTo("Vinterberg");
        assertThat(bob.getEmail()).isEqualTo("bjorn@example.com");
    }

    @Test
    void bindWhenBothUserFormsTogetherBindsEach() {
        // Mixing the two forms in one map is the case that would break if the compact form were
        // handled by anything other than conversion, since the binder decides per entry.
        Map<String, String> values = new HashMap<>();
        values.put("realm.users.alice", "s3cret");
        values.put("realm.users.bob.password", "hunter2");
        values.put("realm.users.bob.email", "bjorn@example.com");

        bind(values);

        assertThat(properties.getRealm().getUsers()).hasSize(2);
        assertThat(properties.getRealm().getUsers().get("alice").getPassword()).isEqualTo("s3cret");
        assertThat(properties.getRealm().getUsers().get("alice").getEmail()).isEmpty();
        assertThat(properties.getRealm().getUsers().get("bob").getEmail()).isEqualTo("bjorn@example.com");
    }

    private void bind(Map<String, String> values) {
        new Binder(new MapConfigurationPropertySource(values)).bind("", Bindable.ofInstance(properties));
    }

}
