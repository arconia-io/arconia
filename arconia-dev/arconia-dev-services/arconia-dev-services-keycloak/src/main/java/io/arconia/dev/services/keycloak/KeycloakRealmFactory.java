package io.arconia.dev.services.keycloak;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.util.JsonSerialization;

import org.springframework.util.Assert;

import io.arconia.dev.services.keycloak.KeycloakDevServicesProperties.Client;
import io.arconia.dev.services.keycloak.KeycloakDevServicesProperties.Realm.User;

/**
 * Builds the realm imported by the Keycloak Dev Service when no realm file is configured.
 */
final class KeycloakRealmFactory {

    /**
     * Usernames as Keycloak stores them, lower case, and usable as the local part of the
     * email address derived from them.
     */
    private static final Pattern USERNAME = Pattern.compile("[a-z0-9._-]+");

    private static final String EMAIL_DOMAIN = "arconia.io";

    /**
     * Redirect URIs and web origins are left unrestricted, so that logging in works whichever
     * port the application runs on.
     */
    private static final String ANY_URL = "*";

    private KeycloakRealmFactory() {}

    /**
     * The generated realm, serialized as the JSON document Keycloak imports at startup.
     */
    static String generateRealmJson(KeycloakDevServicesProperties properties) {
        String realmName = KeycloakRealmMetadata.resolveRealmName(properties);
        Assert.state(!KeycloakRealmMetadata.MASTER_REALM.equals(realmName),
                () -> "the generated realm cannot be named '%s', since Keycloak creates that realm itself; set another name in '%s.realm.name'"
                        .formatted(KeycloakRealmMetadata.MASTER_REALM, KeycloakDevServicesProperties.CONFIG_PREFIX));

        Map<String,User> users = resolveUsers(properties);
        validateUsers(users);
        Set<String> roles = new LinkedHashSet<>();
        users.values().forEach(user -> roles.addAll(user.getRoles()));

        RealmRepresentation realm = new RealmRepresentation();
        realm.setRealm(realmName);
        realm.setEnabled(true);
        realm.setSslRequired("none");
        realm.setRoles(realmRoles(roles));
        realm.setUsers(users(users));
        if (properties.getClient().isEnabled()) {
            realm.setClients(List.of(client(properties.getClient())));
            if (!roles.isEmpty()) {
                realm.clientScopeMapping(properties.getClient().getId()).setRoles(roles);
            }
        }

        try {
            return JsonSerialization.writeValueAsString(realm);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to generate the Keycloak realm '%s'.".formatted(realmName), ex);
        }
    }

    /**
     * The configured users, or the two default users when none are configured.
     */
    private static Map<String,User> resolveUsers(KeycloakDevServicesProperties properties) {
        Map<String,User> users = properties.getRealm().getUsers();
        if (!users.isEmpty()) {
            return users;
        }
        Map<String,User> defaultUsers = new LinkedHashMap<>();
        defaultUsers.put("isabella", defaultUser("isabella", "admin", "user"));
        defaultUsers.put("bjorn", defaultUser("bjorn", "user"));
        return defaultUsers;
    }

    private static User defaultUser(String username, String... roles) {
        User user = new User();
        user.setPassword(username);
        user.setRoles(List.of(roles));
        return user;
    }

    private static void validateUsers(Map<String,User> users) {
        users.forEach((username, user) -> {
            Assert.state(USERNAME.matcher(username).matches(),
                    () -> "the username '%s' in '%s.realm.users' must consist of lower-case letters, digits, '.', '_' or '-'"
                            .formatted(username, KeycloakDevServicesProperties.CONFIG_PREFIX));
            Assert.hasText(user.getPassword(), () -> "the password of the user '%s' cannot be null or empty; set '%s.realm.users.%s.password'"
                    .formatted(username, KeycloakDevServicesProperties.CONFIG_PREFIX, username));
            user.getRoles().forEach(role -> Assert.hasText(role, () -> "a role of the user '%s' cannot be null or empty"
                    .formatted(username)));
        });
    }

    private static RolesRepresentation realmRoles(Set<String> roles) {
        List<RoleRepresentation> realmRoles = new ArrayList<>();
        roles.forEach(role -> realmRoles.add(new RoleRepresentation(role, null, false)));
        RolesRepresentation rolesRepresentation = new RolesRepresentation();
        rolesRepresentation.setRealm(realmRoles);
        return rolesRepresentation;
    }

    private static List<UserRepresentation> users(Map<String,User> users) {
        List<UserRepresentation> userRepresentations = new ArrayList<>();
        users.forEach((username, user) -> userRepresentations.add(user(username, user.getPassword(), user.getRoles())));
        return userRepresentations;
    }

    /**
     * A user Keycloak considers completely set up. Its default user profile requires an email
     * address and both names, and refuses to issue a token without them ("Account is not fully
     * set up"), so they are derived from the username.
     */
    private static UserRepresentation user(String username, String password, List<String> roles) {
        String name = Character.toUpperCase(username.charAt(0)) + username.substring(1);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);

        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEnabled(true);
        user.setEmail(username + "@" + EMAIL_DOMAIN);
        user.setEmailVerified(true);
        user.setFirstName(name);
        user.setLastName(name);
        user.setRealmRoles(roles);
        user.setCredentials(List.of(credential));
        return user;
    }

    /**
     * Keycloak's default "realm roles" mapper puts the realm roles in the access token only, while
     * an OAuth2 Client application reads the user from the ID token and the userinfo endpoint. This
     * mapper adds them there too, under the same claim; it leaves the access token alone, which the
     * default mapper already covers.
     */
    private static ProtocolMapperRepresentation realmRolesInIdTokenAndUserInfo() {
        ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
        mapper.setName("realm roles in ID token and userinfo");
        mapper.setProtocol("openid-connect");
        mapper.setProtocolMapper("oidc-usermodel-realm-role-mapper");
        Map<String,String> config = new LinkedHashMap<>();
        config.put("claim.name", "realm_access.roles");
        config.put("jsonType.label", "String");
        config.put("multivalued", "true");
        config.put("id.token.claim", "true");
        config.put("userinfo.token.claim", "true");
        config.put("access.token.claim", "false");
        config.put("introspection.token.claim", "false");
        mapper.setConfig(config);
        return mapper;
    }

    /**
     * A confidential client. The direct access grant is enabled so that tests and scripts can
     * obtain a token without a browser.
     */
    private static ClientRepresentation client(Client clientProperties) {
        Assert.hasText(clientProperties.getId(), "the client identifier cannot be null or empty; set '%s.client.id'"
                .formatted(KeycloakDevServicesProperties.CONFIG_PREFIX));
        Assert.hasText(clientProperties.getSecret(), "the client secret cannot be null or empty; set '%s.client.secret'"
                .formatted(KeycloakDevServicesProperties.CONFIG_PREFIX));

        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(clientProperties.getId());
        client.setSecret(clientProperties.getSecret());
        client.setEnabled(true);
        client.setPublicClient(false);
        client.setFullScopeAllowed(false);
        client.setStandardFlowEnabled(true);
        client.setDirectAccessGrantsEnabled(true);
        client.setServiceAccountsEnabled(true);
        client.setRedirectUris(List.of(ANY_URL));
        client.setWebOrigins(List.of(ANY_URL));
        client.setProtocolMappers(List.of(realmRolesInIdTokenAndUserInfo()));
        return client;
    }

}
