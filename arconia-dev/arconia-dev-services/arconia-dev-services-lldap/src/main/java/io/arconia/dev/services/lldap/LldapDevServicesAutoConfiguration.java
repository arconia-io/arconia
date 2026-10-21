package io.arconia.dev.services.lldap;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ldap.autoconfigure.LdapConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.testcontainers.ldap.LLdapContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.autoconfigure.DevServicesAutoConfiguration;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.lldap.LldapDevServicesAutoConfiguration.LldapDevServicesRegistrar;

/**
 * Auto-configuration for LLDAP Dev Services.
 */
@AutoConfiguration(after = DevServicesAutoConfiguration.class, before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("lldap")
@EnableConfigurationProperties(LldapDevServicesProperties.class)
@Import(LldapDevServicesRegistrar.class)
public final class LldapDevServicesAutoConfiguration {

    static final String COMPATIBLE_IMAGE_NAME = "lldap/lldap";

    static final int LDAP_PORT = 3890;

    static final int UI_PORT = 17170;

    static final String ADMIN_PASSWORD = "password";

    /**
     * The secret LLDAP signs its session tokens with, which it refuses to start without.
     * A fixed value, so that a container kept across restarts keeps its sessions valid.
     */
    static final String JWT_SECRET = "arconia-dev-services-lldap";

    static LLdapContainer createContainer(LldapDevServicesProperties properties) {
        var container = new LLdapContainer(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        container.withEnv("LLDAP_JWT_SECRET", JWT_SECRET);
        container.withUserPass(ADMIN_PASSWORD);
        return container;
    }

    static class LldapDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(LldapDevServicesProperties.CONFIG_PREFIX, LldapDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("lldap")
                    .description("LLDAP Dev Service")
                    .properties(properties)
                    .container(LLdapContainer.class, () -> createContainer(properties))
                    .port(LDAP_PORT)
                    .additionalPort(UI_PORT, properties.getManagementConsolePort())
                    .discovery(LdapConnectionDetails.class,
                            container -> new LldapDiscoveredConnectionDetails(container, properties))
                    .link("LLDAP Console", UI_PORT));
        }

    }

}
