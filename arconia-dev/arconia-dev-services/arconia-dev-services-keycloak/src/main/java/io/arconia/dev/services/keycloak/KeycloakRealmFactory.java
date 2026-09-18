package io.arconia.dev.services.keycloak;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.util.JsonSerialization;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import io.arconia.dev.services.keycloak.KeycloakDevServicesProperties.Client;
import io.arconia.dev.services.keycloak.KeycloakDevServicesProperties.Realm.User;

/**
 * Builds the realm imported by the Keycloak Dev Service when no realm file is configured.
 */
final class KeycloakRealmFactory {

    /**
     * Roles created when neither roles nor users are configured, covering the common case of
     * an application distinguishing an administrator from an ordinary user.
     * <p>
     * Ordered deliberately. The iteration order of this map reaches the generated document, as
     * the realm's role list and as each user's role assignments, and the document is copied into
     * the container as content, which Testcontainers folds into the hash identifying a reusable
     * container. An unordered map would therefore change the hash from one JVM to the next, so a
     * container that should have been reused is recreated instead — and since reuse opts out of
     * Ryuk, the one it replaces is never reaped.
     */
    private static final Map<String,List<String>> DEFAULT_ROLES = defaultRoles();

    /**
     * The domain used for the email address synthesized for a configured user, matching the one
     * the default users carry so that a realm never holds two of them.
     */
    private static final String SYNTHESIZED_EMAIL_DOMAIN = "arconia.io";

    /**
     * A conservative dot-atom, as RFC 5322 defines the unquoted local part of an email address.
     * Only used to decide whether an address can be derived from a username at all.
     */
    private static final Pattern EMAIL_LOCAL_PART =
            Pattern.compile("[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*");

    /**
     * Redirect URIs and web origins are left unrestricted, so that logging in works whichever
     * port the application happens to run on. Acceptable here precisely because this realm only
     * ever exists in a throwaway development container.
     */
    private static final String ANY_URL = "*";

    private KeycloakRealmFactory() {}

    /**
     * Users created when none are configured, holding the roles of {@link #DEFAULT_ROLES} and a
     * full identity, so that the realm an application gets out of the box looks like one a
     * person would have set up.
     * <p>
     * Built afresh rather than held in a constant: a configured user is a mutable object, and
     * the defaults stand in for configured ones everywhere downstream.
     */
    private static Map<String,User> defaultUsers() {
        Map<String,User> users = new LinkedHashMap<>();
        users.put("isabella", defaultUser("isabella", "Isabella", "Nordskov"));
        users.put("bjorn", defaultUser("bjorn", "Bjorn", "Vinterberg"));
        return users;
    }

    private static User defaultUser(String username, String firstName, String lastName) {
        User user = new User();
        user.setPassword(username);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(username + "@" + SYNTHESIZED_EMAIL_DOMAIN);
        return user;
    }

    private static Map<String,List<String>> defaultRoles() {
        Map<String,List<String>> roles = new LinkedHashMap<>();
        roles.put("admin", List.of("isabella"));
        roles.put("user", List.of("isabella", "bjorn"));
        return Collections.unmodifiableMap(roles);
    }

    /**
     * The generated realm, serialized as the JSON document Keycloak imports at startup.
     */
    static String generateRealmJson(KeycloakDevServicesProperties properties) {
        String realmName = KeycloakRealmMetadata.resolveRealmName(properties);
        Map<String,User> users = resolveUsers(properties);
        Map<String,List<String>> roles = resolveRoles(properties);
        validateUsernamesAreDistinct(users);
        validateRolesReferenceKnownUsers(roles, users);

        RealmRepresentation realm = new RealmRepresentation();
        realm.setRealm(realmName);
        realm.setEnabled(true);
        // Development containers are served over plain HTTP.
        realm.setSslRequired("none");
        realm.setRoles(realmRoles(roles));
        realm.setUsers(users(users, roles));
        if (properties.getClient().isEnabled()) {
            realm.setClients(List.of(client(properties.getClient())));
        }

        try {
            return JsonSerialization.writeValueAsString(realm);
        }
        catch (IOException ex) {
            throw new IllegalStateException("Failed to generate the Keycloak realm '%s'.".formatted(realmName), ex);
        }
    }

