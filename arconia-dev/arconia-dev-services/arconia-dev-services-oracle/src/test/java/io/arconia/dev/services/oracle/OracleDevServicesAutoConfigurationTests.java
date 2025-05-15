package io.arconia.dev.services.oracle;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.oracle.OracleContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OracleDevServicesAutoConfiguration}.
 */
class OracleDevServicesAutoConfigurationTests {

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
    void oracleContainerAvailableWithDefaultConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(OracleContainer.class);
            OracleContainer container = context.getBean(OracleContainer.class);
            assertThat(container.getDockerImageName()).contains("gvenzl/oracle-free");
            assertThat(container.isShouldBeReused()).isFalse();
        });
    }

    @Test
    void oracleContainerConfigurationApplied() {
        contextRunner
            .withPropertyValues(
                "arconia.dev.services.oracle.image-name=docker.io/gvenzl/oracle-free",
                "arconia.dev.services.oracle.environment.ORACLE_PASSWORD=secret",
                "arconia.dev.services.oracle.shared=never"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(OracleContainer.class);
                OracleContainer container = context.getBean(OracleContainer.class);
                assertThat(container.getDockerImageName()).contains("docker.io/gvenzl/oracle-free");
                assertThat(container.getEnv()).contains("ORACLE_PASSWORD=secret");
                assertThat(container.isShouldBeReused()).isFalse();
            });
    }

}
