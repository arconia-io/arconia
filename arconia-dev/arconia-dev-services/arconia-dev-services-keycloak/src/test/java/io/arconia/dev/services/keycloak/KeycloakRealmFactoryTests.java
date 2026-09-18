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
        }
        catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Users configured as nothing but a username and a password, which is the compact
     * configuration form and the one that leaves the identity to be derived.
     */
    private static Map<String,User> users(String... usernamesAndPasswords) {
        Map<String,User> users = new LinkedHashMap<>();
        for (int i = 0; i < usernamesAndPasswords.length; i += 2) {
            users.put(usernamesAndPasswords[i], User.of(usernamesAndPasswords[i + 1]));
        }
        return users;
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
    void shouldCreateTheDefaultUsersWithAFullIdentity() {
        // The email address and both names are what make the users usable: Keycloak's default
        // user profile requires all three, and a user missing any of them cannot obtain a token.
        RealmRepresentation realm = generate(new KeycloakDevServicesProperties());

        assertThat(realm.getUsers())
                .extracting(UserRepresentation::getUsername, UserRepresentation::getFirstName,
                        UserRepresentation::getLastName, UserRepresentation::getEmail)
                .containsExactly(
                        tuple("isabella", "Isabella", "Nordskov", "isabella@arconia.io"),
                        tuple("bjorn", "Bjorn", "Vinterberg", "bjorn@arconia.io"));
        assertThat(realm.getUsers()).allSatisfy(user -> {
            assertThat(user.isEnabled()).isTrue();
            assertThat(user.isEmailVerified()).isTrue();
        });
    }

    @Test
    void shouldCreateTheDefaultRoles() {
        RealmRepresentation realm = generate(new KeycloakDevServicesProperties());

        // The order is asserted, not merely the content: it reaches the generated document, and
        // the document is part of the hash identifying a reusable container. See
        // shouldGenerateTheSameDocumentEveryTime.
        assertThat(realm.getRoles().getRealm())
                .extracting(RoleRepresentation::getName)
                .containsExactly("admin", "user");
        assertThat(user(realm, "isabella").getRealmRoles()).containsExactly("admin", "user");
        assertThat(user(realm, "bjorn").getRealmRoles()).containsExactly("user");
    }

    @Test
    void shouldGenerateTheSameDocumentEveryTime() {
        // The document is copied into the container as content, and Testcontainers folds the
        // content of copied files into the hash that identifies a reusable container. A document
        // that varies — because a role or a user came out of an unordered collection — changes
        // that hash, so a container that should have been reused is recreated instead, and since
        // reuse opts out of Ryuk the one it replaces is never reaped.
        String first = KeycloakRealmFactory.generateRealmJson(new KeycloakDevServicesProperties());
        String second = KeycloakRealmFactory.generateRealmJson(new KeycloakDevServicesProperties());

        assertThat(second).isEqualTo(first);
    }

    @Test
    void shouldGiveEachUserANonTemporaryPassword() {
        // A temporary password would require a reset at first login, failing every
        // non-interactive flow.
        RealmRepresentation realm = generate(new KeycloakDevServicesProperties());

        assertThat(user(realm, "isabella").getCredentials()).singleElement().satisfies(credential -> {
            assertThat(credential.getType()).isEqualTo(CredentialRepresentation.PASSWORD);
            assertThat(credential.getValue()).isEqualTo("isabella");
            assertThat(credential.isTemporary()).isFalse();
        });
    }

    @Test
    void shouldReplaceTheDefaultUsersWithTheConfiguredOnes() {
        var properties = new KeycloakDevServicesProperties();
        properties.getRealm().setUsers(users("alice", "s3cret"));

        RealmRepresentation realm = generate(properties);

        assertThat(realm.getUsers()).extracting(UserRepresentation::getUsername).containsExactly("alice");
        assertThat(user(realm, "alice").getCredentials()).singleElement()
                .extracting(CredentialRepresentation::getValue).isEqualTo("s3cret");
    }

    @Test
    void shouldDeriveTheIdentityOfAUserThatStatesOnlyAPassword() {
        // Keycloak requires an email address and both names, so a user given in the compact form
        // has them derived from its username rather than left out.
        var properties = new KeycloakDevServicesProperties();
        properties.getRealm().setUsers(users("alice", "s3cret"));

        UserRepresentation alice = user(generate(properties), "alice");

        assertThat(alice.getFirstName()).isEqualTo("Alice");
        assertThat(alice.getLastName()).isEqualTo("Alice");
        assertThat(alice.getEmail()).isEqualTo("alice@arconia.io");
    }

    @Test
    void shouldUseTheStatedIdentityOfAConfiguredUser() {
        var properties = new KeycloakDevServicesProperties();
        var alice = User.of("s3cret");
        alice.setFirstName("Alice");
        alice.setLastName("Sørensen");
        alice.setEmail("alice@example.com");
        properties.getRealm().setUsers(new LinkedHashMap<>(Map.of("alice", alice)));

        UserRepresentation user = user(generate(properties), "alice");

        assertThat(user.getFirstName()).isEqualTo("Alice");
        assertThat(user.getLastName()).isEqualTo("Sørensen");
        assertThat(user.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void shouldDeriveOnlyTheFieldsAUserLeavesUnset() {
        // Each field falls back on its own, so stating an email address does not oblige a user
        // to state a name as well.
        var properties = new KeycloakDevServicesProperties();
        var alice = User.of("s3cret");
        alice.setEmail("alice@example.com");
        properties.getRealm().setUsers(new LinkedHashMap<>(Map.of("alice", alice)));

        UserRepresentation user = user(generate(properties), "alice");

        assertThat(user.getEmail()).isEqualTo("alice@example.com");
        assertThat(user.getFirstName()).isEqualTo("Alice");
        assertThat(user.getLastName()).isEqualTo("Alice");
    }

    @Test
    void shouldNotTreatAConfiguredUserNamedAfterADefaultOneSpecially() {
        // The default users are ordinary users that happen to state their identity, not names
        // the factory recognises. Configuring one of them replaces it outright.
        var properties = new KeycloakDevServicesProperties();
        properties.getRealm().setUsers(users("isabella", "another-password"));

        UserRepresentation isabella = user(generate(properties), "isabella");

        assertThat(isabella.getLastName()).isEqualTo("Isabella");
        assertThat(isabella.getEmail()).isEqualTo("isabella@arconia.io");
        assertThat(isabella.getCredentials()).singleElement()
                .extracting(CredentialRepresentation::getValue).isEqualTo("another-password");
    }

    @Test
    void shouldNotUppercaseAnythingButTheFirstLetterOfAUsername() {
        var properties = new KeycloakDevServicesProperties();
        properties.getRealm().setUsers(users("alice.smith", "s3cret"));

        UserRepresentation alice = user(generate(properties), "alice.smith");

        assertThat(alice.getFirstName()).isEqualTo("Alice.smith");
        assertThat(alice.getEmail()).isEqualTo("alice.smith@arconia.io");
    }

    @Test
    void shouldInvertRolesIntoPerUserAssignments() {
        // Configuration lists the users holding a role; Keycloak wants the roles held by a user.
        var properties = new KeycloakDevServicesProperties();
        properties.getRealm().setUsers(users("alice", "alice", "bob", "bob"));
        var roles = new LinkedHashMap<String,List<String>>();
        roles.put("editor", List.of("alice"));
        roles.put("reader", List.of("alice", "bob"));
        properties.getRealm().setRoles(roles);

        RealmRepresentation realm = generate(properties);

        assertThat(realm.getRoles().getRealm())
                .extracting(RoleRepresentation::getName)
                .containsExactly("editor", "reader");
        assertThat(user(realm, "alice").getRealmRoles()).containsExactly("editor", "reader");
        assertThat(user(realm, "bob").getRealmRoles()).containsExactly("reader");
    }

    @Test
    void shouldCreateARoleHeldByNobody() {
        var properties = new KeycloakDevServicesProperties();
        properties.getRealm().setUsers(users("alice", "alice"));
        properties.getRealm().setRoles(new LinkedHashMap<>(Map.of("unassigned", List.of())));

        RealmRepresentation realm = generate(properties);

        assertThat(realm.getRoles().getRealm()).extracting(RoleRepresentation::getName).containsExactly("unassigned");
        assertThat(user(realm, "alice").getRealmRoles()).isEmpty();
    }

    @Test
    void shouldNotApplyTheDefaultRolesToConfiguredUsers() {
        // The default roles name the default users, so applying them to a realm with different
        // users would assign them to nobody while suggesting a relationship that isn't there.
        var properties = new KeycloakDevServicesProperties();
        properties.getRealm().setUsers(users("alice", "alice"));

        assertThat(generate(properties).getRoles().getRealm()).isEmpty();
    }

    @Test
    void shouldFailWhenARoleNamesAnUnknownUser() {
        var properties = new KeycloakDevServicesProperties();
        properties.getRealm().setUsers(users("alice", "alice"));
        properties.getRealm().setRoles(new LinkedHashMap<>(Map.of("editor", List.of("albert"))));

        assertThatThrownBy(() -> KeycloakRealmFactory.generateRealmJson(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("albert")
                .hasMessageContaining("editor")
                .hasMessageContaining("alice");
    }

    @Test
    void shouldFailWhenTwoUsernamesDifferOnlyInCase() {
        // Keycloak stores usernames in lower case, so these are one user by the time the realm is
        // imported and the second would silently replace the first.
        var properties = new KeycloakDevServicesProperties();
        var users = new LinkedHashMap<String,User>();
        users.put("alice", User.of("first"));
        users.put("Alice", User.of("second"));
        properties.getRealm().setUsers(users);

        assertThatThrownBy(() -> KeycloakRealmFactory.generateRealmJson(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("alice")
                .hasMessageContaining("Alice")
                .hasMessageContaining(KeycloakDevServicesProperties.CONFIG_PREFIX + ".realm.users");
    }

    @Test
    void shouldAssignARoleToAUserNamedInADifferentCase() {
        // Keycloak compares usernames without regard to case, so matching exactly would reject
        // this as a role assigned to an unknown user, even though it names the same person.
        var properties = new KeycloakDevServicesProperties();
        properties.getRealm().setUsers(users("Alice", "s3cret"));
        properties.getRealm().setRoles(new LinkedHashMap<>(Map.of("editor", List.of("alice"))));

        RealmRepresentation realm = generate(properties);

        assertThat(user(realm, "Alice").getRealmRoles()).containsExactly("editor");
    }

    @Test
    void shouldFailWhenNoEmailCanBeDerivedFromTheUsername() {
        // Deriving one would produce 'alice@corp.example@arconia.io', which Keycloak accepts and
        // nobody can use.
        var properties = new KeycloakDevServicesProperties();
        properties.getRealm().setUsers(users("alice@corp.example", "s3cret"));

        assertThatThrownBy(() -> KeycloakRealmFactory.generateRealmJson(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("alice@corp.example")
                .hasMessageContaining(".email");
    }

    @Test
    void shouldAcceptAUsernameNoEmailCanBeDerivedFromWhenOneIsStated() {
        // Stating the address is what makes the username's shape irrelevant, so the validation
        // constrains only the fallback.
        var properties = new KeycloakDevServicesProperties();
        var alice = User.of("s3cret");
        alice.setEmail("alice@corp.example");
        properties.getRealm().setUsers(new LinkedHashMap<>(Map.of("alice@corp.example", alice)));

        assertThat(user(generate(properties), "alice@corp.example").getEmail()).isEqualTo("alice@corp.example");
    }

    @Test
    void shouldFailWhenAUserHasNoPassword() {
        var properties = new KeycloakDevServicesProperties();
        properties.getRealm().setUsers(users("alice", ""));

        assertThatThrownBy(() -> KeycloakRealmFactory.generateRealmJson(properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("alice");
    }

    @Test
    void shouldCreateAConfidentialClient() {
        RealmRepresentation realm = generate(new KeycloakDevServicesProperties());

        assertThat(realm.getClients()).singleElement().satisfies(client -> {
            assertThat(client.getClientId()).isEqualTo(KeycloakDevServicesProperties.Client.DEFAULT_CLIENT_ID);
            assertThat(client.getSecret()).isEqualTo(KeycloakDevServicesProperties.Client.DEFAULT_CLIENT_SECRET);
            assertThat(client.isPublicClient()).isFalse();
            assertThat(client.isServiceAccountsEnabled()).isTrue();
            // Needed by tests and scripts obtaining a token without a browser.
            assertThat(client.isDirectAccessGrantsEnabled()).isTrue();
            assertThat(client.getRedirectUris()).containsExactly("*");
        });
    }

    @Test
    void shouldCreateAPublicClientWithoutServiceAccounts() {
        // Keycloak refuses a public client with service accounts enabled, since there would be
        // no client credentials to authenticate them with.
        var properties = new KeycloakDevServicesProperties();
        properties.getClient().setSecret("");

        RealmRepresentation realm = generate(properties);

        assertThat(realm.getClients()).singleElement().satisfies(client -> {
            assertThat(client.isPublicClient()).isTrue();
            assertThat(client.isServiceAccountsEnabled()).isFalse();
            assertThat(client.getSecret()).isNull();
        });
    }

    @Test
    void shouldCreateNoClientWhenTheClientIsDisabled() {
        // A registration pointing at a client that was never created is worse than none, so the
        // two always agree.
        var properties = new KeycloakDevServicesProperties();
        properties.getClient().setEnabled(false);

        assertThat(generate(properties).getClients()).isNullOrEmpty();
    }

    @Test
    void shouldUseTheConfiguredClientIdentifier() {
        // The same properties back the Spring Security client registration, so the realm
        // declares the client the application authenticates as by construction.
        var properties = new KeycloakDevServicesProperties();
        properties.getClient().setId("my-app");
        properties.getClient().setSecret("my-secret");

        assertThat(generate(properties).getClients())
                .singleElement()
                .satisfies(client -> {
                    assertThat(client.getClientId()).isEqualTo("my-app");
                    assertThat(client.getSecret()).isEqualTo("my-secret");
                });
    }

    @Test
    void shouldSerializeOnlyTheFieldsItSets() {
        // Keycloak's realm import rejects properties it does not recognise, and the
        // representation classes track a Keycloak version that is not necessarily the one
        // running in the container. Keeping the document to the fields set here is what makes
        // the generated realm survive a Keycloak upgrade, so it is asserted rather than assumed.
        String json = KeycloakRealmFactory.generateRealmJson(new KeycloakDevServicesProperties());

        Map<String,Object> document = JsonParserFactory.getJsonParser().parseMap(json);

        assertThat(document).containsOnlyKeys("realm", "enabled", "sslRequired", "roles", "clients", "users");
    }

}
