package io.arconia.dev.services.core.container;

import org.testcontainers.containers.Container;

import io.arconia.core.support.Incubating;

/**
 * Callback interface for customizing the container of a dev service before it is started.
 * <p>
 * Beans of this type are applied to any dev service container that is an instance of the
 * generic type {@code T}, in {@code @Order} semantics, right after the container is created
 * and before it is started. This is the extension point for configuration that goes beyond
 * the {@code arconia.dev.services.*} properties, such as custom wait strategies,
 * startup check strategies, commands, or labels.
 * <p>
 * Example (using {@code org.testcontainers.postgresql.PostgreSQLContainer}):
 * <pre>{@code
 * @Bean
 * DevServiceContainerCustomizer<PostgreSQLContainer> postgresCustomizer() {
 *     return container -> container.waitingFor(Wait.forListeningPort());
 * }
 * }</pre>
 *
 * @param <T> the container type this customizer applies to
 */
@Incubating
@FunctionalInterface
public interface DevServiceContainerCustomizer<T extends Container<?>> {

    /**
     * Customize the given container.
     */
    void customize(T container);

}
