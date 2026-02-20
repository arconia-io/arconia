package io.arconia.dev.services.keycloak;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationIT;

/**
 * Integration tests for {@link KeycloakDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class KeycloakDevServicesAutoConfigurationIT extends BaseDevServicesAutoConfigurationIT {

    private static final ApplicationContextRunner contextRunner = defaultContextRunner(KeycloakDevServicesAutoConfiguration.class);

    @Override
    protected ApplicationContextRunner getContextRunner() {
        return contextRunner;
    }

    @Override
    protected Class<?> getAutoConfigurationClass() {
        return KeycloakDevServicesAutoConfiguration.class;
    }

    @Override
    protected Class<? extends GenericContainer<?>> getContainerClass() {
        return KeycloakContainer.class;
    }

    @Override
    protected String getServiceName() {
        return "keycloak";
    }

    @Test
    void containerAvailableInDevMode() {
        getContextRunner()
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .run(context -> {
                    assertThat(context).hasSingleBean(getContainerClass());
                    var container = (KeycloakContainer) context.getBean(getContainerClass());
                    assertThat(container.getDockerImageName()).contains(ArconiaKeycloakContainer.COMPATIBLE_IMAGE_NAME);
                    assertThat(container.getEnv()).isEmpty();
                    assertThat(container.getNetworkAliases()).hasSize(1);
                    assertThat(container.isShouldBeReused()).isTrue();
                    assertThat(container.getBinds()).isEmpty();
                    container.start();
                    assertThat(container.getAdminUsername()).isEqualTo(KeycloakDevServicesProperties.DEFAULT_USERNAME);
                    assertThat(container.getAdminPassword()).isEqualTo(KeycloakDevServicesProperties.DEFAULT_PASSWORD);
                    container.stop();

                    assertThatHasSingletonScope(context);
                });
    }

    @Test
    void containerConfigurationApplied() {
        String[] properties = ArrayUtils.addAll(commonConfigurationProperties(),
                "arconia.dev.services.%s.username=myusername".formatted(getServiceName()),
                "arconia.dev.services.%s.password=mypassword".formatted(getServiceName())
        );

        getContextRunner()
                .withPropertyValues(properties)
                .run(context -> {
                    var container = (KeycloakContainer) context.getBean(getContainerClass());
                    container.start();
                    assertThatConfigurationIsApplied(container);
                    assertThat(container.getAdminUsername()).isEqualTo("myusername");
                    assertThat(container.getAdminPassword()).isEqualTo("mypassword");
                    container.stop();
                });
    }

}
