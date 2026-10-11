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
 * Endpoint for exposing dev services information.
 */
@Endpoint(id = "devservices")
public class DevServicesEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(DevServicesEndpoint.class);

    private final Map<String, DevServiceRegistration> registrations;

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

    @Nullable
    private static ContainerInfo resolveContainerInfo(DevServiceRegistration registration) {
        try {
            return registration.containerInfo().get();
        } catch (Exception ex) {
            // For example, the container may have been removed manually.
            logger.debug("Failed to resolve container information for the '{}' dev service", registration.name(), ex);
            return null;
        }
    }

    public record ServiceInfo(
            String name,
            String description,
            DevServiceRegistration.Origin origin,
            List<DevServiceLink> links,
            @Nullable
            ContainerInfo containerInfo
    ) implements OperationResponseBody {}

}
