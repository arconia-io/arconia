package io.arconia.dev.services.lldap;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ldap.autoconfigure.LdapConnectionDetails;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.ldap.LLdapContainer;

import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationTests;
import io.arconia.dev.services.tests.DisabledIfDockerUnavailable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LldapDevServicesAutoConfiguration}.
 */
class LldapDevServicesAutoConfigurationTests extends BaseDevServicesAutoConfigurationTests {

    LldapDevServicesAutoConfigurationTests() {
        super(LldapDevServicesAutoConfiguration.class, "lldap", LLdapContainer.class, LdapConnectionDetails.class);
    }

    @Override
    protected GenericContainer<?> createDiscoverableContainer(String ownerId) {
        LldapDevServicesProperties properties = new LldapDevServicesProperties();
        return asDiscoverableContainer(LldapDevServicesAutoConfiguration.createContainer(properties), properties, ownerId);
    }

    @Override
    protected Map<String, Integer> additionalFixedPortProperties() {
        return Map.of("management-console-port", LldapDevServicesAutoConfiguration.UI_PORT);
    }

    @Override
    protected void assertDiscoveredConnectionDetails(AssertableApplicationContext context, GenericContainer<?> discoveredContainer) {
        LLdapContainer lldapContainer = (LLdapContainer) discoveredContainer;
        LdapConnectionDetails connectionDetails = context.getBean(LdapConnectionDetails.class);
        assertThat(connectionDetails.getUrls()).containsExactly(lldapContainer.getLdapUrl());
        assertThat(connectionDetails.getBase()).isEqualTo(lldapContainer.getBaseDn());
        assertThat(connectionDetails.getUsername()).isEqualTo(lldapContainer.getUser());
        assertThat(connectionDetails.getPassword()).isEqualTo(lldapContainer.getPassword());
    }

    @Test
    void containerBootsWithFixedCredentials() {
        var container = LldapDevServicesAutoConfiguration.createContainer(new LldapDevServicesProperties());

        assertThat(container.getEnvMap())
                .containsEntry("LLDAP_JWT_SECRET", LldapDevServicesAutoConfiguration.JWT_SECRET)
                .containsEntry("LLDAP_LDAP_USER_PASS", LldapDevServicesAutoConfiguration.ADMIN_PASSWORD);
        assertThat(container.getUser()).startsWith("cn=admin,");
    }

    @Test
    @DisabledIfDockerUnavailable
    void devServiceLinksExposeManagementConsoleUrl() {
        contextRunnerWithContainerLifecycle()
                .run(context -> {
                    var container = context.getBean(getContainerClass());
                    List<DevServiceLink> links = context.getBean(DevServiceRegistration.class).getLinks();
                    assertThat(links).singleElement().satisfies(link -> {
                        assertThat(link.label()).isEqualTo("LLDAP Console");
                        assertThat(link.url()).isEqualTo(
                                "http://" + container.getHost() + ":" + container.getMappedPort(LldapDevServicesAutoConfiguration.UI_PORT));
                    });
                });
    }

}
