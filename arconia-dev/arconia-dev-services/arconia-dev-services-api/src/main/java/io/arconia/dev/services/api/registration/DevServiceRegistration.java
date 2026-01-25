package io.arconia.dev.services.api.registration;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;

/**
 * Describes a registered dev service.
 */
@Incubating
public record DevServiceRegistration(
        String name,
        @Nullable
        String description,
        Supplier<ContainerInfo> containerInfo
) {

    public DevServiceRegistration {
        Assert.hasText(name, "name cannot be null or empty");
        Assert.notNull(containerInfo, "containerInfo cannot be null");
    }

}
