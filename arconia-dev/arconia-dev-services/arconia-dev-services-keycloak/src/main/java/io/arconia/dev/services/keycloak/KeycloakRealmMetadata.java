package io.arconia.dev.services.keycloak;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.boot.json.JsonParserFactory;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import io.arconia.dev.services.core.container.ContainerConfigurer;

/**
 * Resolves the metadata of the realm a dev service connects to.
 */
final class KeycloakRealmMetadata {

    private KeycloakRealmMetadata() {}

    /**
     * The realm declared by the imported realm file, or else the name of the generated realm.
     */
    static String resolveRealmName(KeycloakDevServicesProperties properties) {
        KeycloakDevServicesProperties.Realm realm = properties.getRealm();
        if (StringUtils.hasText(realm.getImportPath())) {
            return readRealmName(realm.getImportPath());
        }
        return StringUtils.hasText(realm.getName()) ? realm.getName() : KeycloakDevServicesProperties.DEFAULT_REALM;
    }

    /**
     * The name of the realm declared by the given realm file.
     */
    static String readRealmName(String sourcePath) {
        Resource resource = ContainerConfigurer.resolveResource(sourcePath);

        Map<String, Object> realm;
        try {
            realm = JsonParserFactory.getJsonParser().parseMap(resource.getContentAsString(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read the realm file '%s'.".formatted(sourcePath), ex);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Failed to parse the realm file '%s' as JSON.".formatted(sourcePath), ex);
        }

        if (!(realm.get("realm") instanceof String name) || !StringUtils.hasText(name)) {
            throw new IllegalStateException(
                    "No 'realm' field found in the realm file '%s'. A realm file must declare the realm it imports."
                            .formatted(sourcePath));
        }

        return name;
    }

}
