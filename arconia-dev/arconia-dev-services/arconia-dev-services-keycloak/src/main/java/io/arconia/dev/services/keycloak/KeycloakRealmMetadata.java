package io.arconia.dev.services.keycloak;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import org.keycloak.util.JsonSerialization;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import io.arconia.dev.services.core.container.ContainerConfigurer;

/**
 * Resolves the realms a dev service imports and the one the application connects to.
 */
final class KeycloakRealmMetadata {

    /**
     * The realm Keycloak creates itself. An import may include it, but an application is
     * never assumed to connect to it.
     */
    static final String MASTER_REALM = "master";

    /**
     * Keycloak reads a file with this suffix as the single realm named by the rest of the file
     * name, and refuses to start when the two differ. Any other file name may hold one realm
     * or a list of realms.
     */
    static final String REALM_FILE_SUFFIX = "-realm.json";

    private KeycloakRealmMetadata() {}

    /**
     * The realm the application connects to: the configured name when set, or else the
     * generated realm, or the first imported realm other than {@value MASTER_REALM}.
     */
    static String resolveRealmName(KeycloakDevServicesProperties properties) {
        KeycloakDevServicesProperties.Realm realm = properties.getRealm();
        String name = realm.getName();
        if (realm.getImportPaths().isEmpty()) {
            return StringUtils.hasText(name) ? name : KeycloakDevServicesProperties.DEFAULT_REALM;
        }

        List<String> imported = resolveRealmNames(properties);
        if (StringUtils.hasText(name)) {
            Assert.state(imported.contains(name),
                    () -> "the imported realm files declare the realms %s, none of which is the realm '%s' configured in '%s.realm.name'"
                            .formatted(imported, name, KeycloakDevServicesProperties.CONFIG_PREFIX));
            return name;
        }
        return imported.stream()
                .filter(declared -> !MASTER_REALM.equals(declared))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "the imported realm files declare no realm other than '%s'; set the realm the application connects to in '%s.realm.name'"
                                .formatted(MASTER_REALM, KeycloakDevServicesProperties.CONFIG_PREFIX)));
    }

    /**
     * The realms the dev service serves, in the order they are declared: those of the imported
     * realm files, or else the generated one.
     */
    static List<String> resolveRealmNames(KeycloakDevServicesProperties properties) {
        List<String> importPaths = properties.getRealm().getImportPaths();
        if (importPaths.isEmpty()) {
            return List.of(resolveRealmName(properties));
        }
        Set<String> names = new LinkedHashSet<>();
        for (String importPath : importPaths) {
            names.addAll(readRealmNames(importPath, properties.getAdminUsername()));
        }
        return List.copyOf(names);
    }

    /**
     * The names of the realms declared by the given realm file, which holds either a single
     * realm or a list of realms. Fails when Keycloak would refuse to start with that file:
     * a file named after a realm must declare that realm only, and an imported
     * {@value MASTER_REALM} realm must not already have a user named like the administrator
     * the dev service creates there.
     */
    static List<String> readRealmNames(String sourcePath, String adminUsername) {
        Resource resource = ContainerConfigurer.resolveResource(sourcePath);

        JsonNode json;
        try (InputStream inputStream = resource.getInputStream()) {
            json = JsonSerialization.mapper.readTree(inputStream);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to parse the realm file '%s' as JSON.".formatted(sourcePath), ex);
        }

        List<JsonNode> realms = new ArrayList<>();
        if (json.isArray()) {
            json.forEach(realms::add);
        } else {
            realms.add(json);
        }
        Assert.state(!realms.isEmpty(), () -> "The realm file '%s' declares no realm.".formatted(sourcePath));

        List<String> names = new ArrayList<>();
        for (JsonNode realm : realms) {
            String name = realmName(realm, sourcePath);
            if (MASTER_REALM.equals(name)) {
                assertAdminUsernameAvailable(realm, adminUsername, sourcePath);
            }
            names.add(name);
        }

        String fileName = resource.getFilename();
        if (fileName != null && fileName.endsWith(REALM_FILE_SUFFIX)) {
            String expected = fileName.substring(0, fileName.length() - REALM_FILE_SUFFIX.length());
            Assert.state(names.equals(List.of(expected)),
                    () -> "Keycloak reads a file named '%s' as the single realm '%s', but the realm file '%s' declares %s. Rename the file or declare that realm."
                            .formatted(fileName, expected, sourcePath, names));
        }

        return names;
    }

    /**
     * Keycloak creates the administrator of the dev service in the master realm even when
     * that realm is imported, and aborts its startup when a user with that name is already
     * there. Usernames are compared ignoring case, as Keycloak stores them in lower case.
     */
    private static void assertAdminUsernameAvailable(JsonNode masterRealm, String adminUsername, String sourcePath) {
        for (JsonNode user : masterRealm.path("users")) {
            String username = user.path("username").asText();
            Assert.state(!username.equalsIgnoreCase(adminUsername),
                    () -> "The master realm in the realm file '%s' already has a user named '%s', but Keycloak creates the administrator of the dev service with that name and refuses to start otherwise. Set another name in '%s.admin-username' or remove the master realm from the file."
                            .formatted(sourcePath, username, KeycloakDevServicesProperties.CONFIG_PREFIX));
        }
    }

    private static String realmName(JsonNode realm, String sourcePath) {
        JsonNode name = realm.get("realm");
        if (name != null && name.isTextual() && StringUtils.hasText(name.asText())) {
            return name.asText();
        }
        throw new IllegalStateException(
                "No 'realm' field found in the realm file '%s'. A realm file must declare the realm it imports."
                        .formatted(sourcePath));
    }

}
