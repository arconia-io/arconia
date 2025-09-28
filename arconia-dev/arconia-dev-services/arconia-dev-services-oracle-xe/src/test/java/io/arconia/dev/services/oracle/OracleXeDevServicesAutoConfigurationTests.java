package io.arconia.dev.services.oracle;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OracleXeDevServicesAutoConfiguration}.
 */
@Disabled("Too slow and heavy for the deployment pipeline")
@EnabledIfDockerAvailable
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
    void containerAvailableWithDefaultConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(OracleContainer.class);
            OracleContainer container = context.getBean(OracleContainer.class);
            assertThat(container.getDockerImageName()).contains("gvenzl/oracle-xe");
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
            .withPropertyValues(
                "arconia.dev.services.oracle-xe.environment.KEY=value",
                "arconia.dev.services.oracle-xe.shared=never",
                "arconia.dev.services.oracle-xe.startup-timeout=90s",
                "arconia.dev.services.oracle-xe.username=mytest",
                "arconia.dev.services.oracle-xe.password=mytest",
                "arconia.dev.services.oracle-xe.db-name=mytest",
                "arconia.dev.services.oracle-xe.init-script-paths=sql/init.sql"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(OracleContainer.class);
                OracleContainer container = context.getBean(OracleContainer.class);
                assertThat(container.getEnv()).contains("KEY=value");
                assertThat(container.isShouldBeReused()).isFalse();
                container.start();
                assertThat(container.getUsername()).isEqualTo("mytest");
                assertThat(container.getPassword()).isEqualTo("mytest");
                assertThat(container.getDatabaseName()).isEqualTo("mytest");
            });
    }

}
