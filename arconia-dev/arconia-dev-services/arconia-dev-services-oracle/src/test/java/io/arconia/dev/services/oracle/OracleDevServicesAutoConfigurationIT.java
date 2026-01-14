package io.arconia.dev.services.oracle;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.oracle.OracleContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OracleDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
@Disabled("Too slow and heavy for the deployment pipeline")
class OracleDevServicesAutoConfigurationIT {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(RestartScope.class))
            .withConfiguration(AutoConfigurations.of(OracleDevServicesAutoConfiguration.class));

    @Test
    void autoConfigurationNotActivatedWhenDisabled() {
        contextRunner
            .withPropertyValues("arconia.dev.services.oracle.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(OracleContainer.class));
    }

    @Test
    void containerAvailableWithDefaultConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(OracleContainer.class);
            OracleContainer container = context.getBean(OracleContainer.class);
            assertThat(container.getDockerImageName()).contains("gvenzl/oracle-free");
            assertThat(container.getEnv()).isEmpty();
            assertThat(container.isShouldBeReused()).isFalse();
            container.start();
            assertThat(container.getUsername()).isEqualTo("test");
            assertThat(container.getPassword()).isEqualTo("test");
            assertThat(container.getDatabaseName()).isEqualTo("test");
        });
    }

    @Test
    void containerConfigurationApplied() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .withPropertyValues(
                        "arconia.dev.services.oracle.port=1234",
                        "arconia.dev.services.oracle.environment.KEY=value",
                        "arconia.dev.services.oracle.shared=never",
                        "arconia.dev.services.oracle.startup-timeout=90s",
                        "arconia.dev.services.oracle.username=mytest",
                        "arconia.dev.services.oracle.password=mytest",
                        "arconia.dev.services.oracle.db-name=mytest",
                        "arconia.dev.services.oracle.init-script-paths=sql/init.sql"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(OracleContainer.class);
                    OracleContainer container = context.getBean(OracleContainer.class);
                    assertThat(container.getEnv()).contains("KEY=value");
                    assertThat(container.isShouldBeReused()).isFalse();

                    container.start();
                    assertThat(container.getMappedPort(ArconiaOracleContainer.ORACLE_PORT)).isEqualTo(1234);
                    assertThat(container.getUsername()).isEqualTo("mytest");
                    assertThat(container.getPassword()).isEqualTo("mytest");
                    assertThat(container.getDatabaseName()).isEqualTo("mytest");

                });
    }

    @Test
    void containerWithRestartScope() {
        contextRunner
                .withClassLoader(this.getClass().getClassLoader())
                .run(context -> {
                    assertThat(context).hasSingleBean(OracleContainer.class);
                    String[] beanNames = context.getBeanFactory().getBeanNamesForType(OracleContainer.class);
                    assertThat(beanNames).hasSize(1);
                    assertThat(context.getBeanFactory().getBeanDefinition(beanNames[0]).getScope())
                            .isEqualTo("restart");
                });
    }

}
