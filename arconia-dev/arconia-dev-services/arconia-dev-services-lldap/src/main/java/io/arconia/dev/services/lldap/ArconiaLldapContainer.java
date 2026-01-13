package io.arconia.dev.services.lldap;

import org.testcontainers.ldap.LLdapContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * A {@link LLdapContainer} specialized for Arconia Dev Services.
 */
public final class ArconiaLldapContainer extends LLdapContainer {

    private final LldapDevServicesProperties properties;

    /**
     * Web administration UI port.
     */
    private static final int LLDAP_WEB_PORT = 17170;

    /**
     * LDAP service port (unencrypted LDAP).
     */
    private static final int LLDAP_PORT = 3890;

    public ArconiaLldapContainer(DockerImageName image, LldapDevServicesProperties properties) {
        super(image);
        this.properties = properties;
    }

    @Override
    protected void configure() {
        super.configure();
        if (properties.getPort() > 0) {
            addFixedExposedPort(properties.getPort(), LLDAP_PORT);
        }
    }
}
