package io.arconia.dev.services.keycloak;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.arconia.boot.bootstrap.BootstrapMode;

/**
 * Integration tests for {@link ArtemisDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class KeycloakDevServicesAutoConfigurationIT {

    private static final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(RestartScope.class))
            .withConfiguration(AutoConfigurations.of(KeycloakDevServicesAutoConfiguration.class))
            // ensure our bootstrap ConfigData loader runs during the test
            .withPropertyValues("spring.config.import=arconia-keycloak:");

    @BeforeEach
    void setUp() {
        BootstrapMode.clear();
    }


    @Test
    void autoConfigurationNotActivatedWhenDisabled() {
        contextRunner
                .withPropertyValues("arconia.dev.services.keycloak.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(KeycloakContainer.class));
    }

    @Test
    void containerAvailableInDevelopmentMode() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(KeycloakContainer.class);
                    KeycloakContainer bean = ctx.getBean(KeycloakContainer.class);
                    assertThat(bean.getDockerImageName()).contains("/keycloak/keycloak");
                    assertThat(bean.getEnv()).isEmpty();
                    assertThat(bean.isShouldBeReused()).isTrue();
                    bean.start();
                    assertThat(bean.getAdminUsername()).isEqualTo(KeycloakDevServicesProperties.DEFAULT_USERNAME);
                    assertThat(bean.getAdminPassword()).isEqualTo(KeycloakDevServicesProperties.DEFAULT_PASSWORD);
                });
    }

    @Test
    void containerAvailableInTestMode() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=test")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(KeycloakContainer.class);
                    KeycloakContainer bean = ctx.getBean(KeycloakContainer.class);
                    assertThat(bean.getDockerImageName()).contains("/keycloak/keycloak");
                    assertThat(bean.getEnv()).isEmpty();
                    assertThat(bean.isShouldBeReused()).isFalse();
                    bean.start();
                    assertThat(bean.getAdminUsername()).isEqualTo(KeycloakDevServicesProperties.DEFAULT_USERNAME);
                    assertThat(bean.getAdminPassword()).isEqualTo(KeycloakDevServicesProperties.DEFAULT_PASSWORD);
                });
    }

    @Test
    void containerConfigurationApplied() {
        contextRunner
            .withSystemProperties("arconia.bootstrap.mode=dev")
            .withPropertyValues(
                "arconia.dev.services.keycloak.port=1234",
                "arconia.dev.services.keycloak.shared=never",
                "arconia.dev.services.keycloak.startup-timeout=90s",
                "arconia.dev.services.keycloak.username=myusername",
                "arconia.dev.services.keycloak.password=mypassword")
            .run(ctx -> {
                assertThat(ctx).hasSingleBean(KeycloakContainer.class);
                KeycloakContainer bean = ctx.getBean(KeycloakContainer.class);
                assertThat(bean.isShouldBeReused()).isFalse();
                bean.start();
                assertThat(bean.getMappedPort(ArconiaKeycloakContainer.WEB_CONSOLE_PORT)).isEqualTo(1234);
                assertThat(bean.getAdminUsername()).isEqualTo("myusername");
                assertThat(bean.getAdminPassword()).isEqualTo("mypassword");
            });
    }


    @Test
    void containerConfigurationAppliedIncludingFromFile() {
        contextRunner
            .withSystemProperties("arconia.bootstrap.mode=dev")
            .withPropertyValues(
                "arconia.dev.services.keycloak.port=1234",
                "arconia.dev.services.keycloak.shared=never",
                "arconia.dev.services.keycloak.startup-timeout=90s",
                "arconia.dev.services.keycloak.username=myusername",
                "arconia.dev.services.keycloak.password=mypassword")
           // load test resource application.properties into the runner environment
            .withInitializer(ctx -> {
                try {
                    var env = (org.springframework.core.env.ConfigurableEnvironment) ctx.getEnvironment();
                    var src = new org.springframework.core.io.support.ResourcePropertySource("testProps",
                            "classpath:application.test.properties");
                    env.getPropertySources().addLast(src);
                } catch (java.io.IOException ex) {
                    throw new IllegalStateException(ex);
                }
            })
            .run(ctx -> {
                assertThat(ctx).hasSingleBean(KeycloakContainer.class);
                KeycloakContainer bean = ctx.getBean(KeycloakContainer.class);
                assertThat(bean.isShouldBeReused()).isFalse();
                bean.start();
                assertThat(bean.getMappedPort(ArconiaKeycloakContainer.WEB_CONSOLE_PORT)).isEqualTo(1234);
                assertThat(bean.getAdminUsername()).isEqualTo("myusername");
                assertThat(bean.getAdminPassword()).isEqualTo("mypassword");
            });
    }


    @Test
    void containerWithRestartScope() {
        contextRunner
                .withClassLoader(this.getClass().getClassLoader())
                .run(context -> {
                    assertThat(context).hasSingleBean(KeycloakContainer.class);
                    String[] beanNames = context.getBeanFactory().getBeanNamesForType(KeycloakContainer.class);
                    assertThat(beanNames).hasSize(1);
                    assertThat(context.getBeanFactory().getBeanDefinition(beanNames[0]).getScope())
                            .isEqualTo("restart");
                });
    }

}
