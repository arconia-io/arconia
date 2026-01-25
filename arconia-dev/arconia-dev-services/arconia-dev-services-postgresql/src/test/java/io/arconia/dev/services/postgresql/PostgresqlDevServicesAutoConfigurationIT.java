package io.arconia.dev.services.postgresql;

import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.support.SimpleThreadScope;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link PostgresqlDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class PostgresqlDevServicesAutoConfigurationIT {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(RestartScope.class, PgVectorStore.class))
            .withConfiguration(AutoConfigurations.of(PostgresqlDevServicesAutoConfiguration.class));

    @Test
    void autoConfigurationNotActivatedWhenGloballyDisabled() {
        contextRunner
                .withPropertyValues("arconia.dev.services.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(PostgreSQLContainer.class));
    }

    @Test
    void autoConfigurationNotActivatedWhenDisabled() {
        contextRunner
                .withPropertyValues("arconia.dev.services.postgresql.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(PostgreSQLContainer.class));
    }

    @Test
    void containerAvailableWithDefaultConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(PostgreSQLContainer.class);
            PostgreSQLContainer container = context.getBean(PostgreSQLContainer.class);
            assertThat(container.getDockerImageName()).contains("postgres");
            assertThat(container.getEnv()).isEmpty();
            assertThat(container.getNetworkAliases()).hasSize(1);
            assertThat(container.isShouldBeReused()).isFalse();
            container.start();
            assertThat(container.getUsername()).isEqualTo("test");
            assertThat(container.getPassword()).isEqualTo("test");
            assertThat(container.getDatabaseName()).isEqualTo("test");
            container.stop();

            String[] beanNames = context.getBeanFactory().getBeanNamesForType(PostgreSQLContainer.class);
            assertThat(beanNames).hasSize(1);
            assertThat(context.getBeanFactory().getBeanDefinition(beanNames[0]).getScope())
                    .isEqualTo("singleton");
        });
    }

    @Test
    void containerConfigurationApplied() {
        contextRunner
            .withPropertyValues(
                    "arconia.dev.services.postgresql.environment.KEY=value",
                    "arconia.dev.services.postgresql.network-aliases=network1",
                    "arconia.dev.services.postgresql.username=mytest",
                    "arconia.dev.services.postgresql.password=mytest",
                    "arconia.dev.services.postgresql.db-name=mytest",
                    "arconia.dev.services.postgresql.init-script-paths=sql/init.sql"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(PostgreSQLContainer.class);
                PostgreSQLContainer container = context.getBean(PostgreSQLContainer.class);
                assertThat(container.getEnv()).contains("KEY=value");
                assertThat(container.getNetworkAliases()).contains("network1");
                container.start();
                assertThat(container.getUsername()).isEqualTo("mytest");
                assertThat(container.getPassword()).isEqualTo("mytest");
                assertThat(container.getDatabaseName()).isEqualTo("mytest");
                assertThat(container.execInContainer("psql", "-U", "mytest", "-d", "mytest", "-t", "-A", "-c",
                    "SELECT EXISTS (SELECT FROM pg_tables WHERE tablename = 'book')::text")
                    .getStdout())
                    .contains("true");
                container.stop();
            });
    }

    @Test
    void containerStartsAndStopsSuccessfully() {
        contextRunner
                .run(context -> {
                    assertThat(context).hasSingleBean(PostgreSQLContainer.class);
                    PostgreSQLContainer container = context.getBean(PostgreSQLContainer.class);
                    container.start();
                    assertThat(container.getCurrentContainerInfo().getState().getStatus()).isEqualTo("running");
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
                    assertThat(context).hasSingleBean(PostgreSQLContainer.class);
                    String[] beanNames = context.getBeanFactory().getBeanNamesForType(PostgreSQLContainer.class);
                    assertThat(beanNames).hasSize(1);
                    assertThat(context.getBeanFactory().getBeanDefinition(beanNames[0]).getScope())
                            .isEqualTo("restart");
                });
    }

    @Test
    void pgVectorImageConfiguredWhenNotSpringAi() {
        contextRunner
                .run(context -> {
                    assertThat(context).hasSingleBean(PostgreSQLContainer.class);
                    PostgreSQLContainer container = context.getBean(PostgreSQLContainer.class);
                    assertThat(container.getDockerImageName()).contains("postgres");
                });
    }

    @Test
    void pgVectorImageConfiguredWhenSpringAi() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(RestartScope.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(PostgreSQLContainer.class);
                    PostgreSQLContainer container = context.getBean(PostgreSQLContainer.class);
                    assertThat(container.getDockerImageName()).contains("pgvector/pgvector");
                });
    }

}
