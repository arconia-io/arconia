package io.arconia.dev.services.lldap;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.support.SimpleThreadScope;
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
    void autoConfigurationNotActivatedWhenGloballyDisabled() {
        contextRunner
                .withPropertyValues("arconia.dev.services.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(LLdapContainer.class));
    }

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
            assertThat(container.getNetworkAliases()).hasSize(1);
            assertThat(container.isShouldBeReused()).isFalse();

            String[] beanNames = context.getBeanFactory().getBeanNamesForType(LLdapContainer.class);
            assertThat(beanNames).hasSize(1);
            assertThat(context.getBeanFactory().getBeanDefinition(beanNames[0]).getScope())
                    .isEqualTo("singleton");
        });
    }

    @Test
    void containerConfigurationApplied() {
        contextRunner
                .withPropertyValues(
                        "arconia.dev.services.lldap.environment.KEY=value",
                        "arconia.dev.services.lldap.environment.LLDAP_JWT_SECRET=letItGoWannaBuildSnowman",
                        "arconia.dev.services.lldap.environment.LLDAP_LDAP_USER_PASS=password",
                        "arconia.dev.services.lldap.network-aliases=network1",
                        "arconia.dev.services.lldap.resources[0].source-path=test-resource.txt",
                        "arconia.dev.services.lldap.resources[0].container-path=/tmp/test-resource.txt"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(LLdapContainer.class);
                    LLdapContainer container = context.getBean(LLdapContainer.class);
                    assertThat(container.getEnv()).contains("KEY=value");
                    assertThat(container.getNetworkAliases()).contains("network1");
                    container.start();
                    assertThat(container.getCurrentContainerInfo().getState().getStatus()).isEqualTo("running");
                    assertThat(container.execInContainer("ls", "/tmp").getStdout()).contains("test-resource.txt");
                    container.stop();
                });
    }

    @Test
    void containerWithRestartScope() {
        contextRunner
                .withClassLoader(this.getClass().getClassLoader())
                .withInitializer(context -> {
                    context.getBeanFactory().registerScope("restart", new SimpleThreadScope());
                })
                .run(context -> {
                    assertThat(context).hasSingleBean(LLdapContainer.class);
                    String[] beanNames = context.getBeanFactory().getBeanNamesForType(LLdapContainer.class);
                    assertThat(beanNames).hasSize(1);
                    assertThat(context.getBeanFactory().getBeanDefinition(beanNames[0]).getScope())
                            .isEqualTo("restart");
                });
    }

}
