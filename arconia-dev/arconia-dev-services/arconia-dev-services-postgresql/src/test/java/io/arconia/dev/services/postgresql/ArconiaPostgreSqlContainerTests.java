package io.arconia.dev.services.postgresql;

import java.lang.reflect.Field;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.util.ReflectionUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static io.arconia.dev.services.postgresql.ArconiaPostgreSqlContainer.READY_REGEX;
import static io.arconia.dev.services.postgresql.ArconiaPostgreSqlContainer.SKIPPING_INITIALIZATION_REGEX;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ArconiaPostgreSqlContainer}.
 */
class ArconiaPostgreSqlContainerTests {

    @Test
    void whenExposedPortsAreNotConfigured() {
        var container = new ArconiaPostgreSqlContainer(new PostgresqlDevServicesProperties());
        container.configure();
        assertThat(container.getPortBindings()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void whenExposedPortsAreConfigured() {
        var properties = new PostgresqlDevServicesProperties();
        properties.setPort(1234);

        var container = new ArconiaPostgreSqlContainer(properties);
        container.configure();

        var portBindings = container.getPortBindings();
        assertThat(portBindings).isNotNull();
        assertThat(portBindings)
                .anyMatch(binding -> binding.startsWith(
                        properties.getPort() + ":" + PostgreSQLContainer.POSTGRESQL_PORT));
    }

    @Test
    void withCustomWaitStrategy() {
        var properties = new PostgresqlDevServicesProperties();
        var container = new ArconiaPostgreSqlContainer(properties);
        var waitStrategy = getWaitStrategy(container);

        Duration actualTimeout = getStartupTimeout(waitStrategy);
        assertThat(actualTimeout).isEqualTo(properties.getStartupTimeout());

        String regex = getWaitStrategyRegex(waitStrategy);
        assertThat(regex).isEqualTo("(" + READY_REGEX + ")?(" + SKIPPING_INITIALIZATION_REGEX + ")?");

        int times = getWaitStrategyTimes(waitStrategy);
        assertThat(times).isEqualTo(2);
    }

    /**
     * Helper method to extract the WaitStrategy from a GenericContainer using reflection.
     */
    private LogMessageWaitStrategy getWaitStrategy(GenericContainer<?> container) {
        Field waitStrategyField = ReflectionUtils.findField(GenericContainer.class, "waitStrategy");
        assertThat(waitStrategyField).isNotNull();
        ReflectionUtils.makeAccessible(waitStrategyField);
        return (LogMessageWaitStrategy) ReflectionUtils.getField(waitStrategyField, container);
    }

    /**
     * Helper method to extract the startup timeout from a WaitStrategy using reflection.
     */
    private Duration getStartupTimeout(LogMessageWaitStrategy waitStrategy) {
        Field startupTimeoutField = ReflectionUtils.findField(waitStrategy.getClass(), "startupTimeout");
        assertThat(startupTimeoutField).isNotNull();
        ReflectionUtils.makeAccessible(startupTimeoutField);
        return (Duration) ReflectionUtils.getField(startupTimeoutField, waitStrategy);
    }

    /**
     * Helper method to extract the regex from a WaitStrategy using reflection.
     */
    private String getWaitStrategyRegex(LogMessageWaitStrategy waitStrategy) {
        Field regexField = ReflectionUtils.findField(waitStrategy.getClass(), "regEx");
        assertThat(regexField).isNotNull();
        ReflectionUtils.makeAccessible(regexField);
        return (String) ReflectionUtils.getField(regexField, waitStrategy);
    }

    /**
     * Helper method to extract the times from a WaitStrategy using reflection.
     */
    private Integer getWaitStrategyTimes(LogMessageWaitStrategy waitStrategy) {
        Field timesField = ReflectionUtils.findField(waitStrategy.getClass(), "times");
        assertThat(timesField).isNotNull();
        ReflectionUtils.makeAccessible(timesField);
        return (Integer) ReflectionUtils.getField(timesField, waitStrategy);
    }

}
