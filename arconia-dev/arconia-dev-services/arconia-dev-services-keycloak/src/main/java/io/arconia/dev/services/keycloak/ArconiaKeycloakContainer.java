package io.arconia.dev.services.keycloak;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.util.Assert;
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

        List<String> importPaths = properties.getRealm().getImportPaths();
        if (importPaths.isEmpty()) {
            this.withCopyToContainer(Transferable.of(KeycloakRealmFactory.generateRealmJson(properties), REALM_FILE_MODE),
                    IMPORT_DIRECTORY + realm + KeycloakRealmMetadata.REALM_FILE_SUFFIX);
        } else {
            Set<String> fileNames = new HashSet<>();
            for (String importPath : importPaths) {
                String fileName = ContainerConfigurer.resolveResource(importPath).getFilename();
                Assert.state(fileName != null && fileNames.add(fileName),
                        () -> "Realm files are imported under their own name, so two of them cannot be named '%s'."
                                .formatted(fileName));
                this.withCopyFileToContainer(ContainerConfigurer.resolveMountableFile(importPath, REALM_FILE_MODE),
                        IMPORT_DIRECTORY + fileName);
            }
        }
    }

    @Override
    protected void configure() {
        super.configure();
        for (Integer hostPort : properties.getHostAccessiblePorts()) {
            Testcontainers.exposeHostPorts(hostPort);
        }
    }

    String getRealm() {
        return realm;
    }

}
