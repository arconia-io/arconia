package io.arconia.dev.services.oracle;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.OracleContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OracleXeDevServicesAutoConfiguration}.
 */
class OracleXeDevServicesAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(RestartScope.class))
            .withConfiguration(AutoConfigurations.of(OracleXeDevServicesAutoConfiguration.class));

    @Test
    void autoConfigurationNotActivatedWhenDisabled() {
        contextRunner
            .withPropertyValues("arconia.dev.services.oracle-xe.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(OracleContainer.class));
    }

    @Test
    void oracleXeContainerAvailableWithDefaultConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(OracleContainer.class);
            OracleContainer container = context.getBean(OracleContainer.class);
            assertThat(container.getDockerImageName()).contains("gvenzl/oracle-xe");
            assertThat(container.isShouldBeReused()).isFalse();
        });
    }

    @Test
    void oracleXeContainerConfigurationApplied() {
        contextRunner
            .withPropertyValues(
                "arconia.dev.services.oracle-xe.image-name=docker.io/gvenzl/oracle-xe",
                "arconia.dev.services.oracle-xe.environment.ORACLE_PASSWORD=secret",
                "arconia.dev.services.oracle-xe.shared=never"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(OracleContainer.class);
                OracleContainer container = context.getBean(OracleContainer.class);
                assertThat(container.getDockerImageName()).contains("docker.io/gvenzl/oracle-xe");
                assertThat(container.getEnv()).contains("ORACLE_PASSWORD=secret");
                assertThat(container.isShouldBeReused()).isFalse();
            });
    }

}
