package io.arconia.dev.services.lldap;

import org.testcontainers.ldap.LLdapContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * A {@link LLdapContainer} specialized for Arconia Dev Services.
 */
public final class ArconiaLldapContainer extends LLdapContainer {

    public ArconiaLldapContainer(DockerImageName image) {
        super(image);
    }

}
