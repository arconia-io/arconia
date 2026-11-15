package io.arconia.dev.services.mariadb;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.mariadb.MariaDBContainer;

import io.arconia.dev.services.tests.BaseJdbcDevServicesAutoConfigurationTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MariaDbDevServicesAutoConfiguration}.
 */
class MariaDbDevServicesAutoConfigurationTests extends BaseJdbcDevServicesAutoConfigurationTests {

    MariaDbDevServicesAutoConfigurationTests() {
        super(MariaDbDevServicesAutoConfiguration.class, "mariadb", MariaDBContainer.class);
    }

    @Override
    protected GenericContainer<?> createDiscoverableContainer(String ownerId) {
        var properties = new MariaDbDevServicesProperties();
        return asDiscoverableContainer(MariaDbDevServicesAutoConfiguration.createContainer(properties), properties, ownerId);
    }

    @Test
    void waitStrategyIsOwnedByTheContainerAndFollowsTheStartupTimeout() {
        getContextRunner()
                .withPropertyValues("arconia.dev.services.mariadb.startup-timeout=45s")
                .run(context -> {
                    var container = context.getBean(getContainerClass());
                    // The container gets a wait strategy of its own, so the startup timeout applies
                    // to it alone rather than to the strategy Testcontainers shares by default.
                    var waitStrategy = ReflectionTestUtils.getField(container, "waitStrategy");
                    assertThat(waitStrategy).isNotNull();
                    assertThat(ReflectionTestUtils.getField(waitStrategy, "startupTimeout")).isEqualTo(Duration.ofSeconds(45));
                    try (var other = new GenericContainer<>("alpine:latest")) {
                        assertThat(waitStrategy).isNotSameAs(ReflectionTestUtils.getField(other, "waitStrategy"));
                    }
                });
    }

}
