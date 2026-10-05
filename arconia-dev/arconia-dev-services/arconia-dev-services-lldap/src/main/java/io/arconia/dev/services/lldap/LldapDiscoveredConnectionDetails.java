package io.arconia.dev.services.lldap;

import org.springframework.boot.ldap.autoconfigure.LdapConnectionDetails;

import io.arconia.dev.services.core.registration.DiscoveredContainer;

/**
 * {@link LdapConnectionDetails} for connecting to an LLDAP dev service
 * running in a container discovered from another application.
 */
final class LldapDiscoveredConnectionDetails implements LdapConnectionDetails {

    private final String url;

    private final String baseDn;

    private final String username;

    private final String password;

    LldapDiscoveredConnectionDetails(DiscoveredContainer container, LldapDevServicesProperties properties) {
        // The container is only asked how it would be configured, it is never started.
        ArconiaLldapContainer configuration = new ArconiaLldapContainer(properties);
        this.url = "ldap://%s:%d".formatted(container.host(), container.mappedPort(ArconiaLldapContainer.LDAP_PORT));
        this.baseDn = configuration.getBaseDn();
        this.username = configuration.getUser();
        this.password = configuration.getPassword();
    }

    @Override
    public String[] getUrls() {
        return new String[] { url };
    }

    @Override
    public String getBase() {
        return baseDn;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

}
