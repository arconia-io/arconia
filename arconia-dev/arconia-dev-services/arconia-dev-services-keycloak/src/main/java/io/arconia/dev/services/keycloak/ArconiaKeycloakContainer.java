package io.arconia.dev.services.keycloak;

import org.springframework.util.StringUtils;
import org.testcontainers.Testcontainers;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import dasniko.testcontainers.keycloak.KeycloakContainer;

import io.arconia.dev.services.core.container.ContainerConfigurer;

/**
 * A {@link KeycloakContainer} configured for use with Arconia Dev Services.
 */
final class ArconiaKeycloakContainer extends KeycloakContainer {

    static final String COMPATIBLE_IMAGE_NAME = "quay.io/keycloak/keycloak";

    static final int HTTP_PORT = 8080;

    static final String IMPORT_DIRECTORY = "/opt/keycloak/data/import/";

    /**
     * Realm files are copied with an explicit file mode: a resource extracted from a jar
     * carries no usable permissions, and Keycloak runs as a non-root user that would
     * otherwise be unable to read the imported realm.
     */
    private static final int REALM_FILE_MODE = 0644;

    private final KeycloakDevServicesProperties properties;

    private final String realm;

    ArconiaKeycloakContainer(KeycloakDevServicesProperties properties) {
        super(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        this.properties = properties;
        this.realm = KeycloakRealmMetadata.resolveRealmName(properties);

        this.withImagePullPolicy(PullPolicy.defaultPolicy());
        this.withAdminUsername(properties.getAdminUsername());
        this.withAdminPassword(properties.getAdminPassword());

        // Keycloak requires each imported file to be named after the realm it declares.
        String importPath = properties.getRealm().getImportPath();
        if (StringUtils.hasText(importPath)) {
            this.withCopyFileToContainer(ContainerConfigurer.resolveMountableFile(importPath, REALM_FILE_MODE),
                    IMPORT_DIRECTORY + importFileName(realm));
        } else {
            this.withCopyToContainer(Transferable.of(KeycloakRealmFactory.generateRealmJson(properties), REALM_FILE_MODE),
                    IMPORT_DIRECTORY + importFileName(realm));
        }
    }

    static String importFileName(String realmName) {
        return realmName + "-realm.json";
    }

    @Override
    protected void configure() {
        super.configure();
        // Publishes host ports into the container's network namespace, so Keycloak can call
        // back into the application. Must happen before the container starts.
        for (Integer hostPort : properties.getHostAccessiblePorts()) {
            Testcontainers.exposeHostPorts(hostPort);
        }
    }

    String getRealm() {
        return realm;
    }

}
