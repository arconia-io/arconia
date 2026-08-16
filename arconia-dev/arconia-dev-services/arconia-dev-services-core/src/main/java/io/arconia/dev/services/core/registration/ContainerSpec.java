package io.arconia.dev.services.core.registration;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.Container;

import io.arconia.core.support.Incubating;

/**
 * Specification for a container to register.
 */
@Incubating
public final class ContainerSpec {

    @Nullable
    private Class<? extends Container<?>> type;

    @Nullable
    private Supplier<? extends Container<?>> supplier;

    private boolean serviceConnectionSupported = true;

    @Nullable
    private String serviceConnectionName;

    ContainerSpec() {}

    /**
     * The container type to register.
     */
    public ContainerSpec type(Class<? extends Container<?>> type) {
        this.type = type;
        return this;
    }

    /**
     * A supplier function providing the container instance.
     */
    public ContainerSpec supplier(Supplier<? extends Container<?>> supplier) {
        this.supplier = supplier;
        return this;
    }

    /**
     * The name of the {@link ServiceConnection} annotation to add to the registered container bean.
     * <p>
     * By default, {@code @ServiceConnection} is added with no explicit name,
     * and Spring Boot auto-detects the connection details factory by container type.
     * <p>
     * Passing a non-null value sets the {@code @ServiceConnection} name explicitly.
     * Passing {@code null} disables {@code @ServiceConnection} entirely, for cases where
     * no {@code ContainerConnectionDetailsFactory} is available and property-based wiring
     * (via {@link DevServiceDynamicPropertySource}) is used instead.
     */
    public ContainerSpec serviceConnectionName(@Nullable String name) {
        this.serviceConnectionName = name;
        this.serviceConnectionSupported = (name != null);
        return this;
    }

    @Nullable
    Class<? extends Container<?>> getType() {
        return type;
    }

    @Nullable
    Supplier<? extends Container<?>> getSupplier() {
        return supplier;
    }

    boolean isServiceConnectionSupported() {
        return serviceConnectionSupported;
    }

    @Nullable
    String getServiceConnectionName() {
        return serviceConnectionName;
    }

}
