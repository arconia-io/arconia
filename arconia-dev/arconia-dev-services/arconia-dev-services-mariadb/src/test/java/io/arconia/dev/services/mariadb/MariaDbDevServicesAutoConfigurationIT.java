package io.arconia.dev.services.mariadb;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.mariadb.MariaDBContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link MariaDbDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class MariaDbDevServicesAutoConfigurationIT {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(RestartScope.class))
            .withConfiguration(AutoConfigurations.of(MariaDbDevServicesAutoConfiguration.class));

    @Test
    void autoConfigurationNotActivatedWhenDisabled() {
        contextRunner
            .withPropertyValues("arconia.dev.services.mariadb.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(MariaDBContainer.class));
    }

    @Test
    void containerAvailableWithDefaultConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(MariaDBContainer.class);
            MariaDBContainer container = context.getBean(MariaDBContainer.class);
            assertThat(container.getDockerImageName()).contains("mariadb");
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
                "arconia.dev.services.mariadb.environment.KEY=value",
                "arconia.dev.services.mariadb.shared=never",
                "arconia.dev.services.mariadb.startup-timeout=90s",
                "arconia.dev.services.mariadb.username=mytest",
                "arconia.dev.services.mariadb.password=mytest",
                "arconia.dev.services.mariadb.db-name=mytest",
                "arconia.dev.services.mariadb.init-script-paths=sql/init.sql"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(MariaDBContainer.class);
                MariaDBContainer container = context.getBean(MariaDBContainer.class);
                assertThat(container.getEnv()).contains("KEY=value");
                assertThat(container.isShouldBeReused()).isFalse();
                container.start();
                assertThat(container.getUsername()).isEqualTo("mytest");
                assertThat(container.getPassword()).isEqualTo("mytest");
                assertThat(container.getDatabaseName()).isEqualTo("mytest");
                assertThat(container.execInContainer("mariadb", "-u", "mytest", "-pmytest", "mytest", "-N", "-e",
                        "SELECT IF(EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'mytest' AND table_name = 'BOOK'), 'true', 'false')")
                        .getStdout())
                        .contains("true");
            });
    }

    @Test
    void containerWithRestartScope() {
        contextRunner
                .withClassLoader(this.getClass().getClassLoader())
                .run(context -> {
                    assertThat(context).hasSingleBean(MariaDBContainer.class);
                    String[] beanNames = context.getBeanFactory().getBeanNamesForType(MariaDBContainer.class);
                    assertThat(beanNames).hasSize(1);
                    assertThat(context.getBeanFactory().getBeanDefinition(beanNames[0]).getScope())
                            .isEqualTo("restart");
                });
    }

}
