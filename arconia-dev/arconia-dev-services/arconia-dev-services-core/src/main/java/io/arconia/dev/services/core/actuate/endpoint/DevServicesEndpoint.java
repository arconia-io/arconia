package io.arconia.dev.services.core.actuate.endpoint;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.endpoint.OperationResponseBody;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;

import io.arconia.dev.services.api.registration.ContainerInfo;
import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.api.registration.DevServiceRegistration;

/**
 * Endpoint for exposing development services information.
 */
@Endpoint(id = "devservices")
public class DevServicesEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(DevServicesEndpoint.class);

    private final Map<String, DevServiceRegistration> registrations;

    /**
     * Create the endpoint for the given registrations, reported in the order of the given map.
     */
    public DevServicesEndpoint(Map<String, DevServiceRegistration> registrations) {
        this.registrations = Collections.unmodifiableMap(new LinkedHashMap<>(registrations));
    }

    @ReadOperation
    public Map<String, ServiceInfo> devServices() {
        Map<String, ServiceInfo> services = new LinkedHashMap<>();
        registrations.forEach((name, registration) -> services.put(name, toServiceInfo(registration)));
        return OperationResponseBody.of(services);
    }

    @ReadOperation
    @Nullable
    public ServiceInfo devService(@Selector String name) {
        DevServiceRegistration registration = registrations.get(name);
        if (registration == null) {
            // A null result is mapped to a 404 response.
            return null;
        }
        return toServiceInfo(registration);
    }

    private static ServiceInfo toServiceInfo(DevServiceRegistration registration) {
        return new ServiceInfo(registration.name(), registration.description(), registration.origin(), registration.links(), resolveContainerInfo(registration));
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
            logger.debug("Failed to resolve container information for the '{}' dev service", registration.name(), ex);
            return null;
        }
    }

    /**
     * Description of a dev service: the container information is {@code null} when the
     * container can no longer be inspected.
     */
    public record ServiceInfo(
            String name,
            String description,
            DevServiceRegistration.Origin origin,
            List<DevServiceLink> links,
            @Nullable
            ContainerInfo containerInfo
    ) implements OperationResponseBody {}

}
