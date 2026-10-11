package io.arconia.dev.services.postgresql;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.postgresql.PostgreSQLContainer;

import io.arconia.dev.services.core.container.ContainerConfigurer;
import io.arconia.dev.services.tests.BaseDevServiceContainerTests;

import static io.arconia.dev.services.postgresql.ArconiaPostgreSqlContainer.READY_REGEX;
import static io.arconia.dev.services.postgresql.ArconiaPostgreSqlContainer.SKIPPING_INITIALIZATION_REGEX;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ArconiaPostgreSqlContainer}.
 */
class ArconiaPostgreSqlContainerTests extends BaseDevServiceContainerTests<PostgresqlDevServicesProperties, ArconiaPostgreSqlContainer> {

    @Override
    protected PostgresqlDevServicesProperties createProperties() {
        return new PostgresqlDevServicesProperties();
    }

    @Override
    protected ArconiaPostgreSqlContainer createContainer(PostgresqlDevServicesProperties properties) {
        return new ArconiaPostgreSqlContainer(properties);
    }

    @Override
    protected Map<Integer, Integer> configureFixedPorts(PostgresqlDevServicesProperties properties) {
        properties.setPort(1234);
        return Map.of(1234, PostgreSQLContainer.POSTGRESQL_PORT);
    }

    @Test
    void waitStrategyIsOwnedByTheContainerAndFollowsTheStartupTimeout() {
        var properties = new PostgresqlDevServicesProperties();
        var container = new ArconiaPostgreSqlContainer(properties);
        ContainerConfigurer.apply(container, properties);

        // The container declares its own log-based wait strategy, so the startup timeout
        // applies to it alone rather than to the strategy Testcontainers shares by default.
        var waitStrategy = (LogMessageWaitStrategy) ReflectionTestUtils.getField(container, "waitStrategy");
        assertThat(waitStrategy).isNotNull();
        assertThat(ReflectionTestUtils.getField(waitStrategy, "startupTimeout")).isEqualTo(properties.getStartupTimeout());
        assertThat(ReflectionTestUtils.getField(waitStrategy, "regEx")).isEqualTo("(" + READY_REGEX + "|" + SKIPPING_INITIALIZATION_REGEX + ")");
        assertThat(ReflectionTestUtils.getField(waitStrategy, "times")).isEqualTo(2);
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofSeconds(30));
    }

}
