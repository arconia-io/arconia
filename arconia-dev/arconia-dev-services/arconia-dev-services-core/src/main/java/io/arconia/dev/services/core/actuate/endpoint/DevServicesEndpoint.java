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
                .map(reg -> new ServiceInfoSummary(reg.name(), reg.description(), ContainerInfoSummary.from(reg.containerInfo().get())))
                .collect(Collectors.toMap(ServiceInfoSummary::name, info -> info));
    }

    @ReadOperation
    public ServiceInfo devService(@Selector String name) {
        return registrations.values().stream()
                .filter(reg -> reg.name().equals(name))
                .findFirst()
                .map(reg -> new ServiceInfo(reg.name(), reg.description(), reg.containerInfo().get()))
                .orElseThrow(() -> new IllegalArgumentException("Dev service not found: " + name));
    }

    public record ServiceInfoSummary(
            String name,
            @Nullable
            String description,
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
            ContainerInfo containerInfo
    ) {}

}
