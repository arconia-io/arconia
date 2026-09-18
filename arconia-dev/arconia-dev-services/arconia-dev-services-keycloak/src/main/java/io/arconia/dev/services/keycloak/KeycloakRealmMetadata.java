package io.arconia.dev.services.keycloak;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.json.JsonParserFactory;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import io.arconia.dev.services.core.container.ContainerConfigurer;

/**
 * Resolves the name of the realm a dev service connects to.
 */
final class KeycloakRealmMetadata {

    /**
     * The field naming the realm in a Keycloak realm file.
     */
    private static final String REALM_FIELD = "realm";

    /**
     * The realm every Keycloak instance has, and the only one present when this dev service
     * neither generates nor imports a realm.
     */
    static final String MASTER_REALM = "master";

    private KeycloakRealmMetadata() {}

    /**
     * Whether this dev service puts a realm in place for the application, either by generating
     * one or by importing realm files.
     * <p>
     * When it does not, the application is configuring Spring Security itself, so the dev
     * service contributes no OAuth2 configuration and simply leaves Keycloak running.
     */
    static boolean hasRealm(KeycloakDevServicesProperties.Realm realm) {
        return !realm.getImportPaths().isEmpty() || realm.isCreate();
    }

    /**
     * The name of the realm to connect to, for a caller that has not read the realm files.
     * <p>
     * Reads at most the first file, and none at all when the realm name is configured: this is
     * the path taken when joining a shared dev service, where nothing is imported, so reading
     * the remaining files would be pure waste. A caller that imports the files reads them all
     * once and passes the result to {@link #resolveRealmName(KeycloakDevServicesProperties,
     * Collection)} instead.
     */
    static String resolveRealmName(KeycloakDevServicesProperties properties) {
        return resolveRealmName(properties, firstDeclaredRealm(properties));
    }

    /**
     * The name of the realm to connect to, resolved in this order:
     * <ol>
     * <li>the configured realm name, when set;</li>
     * <li>the realm declared by the first configured realm file;</li>
     * <li>the default realm, when one is generated;</li>
     * <li>{@code master}, when no realm is generated or imported, since that is then the only
     * realm Keycloak has.</li>
     * </ol>
     *
     * @param importedRealms the realms declared by the configured realm files, in configuration
     * order, as returned by {@link #readRealmNames(List)}
     */
    static String resolveRealmName(KeycloakDevServicesProperties properties, Collection<String> importedRealms) {
        if (StringUtils.hasText(properties.getRealm().getName())) {
            return properties.getRealm().getName();
        }

        if (!importedRealms.isEmpty()) {
            return importedRealms.iterator().next();
        }

        return properties.getRealm().isCreate() ? KeycloakDevServicesProperties.DEFAULT_REALM : MASTER_REALM;
    }

    /**
     * The realm declared by the first configured realm file, or nothing when the realm name is
     * configured or no realm file is, in both of which cases no file needs reading at all.
     */
    private static List<String> firstDeclaredRealm(KeycloakDevServicesProperties properties) {
        if (StringUtils.hasText(properties.getRealm().getName())) {
            return List.of();
        }
        List<String> realmImportPaths = properties.getRealm().getImportPaths();
        return realmImportPaths.isEmpty() ? List.of() : List.of(readRealmName(realmImportPaths.get(0)));
    }

    /**
     * The realms declared by the given realm files, mapped from the path each was read from and
     * kept in configuration order.
     * <p>
     * Read in a single pass and carried around afterwards, because each name is needed twice —
     * once to resolve the realm to connect to, and once to name the file inside the container —
     * while a realm exported from Keycloak is a whole document, commonly megabytes, of which
     * this reads one field.
     * <p>
     * Two files declaring the same realm is rejected here rather than later: files are named
     * after the realm they declare, so they would overwrite each other inside the container and
     * silently drop one of them.
     */
    static Map<String, String> readRealmNames(List<String> sourcePaths) {
        Map<String, String> realmNames = new LinkedHashMap<>();
        for (String sourcePath : sourcePaths) {
            Assert.hasText(sourcePath, "a realm import path cannot be null or empty, in '%s.realm.import-paths'"
                    .formatted(KeycloakDevServicesProperties.CONFIG_PREFIX));
            String realmName = readRealmName(sourcePath);
            Assert.state(!realmNames.containsValue(realmName),
                    () -> "multiple realm import paths declare the realm '%s': %s".formatted(realmName, sourcePaths));
            realmNames.put(sourcePath, realmName);
        }
        return realmNames;
    }

    /**
     * The name of the realm declared by the given realm file.
     */
    static String readRealmName(String sourcePath) {
        String realmName = parseRealmName(sourcePath);
        if (!StringUtils.hasText(realmName)) {
            throw new IllegalStateException(
                    "No 'realm' field found in the realm file '%s'. Every realm file must declare the realm it imports."
                            .formatted(sourcePath));
        }
        return realmName;
    }

    private static String parseRealmName(String sourcePath) {
        Resource resource = ContainerConfigurer.resolveResource(sourcePath);
        String content;
        try {
            content = resource.getContentAsString(StandardCharsets.UTF_8);
        }
        catch (IOException ex) {
            throw new IllegalStateException("Failed to read the realm file '%s'.".formatted(sourcePath), ex);
        }

        Map<String, Object> realm;
        try {
            realm = JsonParserFactory.getJsonParser().parseMap(content);
        }
        catch (RuntimeException ex) {
            throw new IllegalStateException(
                    "Failed to parse the realm file '%s' as JSON.".formatted(sourcePath), ex);
        }

        Object realmName = realm.get(REALM_FIELD);
        return realmName instanceof String name ? name : "";
    }

}