    /**
     * The users to create, defaulting to {@link #defaultUsers()} when none are configured.
     * Configuring any user replaces the defaults entirely, rather than adding to them, so that
     * the realm holds exactly the users that were asked for.
     */
    private static Map<String,User> resolveUsers(KeycloakDevServicesProperties properties) {
        Map<String,User> configuredUsers = properties.getRealm().getUsers();
        return configuredUsers.isEmpty() ? defaultUsers() : configuredUsers;
    }

    /**
     * The roles to create, mapped to the users holding them.
     * <p>
     * The defaults only apply when no users are configured either: they name the default users,
     * so applying them to a realm with different users would assign roles to nobody and,
     * worse, suggest a relationship that isn't there.
     */
    private static Map<String,List<String>> resolveRoles(KeycloakDevServicesProperties properties) {
        Map<String,List<String>> configuredRoles = properties.getRealm().getRoles();
        if (!configuredRoles.isEmpty()) {
            return configuredRoles;
        }
        return properties.getRealm().getUsers().isEmpty() ? DEFAULT_ROLES : Map.of();
    }

    /**
     * Keycloak stores usernames in lower case, so two configured users differing only in case are
     * one user by the time the realm is imported, and the second silently replaces the first.
     * Rejecting them here, where both spellings can still be named.
     */
    private static void validateUsernamesAreDistinct(Map<String,User> users) {
        Map<String,String> byNormalisedName = new LinkedHashMap<>();
        for (String username : users.keySet()) {
            String previous = byNormalisedName.putIfAbsent(username.toLowerCase(Locale.ROOT), username);
            if (previous != null) {
                throw new IllegalStateException(
                        "The users '%s' and '%s' configured in '%s.realm.users' differ only in case. Keycloak stores usernames in lower case, so one would replace the other."
                                .formatted(previous, username, KeycloakDevServicesProperties.CONFIG_PREFIX));
            }
        }
    }

    /**
     * A role naming a user that does not exist is a typo, and Keycloak would import the realm
     * without complaint, leaving an application mysteriously short of a role at runtime. Failing
     * at startup instead, naming both the unknown user and the ones that are configured.
     */
    private static void validateRolesReferenceKnownUsers(Map<String,List<String>> roles,
            Map<String,User> users) {
        for (Map.Entry<String,List<String>> role : roles.entrySet()) {
            for (String username : role.getValue()) {
                if (!isConfiguredUser(username, users)) {
                    throw new IllegalStateException(
                            "The role '%s' configured in '%s.realm.roles' is assigned to the user '%s', which is not configured. Configured users: %s."
                                    .formatted(role.getKey(), KeycloakDevServicesProperties.CONFIG_PREFIX, username,
                                            users.keySet()));
                }
            }
        }
    }

    /**
     * Whether the given name refers to a configured user, compared the way Keycloak compares
     * usernames: without regard to case. Matching exactly would reject {@code roles.admin=[alice]}
     * alongside {@code users.Alice} as an unknown user, even though Keycloak considers them the
     * same person.
     */
    private static boolean isConfiguredUser(String username, Map<String,User> users) {
        return users.keySet().stream().anyMatch(configured -> configured.equalsIgnoreCase(username));
    }

    private static RolesRepresentation realmRoles(Map<String,List<String>> roles) {
        List<RoleRepresentation> realmRoles = new ArrayList<>();
        for (String role : roles.keySet()) {
            Assert.hasText(role, "a role name in '%s.realm.roles' cannot be null or empty".formatted(KeycloakDevServicesProperties.CONFIG_PREFIX));
            realmRoles.add(new RoleRepresentation(role, null, false));
        }

        RolesRepresentation rolesRepresentation = new RolesRepresentation();
        rolesRepresentation.setRealm(realmRoles);
        return rolesRepresentation;
    }

    private static List<UserRepresentation> users(Map<String,User> users,
            Map<String,List<String>> roles) {
        List<UserRepresentation> userRepresentations = new ArrayList<>();
        for (Map.Entry<String,User> user : users.entrySet()) {
            userRepresentations.add(user(user.getKey(), user.getValue(), rolesOf(user.getKey(), roles)));
        }
        return userRepresentations;
    }

    /**
     * The roles held by a user, read from the roles map the other way round: configuration lists
     * the users holding a role, whereas Keycloak wants the roles held by a user.
     */
    private static List<String> rolesOf(String username, Map<String,List<String>> roles) {
        List<String> userRoles = new ArrayList<>();
        for (Map.Entry<String,List<String>> role : roles.entrySet()) {
            // Compared without regard to case, for the same reason as isConfiguredUser.
            if (role.getValue().stream().anyMatch(assignee -> assignee.equalsIgnoreCase(username))) {
                userRoles.add(role.getKey());
            }
        }
        return userRoles;
    }

