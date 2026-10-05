package io.arconia.dev.services.lldap;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ldap.autoconfigure.LdapConnectionDetails;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.ldap.LLdapContainer;

import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.api.registration.DevServiceLinkDefinition;
import io.arconia.dev.services.api.registration.DevServiceLinkProvider;
import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationIT;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link LldapDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class LldapDevServicesAutoConfigurationIT extends BaseDevServicesAutoConfigurationIT {

    private final ApplicationContextRunner contextRunner = defaultContextRunner(LldapDevServicesAutoConfiguration.class);

    @Override
    protected ApplicationContextRunner getContextRunner() {
        return contextRunner;
    }

    @Override
    protected Class<?> getAutoConfigurationClass() {
        return LldapDevServicesAutoConfiguration.class;
    }

    @Override
    protected Class<? extends GenericContainer<?>> getContainerClass() {
        return LLdapContainer.class;
    }

    @Override
    protected String getServiceName() {
        return "lldap";
    }

    @Override
    protected Class<?> getConnectionDetailsClass() {
        return LdapConnectionDetails.class;
    }

    @Override
    protected GenericContainer<?> createDiscoverableContainer(String ownerId) {
        // LLDAP requires these to boot; otherwise the container exits with code 1.
        // The password is the one LLDAP connection details default to.
        LldapDevServicesProperties properties = new LldapDevServicesProperties();
        properties.setEnvironment(Map.of(
                "LLDAP_JWT_SECRET", "letItGoWannaBuildSnowman",
                "LLDAP_LDAP_USER_PASS", "password"));
        return withDiscoveryLabels(new ArconiaLldapContainer(properties), ownerId);
    }

    @Override
    protected List<DevServiceLinkDefinition> discoverableContainerLinkDefinitions() {
        return new ArconiaLldapContainer(new LldapDevServicesProperties()).devServiceLinkDefinitions();
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
    void containerAvailableWithDefaultConfiguration() {
        getContextRunner().run(context -> {
            assertThat(context).hasSingleBean(getContainerClass());
            var container = context.getBean(getContainerClass());
            assertThat(container.getDockerImageName()).contains(ArconiaLldapContainer.COMPATIBLE_IMAGE_NAME);
            assertThat(container.getEnv()).isEmpty();
            assertThat(container.getNetworkAliases()).hasSize(1);
            assertThat(container.getBinds()).isEmpty();

            assertThatHasSingletonScope(context);
        });
    }

    @Test
    void devServiceLinksExposeManagementConsoleUrl() {
        getContextRunner()
                // LLDAP requires these to boot; otherwise the container exits with code 1.
                .withPropertyValues(
                        "arconia.dev.services.%s.environment.LLDAP_JWT_SECRET=letItGoWannaBuildSnowman".formatted(getServiceName()),
                        "arconia.dev.services.%s.environment.LLDAP_LDAP_USER_PASS=password".formatted(getServiceName()))
                .run(context -> {
                    var container = context.getBean(getContainerClass());
                    container.start();
                    List<DevServiceLink> links = ((DevServiceLinkProvider) container).devServiceLinkDefinitions().stream()
                            .map(definition -> definition.toLink(container.getHost(), container.getMappedPort(definition.port())))
                            .toList();
                    assertThat(links).singleElement().satisfies(link -> {
                        assertThat(link.id()).isEqualTo("lldap");
                        assertThat(link.label()).isEqualTo("LLDAP Console");
                        assertThat(link.url()).isEqualTo(
                                "http://" + container.getHost() + ":" + container.getMappedPort(ArconiaLldapContainer.UI_PORT));
                    });
                    container.stop();
                });
    }

    @Test
    void containerConfigurationApplied() {
        String[] properties = ArrayUtils.addAll(commonConfigurationProperties(),
                "arconia.dev.services.%s.environment.LLDAP_JWT_SECRET=letItGoWannaBuildSnowman".formatted(getServiceName()),
                "arconia.dev.services.%s.environment.LLDAP_LDAP_USER_PASS=password".formatted(getServiceName())
        );

        getContextRunner()
                .withPropertyValues(properties)
                .run(context -> {
                    var container = context.getBean(getContainerClass());
                    container.start();
                    assertThatConfigurationIsApplied(container);
                    assertThat(container.getEnv()).contains("LLDAP_JWT_SECRET=letItGoWannaBuildSnowman");
                    assertThat(container.getEnv()).contains("LLDAP_LDAP_USER_PASS=password");
                    container.stop();
                });
    }

}
