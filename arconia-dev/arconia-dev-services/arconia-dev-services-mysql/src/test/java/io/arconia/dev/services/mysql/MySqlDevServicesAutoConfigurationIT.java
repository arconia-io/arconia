package io.arconia.dev.services.mysql;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.support.SimpleThreadScope;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.mysql.MySQLContainer;

import static io.arconia.dev.services.mysql.MySqlDevServicesProperties.DEFAULT_DB_NAME;
import static io.arconia.dev.services.mysql.MySqlDevServicesProperties.DEFAULT_PASSWORD;
import static io.arconia.dev.services.mysql.MySqlDevServicesProperties.DEFAULT_USERNAME;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link MySqlDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class MySqlDevServicesAutoConfigurationIT {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(RestartScope.class))
            .withConfiguration(AutoConfigurations.of(MySqlDevServicesAutoConfiguration.class));

    @Test
    void autoConfigurationNotActivatedWhenGloballyDisabled() {
        contextRunner
                .withPropertyValues("arconia.dev.services.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(MySQLContainer.class));
    }

    @Test
    void autoConfigurationNotActivatedWhenDisabled() {
        contextRunner
                .withPropertyValues("arconia.dev.services.mysql.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(MySQLContainer.class));
    }

    @Test
    void containerAvailableWithDefaultConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(MySQLContainer.class);
            MySQLContainer container = context.getBean(MySQLContainer.class);
            assertThat(container.getDockerImageName()).contains("mysql");
            assertThat(container.getEnv()).isEmpty();
            assertThat(container.getNetworkAliases()).hasSize(1);
            assertThat(container.isShouldBeReused()).isFalse();
            container.start();
            assertThat(container.getUsername()).isEqualTo(DEFAULT_USERNAME);
            assertThat(container.getPassword()).isEqualTo(DEFAULT_PASSWORD);
            assertThat(container.getDatabaseName()).isEqualTo(DEFAULT_DB_NAME);
            container.stop();

            String[] beanNames = context.getBeanFactory().getBeanNamesForType(MySQLContainer.class);
            assertThat(beanNames).hasSize(1);
            assertThat(context.getBeanFactory().getBeanDefinition(beanNames[0]).getScope())
                    .isEqualTo("singleton");
        });
    }

    @Test
    void containerConfigurationApplied() {
        contextRunner
                .withPropertyValues(
                        "arconia.dev.services.mysql.environment.KEY=value",
                        "arconia.dev.services.mysql.network-aliases=network1",
                        "arconia.dev.services.mysql.resources[0].source-path=test-resource.txt",
                        "arconia.dev.services.mysql.resources[0].container-path=/tmp/test-resource.txt",
                        "arconia.dev.services.mysql.username=mytest",
                        "arconia.dev.services.mysql.password=mytest",
                        "arconia.dev.services.mysql.db-name=mytest",
                        "arconia.dev.services.mysql.init-script-paths=sql/init.sql"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(MySQLContainer.class);
                    MySQLContainer container = context.getBean(MySQLContainer.class);
                    assertThat(container.getEnv()).contains("KEY=value");
                    assertThat(container.getNetworkAliases()).contains("network1");
                    container.start();
                    assertThat(container.getCurrentContainerInfo().getState().getStatus()).isEqualTo("running");
                    assertThat(container.execInContainer("ls", "/tmp").getStdout()).contains("test-resource.txt");
                    assertThat(container.getUsername()).isEqualTo("mytest");
                    assertThat(container.getPassword()).isEqualTo("mytest");
                    assertThat(container.getDatabaseName()).isEqualTo("mytest");
                    assertThat(container.execInContainer("mysql", "-u", "mytest", "-pmytest", "mytest", "-N", "-e",
                            "SELECT IF(EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'mytest' AND table_name = 'BOOK'), 'true', 'false')")
                            .getStdout())
                            .contains("true");
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
                    assertThat(context).hasSingleBean(MySQLContainer.class);
                    String[] beanNames = context.getBeanFactory().getBeanNamesForType(MySQLContainer.class);
                    assertThat(beanNames).hasSize(1);
                    assertThat(context.getBeanFactory().getBeanDefinition(beanNames[0]).getScope())
                            .isEqualTo("restart");
                });
    }

}
