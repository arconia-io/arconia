package io.arconia.dev.services.lldap;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.ldap.LLdapContainer;

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

    @Test
    void containerAvailableWithDefaultConfiguration() {
        getContextRunner().run(context -> {
            assertThat(context).hasSingleBean(getContainerClass());
            var container = context.getBean(getContainerClass());
            assertThat(container.getDockerImageName()).contains(ArconiaLldapContainer.COMPATIBLE_IMAGE_NAME);
            assertThat(container.getEnv()).isEmpty();
            assertThat(container.getNetworkAliases()).hasSize(1);
            assertThat(container.isShouldBeReused()).isFalse();
            assertThat(container.getBinds()).isEmpty();

            assertThatHasSingletonScope(context);
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
