package io.arconia.dev.services.core.registration;

import java.util.function.Function;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;

/**
 * Specification for discovering and adopting a shared dev service among applications.
 * <p>
 * When sharing is enabled, the dev service container is discoverable by other
 * applications, and the application connects to an existing shared container
 * (adopted as it runs, with the owning application's configuration)
 * if available instead of starting a new one.
 */
@Incubating
public final class DiscoverySpec {

    private boolean shared = false;

    @Nullable
    private Class<? extends ConnectionDetails> connectionDetailsType;

    @Nullable
    private Function<DiscoveredContainer, ? extends ConnectionDetails> connectionDetails;

    DiscoverySpec() {}

    /**
     * Whether sharing is enabled for the dev service,
     * typically bound to the {@code shared} configuration property.
     */
    public DiscoverySpec shared(boolean shared) {
        this.shared = shared;
        return this;
    }

    /**
     * The type of {@link ConnectionDetails} provided for the dev service and a factory
     * function providing the instance for connecting to a shared dev service discovered
     * in a running container.
     * <p>
     * The declared type is used to look up existing user-defined {@code ConnectionDetails}
     * beans: when one is present, it takes precedence over the one provided by the dev service.
     */
    public <T extends ConnectionDetails> DiscoverySpec connectionDetails(Class<T> connectionDetailsType,
            Function<DiscoveredContainer, ? extends T> connectionDetails) {
        Assert.state(this.connectionDetails == null, "discovery supports a single connection details contribution");
        this.connectionDetailsType = connectionDetailsType;
        this.connectionDetails = connectionDetails;
        return this;
    }

    boolean isShared() {
        return shared;
    }

    @Nullable
    Class<? extends ConnectionDetails> getConnectionDetailsType() {
        return connectionDetailsType;
    }

    @Nullable
    Function<DiscoveredContainer, ? extends ConnectionDetails> getConnectionDetails() {
        return connectionDetails;
    }

}
