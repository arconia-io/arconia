package io.arconia.dev.services.lldap;

import org.testcontainers.ldap.LLdapContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.container.ContainerConfigurer;
import io.arconia.dev.services.core.util.ContainerUtils;

/**
 * A {@link LLdapContainer} configured for use with Arconia Dev Services.
 */
final class ArconiaLldapContainer extends LLdapContainer {

    private static final String COMPATIBLE_IMAGE_NAME = "lldap/lldap";

    private final LldapDevServicesProperties properties;

    static final int LDAP_PORT = 3890;

    static final int UI_PORT = 17170;

    public ArconiaLldapContainer(LldapDevServicesProperties properties) {
        super(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        this.properties = properties;

        ContainerConfigurer.base(this, properties);
    }

    @Override
    protected void configure() {
        super.configure();
        if (ContainerUtils.isValidPort(properties.getPort())) {
            addFixedExposedPort(properties.getPort(), LDAP_PORT);
        }
        if (ContainerUtils.isValidPort(properties.getManagementConsolePort())) {
            addFixedExposedPort(properties.getManagementConsolePort(), UI_PORT);
        }
    }

}
