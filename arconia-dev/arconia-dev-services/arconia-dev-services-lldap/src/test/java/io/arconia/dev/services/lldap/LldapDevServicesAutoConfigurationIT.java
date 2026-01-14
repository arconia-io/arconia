package io.arconia.dev.services.lldap;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.ldap.LLdapContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link LldapDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class LldapDevServicesAutoConfigurationIT {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(RestartScope.class))
            .withConfiguration(AutoConfigurations.of(LldapDevServicesAutoConfiguration.class));

    @Test
    void autoConfigurationNotActivatedWhenDisabled() {
        contextRunner
                .withPropertyValues("arconia.dev.services.lldap.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(LLdapContainer.class));
    }

    @Test
    void containerAvailableWithDefaultConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(LLdapContainer.class);
            LLdapContainer container = context.getBean(LLdapContainer.class);
            assertThat(container.getDockerImageName()).contains("lldap/lldap");
            assertThat(container.getEnv()).isEmpty();
            assertThat(container.isShouldBeReused()).isFalse();
        });
    }

    @Test
    void containerConfigurationApplied() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .withPropertyValues(
                        "arconia.dev.services.lldap.port=1234",
                        "arconia.dev.services.lldap.environment.KEY=value",
                        "arconia.dev.services.lldap.shared=never",
                        "arconia.dev.services.lldap.startup-timeout=90s")
                .run(context -> {
                    assertThat(context).hasSingleBean(LLdapContainer.class);
                    LLdapContainer container = context.getBean(LLdapContainer.class);
                    assertThat(container.getEnv()).contains("KEY=value");
                    assertThat(container.isShouldBeReused()).isFalse();

                    container.start();
                    assertThat(container.getMappedPort(ArconiaLldapContainer.LLDAP_WEB_CONSOLE_PORT)).isEqualTo(1234);
                });
    }

    @Test
    void containerWithRestartScope() {
        contextRunner
                .withClassLoader(this.getClass().getClassLoader())
                .run(context -> {
                    assertThat(context).hasSingleBean(LLdapContainer.class);
                    String[] beanNames = context.getBeanFactory().getBeanNamesForType(LLdapContainer.class);
                    assertThat(beanNames).hasSize(1);
                    assertThat(context.getBeanFactory().getBeanDefinition(beanNames[0]).getScope())
                            .isEqualTo("restart");
                });
    }

}
