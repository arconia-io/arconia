package io.arconia.dev.services.core.actuate.endpoint;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;

import io.arconia.dev.services.api.registration.ContainerInfo;
import io.arconia.dev.services.api.registration.DevServiceRegistration;

/**
 * Endpoint for exposing development services information.
 */
@Endpoint(id = "devservices")
public class DevServicesEndpoint {

    private final Map<String, DevServiceRegistration> registrations;

    public DevServicesEndpoint(Map<String, DevServiceRegistration> registrations) {
        this.registrations = registrations;
    }

    @ReadOperation
    public Map<String, ServiceInfoSummary> devServices() {
        return registrations.values().stream()
                .map(reg -> new ServiceInfoSummary(reg.name(), reg.description(), resolveContainerInfoSummary(reg)))
                .collect(Collectors.toMap(ServiceInfoSummary::name, info -> info));
    }

    @ReadOperation
    @Nullable
    public ServiceInfo devService(@Selector String name) {
        DevServiceRegistration registration = registrations.get(name);
        if (registration == null) {
            // A null result is mapped to a 404 response.
            return null;
        }
        return new ServiceInfo(registration.name(), registration.description(), resolveContainerInfo(registration));
    }

    /**
     * Resolve the container information for the given registration, degrading gracefully
     * (null container info) when the container cannot be found, for example because it was
     * removed manually.
     */
    @Nullable
    private static ContainerInfo resolveContainerInfo(DevServiceRegistration registration) {
        try {
            return registration.containerInfo().get();
        } catch (Exception ex) {
            return null;
        }
    }

    @Nullable
    private static ContainerInfoSummary resolveContainerInfoSummary(DevServiceRegistration registration) {
        ContainerInfo containerInfo = resolveContainerInfo(registration);
        return (containerInfo != null) ? ContainerInfoSummary.from(containerInfo) : null;
    }

    public record ServiceInfoSummary(
            String name,
            @Nullable
            String description,
            @Nullable
            ContainerInfoSummary containerInfo
    ) {}

    public record ContainerInfoSummary(
            String id,
            String imageName,
            List<ContainerInfo.ContainerPort> exposedPorts
    ) {

        public static ContainerInfoSummary from(ContainerInfo containerInfo) {
            return new ContainerInfoSummary(containerInfo.id(), containerInfo.imageName(), containerInfo.exposedPorts());
        }

    }

    public record ServiceInfo(
            String name,
            @Nullable
            String description,
            @Nullable
            ContainerInfo containerInfo
    ) {}

}
