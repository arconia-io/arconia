package io.arconia.dev.services.keycloak;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.util.JsonSerialization;

import org.springframework.boot.json.JsonParserFactory;

import io.arconia.dev.services.keycloak.KeycloakDevServicesProperties.Realm.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Unit tests for {@link KeycloakRealmFactory}.
 */
class KeycloakRealmFactoryTests {

    private static RealmRepresentation generate(KeycloakDevServicesProperties properties) {
        try {
            return JsonSerialization.readValue(
                    KeycloakRealmFactory.generateRealmJson(properties), RealmRepresentation.class);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static Map<String,User> users(String username, String password, String... roles) {
        Map<String,User> users = new LinkedHashMap<>();
        users.put(username, configuredUser(password, roles));
        return users;
    }

    private static User configuredUser(String password, String... roles) {
        User user = new User();
        user.setPassword(password);
        user.setRoles(List.of(roles));
        return user;
    }

    private static UserRepresentation user(RealmRepresentation realm, String username) {
        return realm.getUsers().stream()
                .filter(user -> username.equals(user.getUsername()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no user '%s' in the generated realm".formatted(username)));
    }

    @Test
    void shouldGenerateTheDefaultRealm() {
        RealmRepresentation realm = generate(new KeycloakDevServicesProperties());

        assertThat(realm.getRealm()).isEqualTo(KeycloakDevServicesProperties.DEFAULT_REALM);
        assertThat(realm.isEnabled()).isTrue();
        assertThat(realm.getSslRequired()).isEqualTo("none");
    }

    @Test
    void shouldUseTheConfiguredRealmName() {
        var properties = new KeycloakDevServicesProperties();
        properties.getRealm().setName("my-realm");

        assertThat(generate(properties).getRealm()).isEqualTo("my-realm");
    }

    @Test
    void shouldFailWhenTheRealmIsNamedMaster() {
        var properties = new KeycloakDevServicesProperties();
        properties.getRealm().setName("master");

        assertThatThrownBy(() -> KeycloakRealmFactory.generateRealmJson(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("'master'")
                .hasMessageContaining(KeycloakDevServicesProperties.CONFIG_PREFIX + ".realm.name");
    }

    @Test
    void shouldCreateTheDefaultUsersAndRoles() {
        RealmRepresentation realm = generate(new KeycloakDevServicesProperties());

        assertThat(realm.getUsers()).extracting(UserRepresentation::getUsername).containsExactly("isabella", "bjorn");
        assertThat(realm.getRoles().getRealm()).extracting(RoleRepresentation::getName).containsExactly("admin", "user");
        assertThat(user(realm, "isabella").getRealmRoles()).containsExactly("admin", "user");
        assertThat(user(realm, "bjorn").getRealmRoles()).containsExactly("user");
    }

    @Test
    void shouldGenerateTheSameDocumentEveryTime() {
        String first = KeycloakRealmFactory.generateRealmJson(new KeycloakDevServicesProperties());
        String second = KeycloakRealmFactory.generateRealmJson(new KeycloakDevServicesProperties());

        assertThat(second).isEqualTo(first);
    }

    @Test
    void shouldDeriveAFullIdentityFromTheUsername() {
        var properties = new KeycloakDevServicesProperties();
        properties.getRealm().setUsers(users("alice.smith", "s3cret"));

        assertThat(generate(properties).getUsers())
                .extracting(UserRepresentation::getUsername, UserRepresentation::getFirstName,
                        UserRepresentation::getLastName, UserRepresentation::getEmail, UserRepresentation::isEmailVerified)
                .containsExactly(tuple("alice.smith", "Alice.smith", "Alice.smith", "alice.smith@arconia.io", true));
    }

    @Test
    void shouldGiveEachUserANonTemporaryPassword() {
        var properties = new KeycloakDevServicesProperties();
        properties.getRealm().setUsers(users("alice", "s3cret"));

        assertThat(user(generate(properties), "alice").getCredentials()).singleElement().satisfies(credential -> {
            assertThat(credential.getType()).isEqualTo(CredentialRepresentation.PASSWORD);
            assertThat(credential.getValue()).isEqualTo("s3cret");
            assertThat(credential.isTemporary()).isFalse();
        });
    }

    @Test
    void shouldReplaceTheDefaultUsersAndRolesWithTheConfiguredOnes() {
        var properties = new KeycloakDevServicesProperties();
        properties.getRealm().setUsers(users("alice", "alice"));

        RealmRepresentation realm = generate(properties);

        assertThat(realm.getUsers()).extracting(UserRepresentation::getUsername).containsExactly("alice");
        assertThat(realm.getRoles().getRealm()).isEmpty();
        assertThat(user(realm, "alice").getRealmRoles()).isEmpty();
    }

    @Test
    void shouldCreateTheRolesHeldByTheUsers() {
        var properties = new KeycloakDevServicesProperties();
        var users = new LinkedHashMap<String,User>();
        users.put("alice", configuredUser("alice", "editor", "reader"));
        users.put("bob", configuredUser("bob", "reader"));
        properties.getRealm().setUsers(users);

        RealmRepresentation realm = generate(properties);

        assertThat(realm.getRoles().getRealm())
                .extracting(RoleRepresentation::getName)
                .containsExactly("editor", "reader");
        assertThat(user(realm, "alice").getRealmRoles()).containsExactly("editor", "reader");
        assertThat(user(realm, "bob").getRealmRoles()).containsExactly("reader");
    }

    @Test
    void shouldFailWhenARoleIsEmpty() {
        var properties = new KeycloakDevServicesProperties();
        properties.getRealm().setUsers(users("alice", "alice", ""));

        assertThatThrownBy(() -> KeycloakRealmFactory.generateRealmJson(properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("alice");
    }

    @Test
    void shouldFailWhenAUsernameIsNotLowerCase() {
        // Keycloak stores usernames in lower case, and the username is also the local part of
        // the derived email address.
        var properties = new KeycloakDevServicesProperties();
        properties.getRealm().setUsers(users("Alice", "s3cret"));

        assertThatThrownBy(() -> KeycloakRealmFactory.generateRealmJson(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("'Alice'")
                .hasMessageContaining(KeycloakDevServicesProperties.CONFIG_PREFIX + ".realm.users");
    }

    @Test
    void shouldFailWhenAUserHasNoPassword() {
        var properties = new KeycloakDevServicesProperties();
        properties.getRealm().setUsers(users("alice", ""));

        assertThatThrownBy(() -> KeycloakRealmFactory.generateRealmJson(properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(KeycloakDevServicesProperties.CONFIG_PREFIX + ".realm.users.alice.password");
    }

    @Test
    void shouldCreateAConfidentialClient() {
        RealmRepresentation realm = generate(new KeycloakDevServicesProperties());

        assertThat(realm.getClients()).singleElement().satisfies(client -> {
            assertThat(client.getClientId()).isEqualTo(KeycloakDevServicesProperties.Client.DEFAULT_CLIENT_ID);
            assertThat(client.getSecret()).isEqualTo(KeycloakDevServicesProperties.Client.DEFAULT_CLIENT_SECRET);
            assertThat(client.isPublicClient()).isFalse();
            assertThat(client.isServiceAccountsEnabled()).isTrue();
            assertThat(client.isDirectAccessGrantsEnabled()).isTrue();
            assertThat(client.getRedirectUris()).containsExactly("*");
            // Full scope is deprecated by Keycloak.
            assertThat(client.isFullScopeAllowed()).isFalse();
        });
    }

    @Test
    void shouldGrantTheClientTheRealmRoles() {
        // Without full scope, only the roles mapped to the client reach its tokens.
        RealmRepresentation realm = generate(new KeycloakDevServicesProperties());

        assertThat(realm.getScopeMappings()).singleElement().satisfies(scopeMapping -> {
            assertThat(scopeMapping.getClient()).isEqualTo(KeycloakDevServicesProperties.Client.DEFAULT_CLIENT_ID);
            assertThat(scopeMapping.getRoles()).containsExactly("admin", "user");
        });
    }

    @Test
    void shouldUseTheConfiguredClient() {
        var properties = new KeycloakDevServicesProperties();
        properties.getClient().setId("my-app");
        properties.getClient().setSecret("my-secret");

        assertThat(generate(properties).getClients()).singleElement().satisfies(client -> {
            assertThat(client.getClientId()).isEqualTo("my-app");
            assertThat(client.getSecret()).isEqualTo("my-secret");
        });
    }

    @Test
    void shouldFailWhenTheClientSecretIsEmpty() {
        var properties = new KeycloakDevServicesProperties();
        properties.getClient().setSecret(" ");

        assertThatThrownBy(() -> KeycloakRealmFactory.generateRealmJson(properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(KeycloakDevServicesProperties.CONFIG_PREFIX + ".client.secret");
    }

    @Test
    void shouldCreateNoClientWhenTheClientIsDisabled() {
        var properties = new KeycloakDevServicesProperties();
        properties.getClient().setEnabled(false);

        assertThat(generate(properties).getClients()).isNullOrEmpty();
    }

    @Test
    void shouldSerializeOnlyTheFieldsItSets() {
        // Keycloak's realm import rejects properties it does not recognise, and the
        // representation classes may trail the Keycloak version running in the container.
        String json = KeycloakRealmFactory.generateRealmJson(new KeycloakDevServicesProperties());

        Map<String,Object> document = JsonParserFactory.getJsonParser().parseMap(json);

        assertThat(document).containsOnlyKeys("realm", "enabled", "sslRequired", "roles", "clients", "scopeMappings", "users");
    }

}
