package io.arconia.dev.services.postgresql;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.postgresql.PostgreSQLContainer;

import io.arconia.dev.services.tests.BaseJdbcDevServicesAutoConfigurationTests;

import static io.arconia.dev.services.postgresql.PostgresqlDevServicesAutoConfiguration.READY_REGEX;
import static io.arconia.dev.services.postgresql.PostgresqlDevServicesAutoConfiguration.SKIPPING_INITIALIZATION_REGEX;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PostgresqlDevServicesAutoConfiguration}.
 */
class PostgresqlDevServicesAutoConfigurationTests extends BaseJdbcDevServicesAutoConfigurationTests {

    PostgresqlDevServicesAutoConfigurationTests() {
        super(PostgresqlDevServicesAutoConfiguration.class, "postgresql", PostgreSQLContainer.class);
    }

    @Override
    protected ApplicationContextRunner getContextRunner() {
        // Hide PgVectorStore, so that the default image is the plain PostgreSQL one.
        return super.getContextRunner().withClassLoader(new FilteredClassLoader(PgVectorStore.class));
    }

    @Override
    protected GenericContainer<?> createDiscoverableContainer(String ownerId) {
        var properties = new PostgresqlDevServicesProperties();
        return asDiscoverableContainer(PostgresqlDevServicesAutoConfiguration.createContainer(properties), properties, ownerId);
    }

    @Test
    void waitStrategyIsOwnedByTheContainerAndFollowsTheStartupTimeout() {
        getContextRunner()
                .withPropertyValues("arconia.dev.services.postgresql.startup-timeout=45s")
                .run(context -> {
                    var container = context.getBean(getContainerClass());
                    // The container declares its own log-based wait strategy, so the startup timeout
                    // applies to it alone rather than to the strategy Testcontainers shares by default.
                    var waitStrategy = (LogMessageWaitStrategy) ReflectionTestUtils.getField(container, "waitStrategy");
                    assertThat(waitStrategy).isNotNull();
                    assertThat(ReflectionTestUtils.getField(waitStrategy, "startupTimeout")).isEqualTo(Duration.ofSeconds(45));
                    assertThat(ReflectionTestUtils.getField(waitStrategy, "regEx")).isEqualTo("(" + READY_REGEX + "|" + SKIPPING_INITIALIZATION_REGEX + ")");
                    assertThat(ReflectionTestUtils.getField(waitStrategy, "times")).isEqualTo(2);
                });
    }

    @Test
    void pgVectorImageConfiguredWhenNotSpringAi() {
        getContextRunner()
                .run(context -> {
                    var container = context.getBean(getContainerClass());
                    assertThat(container.getDockerImageName()).contains("postgres");
                });
    }

    @Test
    void pgVectorImageConfiguredWhenSpringAi() {
        getContextRunner()
                // The default context runner hides PgVectorStore: make it visible again.
                .withClassLoader(getClass().getClassLoader())
                .run(context -> {
                    var container = context.getBean(getContainerClass());
                    assertThat(container.getDockerImageName()).contains("pgvector/pgvector");
                });
    }

}
