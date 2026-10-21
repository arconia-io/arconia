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
import org.springframework.boot.info.SslInfo;
import org.springframework.boot.info.SslInfo.BundleInfo;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.util.StringUtils;

import io.arconia.core.support.Incubating;
import io.arconia.dev.services.api.registration.ContainerInfo;
import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.api.registration.DevServiceRegistration;

/**
 * Endpoint for exposing dev services information.
 */
@Incubating
@Endpoint(id = "devservices")
public class DevServicesEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(DevServicesEndpoint.class);

    private final Map<String, DevServiceRegistration> registrations;

    @Nullable
    private final SslInfo sslInfo;

    public DevServicesEndpoint(Map<String, DevServiceRegistration> registrations, @Nullable SslBundles sslBundles) {
        this.registrations = Collections.unmodifiableMap(new LinkedHashMap<>(registrations));
        this.sslInfo = (sslBundles != null) ? new SslInfo(sslBundles) : null;
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

    private ServiceInfo toServiceInfo(DevServiceRegistration registration) {
        return new ServiceInfo(registration.getName(), registration.getDescription(), registration.getCategory(), registration.getOrigin(),
                registration.getLinks(), resolveContainerInfo(registration), resolveSslInfo(registration));
    }

    @Nullable
    private static ContainerInfo resolveContainerInfo(DevServiceRegistration registration) {
        try {
            return registration.getContainerInfo().get();
        } catch (Exception ex) {
            // For example, the container may have been removed manually.
            logger.debug("Dev Service '{}': failed to resolve container information", registration.getName(), ex);
            return null;
        }
    }

    @Nullable
    private BundleInfo resolveSslInfo(DevServiceRegistration registration) {
        String bundleName = registration.getSslBundleName();
        if (!StringUtils.hasText(bundleName) || sslInfo == null) {
            return null;
        }
        try {
            return sslInfo.getBundle(bundleName);
        } catch (Exception ex) {
            logger.debug("Dev Service '{}': failed to resolve the SSL bundle '{}'", registration.getName(), bundleName, ex);
            return null;
        }
    }

    public record ServiceInfo(
            String name,
            String description,
            @Nullable
            String category,
            DevServiceRegistration.Origin origin,
            List<DevServiceLink> links,
            @Nullable
            ContainerInfo containerInfo,
            @Nullable
            BundleInfo ssl
    ) implements OperationResponseBody {}

}