    /**
     * A user Keycloak considers completely set up.
     * <p>
     * The email address and the first and last name are not optional embellishments. Keycloak's
     * default user profile marks all three as required, and a user missing any of them is asked
     * to complete its profile at first login, which fails any non-interactive flow with the
     * decidedly unhelpful {@code Account is not fully set up}. A user that leaves them unset
     * therefore has them derived from its username rather than left out: the alternatives are to
     * disable the profile verification action, which would leave the realm without most of
     * Keycloak's default required actions, or to rewrite the realm's user profile, and both
     * distort the realm to avoid supplying three values the user can simply state.
     */
    private static UserRepresentation user(String username, User configuredUser, List<String> roles) {
        Assert.hasText(username, "a username in '%s.realm.users' cannot be null or empty".formatted(KeycloakDevServicesProperties.CONFIG_PREFIX));
        Assert.hasText(configuredUser.getPassword(),
                () -> "the password of the user '%s' cannot be null or empty; set '%s.realm.users.%s.password'".formatted(username, KeycloakDevServicesProperties.CONFIG_PREFIX, username));

        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setRealmRoles(roles);
        user.setCredentials(List.of(password(configuredUser.getPassword())));
        user.setFirstName(orDerivedName(configuredUser.getFirstName(), username));
        user.setLastName(orDerivedName(configuredUser.getLastName(), username));
        user.setEmail(orDerivedEmail(configuredUser.getEmail(), username));
        return user;
    }

    /**
     * The configured name, or one derived from the username when none was given. Only the first
     * letter is uppercased, and nothing else about the username is interpreted: a username is not
     * a name, so any attempt to derive a plausible one from it would be guesswork. This keeps the
     * result predictable and visibly generated, and a user that deserves better can say so.
     */
    private static String orDerivedName(String name, String username) {
        if (StringUtils.hasText(name)) {
            return name;
        }
        return Character.toUpperCase(username.charAt(0)) + username.substring(1);
    }

    /**
     * The configured email address, or one derived from the username when none was given.
     * <p>
     * Deriving one only works if the username can serve as the local part of an address, which is
     * not a given: a username that is itself an email address would otherwise produce
     * {@code alice@corp.example@arconia.io}, which Keycloak accepts and no one can use. Failing
     * instead, and pointing at the property that settles it.
     */
    private static String orDerivedEmail(String email, String username) {
        if (StringUtils.hasText(email)) {
            return email;
        }
        if (!EMAIL_LOCAL_PART.matcher(username).matches()) {
            throw new IllegalStateException(
                    "No email address can be derived from the username '%s', which is not usable as the local part of one. Set '%s.realm.users.%s.email' explicitly."
                            .formatted(username, KeycloakDevServicesProperties.CONFIG_PREFIX, username));
        }
        return username + "@" + SYNTHESIZED_EMAIL_DOMAIN;
    }

    private static CredentialRepresentation password(String password) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        // A temporary password would require a reset at first login, failing every
        // non-interactive flow the same way an incomplete profile does.
        credential.setTemporary(false);
        return credential;
    }

    /**
     * The client the application authenticates as. The direct access grant is enabled so that
     * tests and scripts can obtain a token without a browser, and service accounts so that the
     * application can act on its own behalf.
     */
    private static ClientRepresentation client(Client clientProperties) {
        Assert.hasText(clientProperties.getId(), "the client identifier cannot be null or empty; set '%s.client.id'"
                .formatted(KeycloakDevServicesProperties.CONFIG_PREFIX));

        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(clientProperties.getId());
        client.setEnabled(true);
        client.setStandardFlowEnabled(true);
        client.setDirectAccessGrantsEnabled(true);
        client.setRedirectUris(List.of(ANY_URL));
        client.setWebOrigins(List.of(ANY_URL));

        // An empty secret means a public client, which Keycloak refuses to create with service
        // accounts enabled, since there would be no client credentials to authenticate them with.
        boolean publicClient = clientProperties.getSecret().isEmpty();
        client.setPublicClient(publicClient);
        client.setServiceAccountsEnabled(!publicClient);
        if (!publicClient) {
            client.setSecret(clientProperties.getSecret());
        }
        return client;
    }

}
