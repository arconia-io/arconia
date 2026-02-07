package io.arconia.dev.services.oracle;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.support.SimpleThreadScope;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.oracle.OracleContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OracleDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class OracleDevServicesAutoConfigurationIT {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(RestartScope.class))
            .withConfiguration(AutoConfigurations.of(OracleDevServicesAutoConfiguration.class));

    @Test
    void autoConfigurationNotActivatedWhenGloballyDisabled() {
        contextRunner
                .withPropertyValues("arconia.dev.services.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(OracleContainer.class));
    }

    @Test
    void autoConfigurationNotActivatedWhenDisabled() {
        contextRunner
                .withPropertyValues("arconia.dev.services.oracle.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(OracleContainer.class));
    }

    @Test
    @Disabled("Too slow and heavy for the deployment pipeline.")
    void containerAvailableWithDefaultConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(OracleContainer.class);
            OracleContainer container = context.getBean(OracleContainer.class);
            assertThat(container.getDockerImageName()).contains("gvenzl/oracle-free");
            assertThat(container.getEnv()).isEmpty();
            assertThat(container.getNetworkAliases()).hasSize(1);
            assertThat(container.isShouldBeReused()).isFalse();
            container.start();
            assertThat(container.getUsername()).isEqualTo("test");
            assertThat(container.getPassword()).isEqualTo("test");
            assertThat(container.getDatabaseName()).isEqualTo("test");
            container.stop();

            String[] beanNames = context.getBeanFactory().getBeanNamesForType(OracleContainer.class);
            assertThat(beanNames).hasSize(1);
            assertThat(context.getBeanFactory().getBeanDefinition(beanNames[0]).getScope())
                    .isEqualTo("singleton");
        });
    }

    @Test
    @Disabled("Too slow and heavy for the deployment pipeline.")
    void containerConfigurationApplied() {
        contextRunner
                .withPropertyValues(
                        "arconia.dev.services.oracle.environment.KEY=value",
                        "arconia.dev.services.oracle.network-aliases=network1",
                        "arconia.dev.services.oracle.resources[0].source-path=test-resource.txt",
                        "arconia.dev.services.oracle.resources[0].container-path=/tmp/test-resource.txt",
                        "arconia.dev.services.oracle.username=mytest",
                        "arconia.dev.services.oracle.password=mytest",
                        "arconia.dev.services.oracle.db-name=mytest",
                        "arconia.dev.services.oracle.init-script-paths=sql/init.sql"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(OracleContainer.class);
                    OracleContainer container = context.getBean(OracleContainer.class);
                    assertThat(container.getEnv()).contains("KEY=value");
                    assertThat(container.getNetworkAliases()).contains("network1");
                    container.start();
                    assertThat(container.getCurrentContainerInfo().getState().getStatus()).isEqualTo("running");
                    assertThat(container.execInContainer("ls", "/tmp").getStdout()).contains("test-resource.txt");
                    assertThat(container.getUsername()).isEqualTo("mytest");
                    assertThat(container.getPassword()).isEqualTo("mytest");
                    assertThat(container.getDatabaseName()).isEqualTo("mytest");
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
                    assertThat(context).hasSingleBean(OracleContainer.class);
                    String[] beanNames = context.getBeanFactory().getBeanNamesForType(OracleContainer.class);
                    assertThat(beanNames).hasSize(1);
                    assertThat(context.getBeanFactory().getBeanDefinition(beanNames[0]).getScope())
                            .isEqualTo("restart");
                });
    }

}
