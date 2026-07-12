package io.arconia.dev.services.core.autoconfigure;

import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Bean;

import io.arconia.boot.autoconfigure.bootstrap.ConditionalOnDevMode;
import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.core.actuate.endpoint.DevServicesEndpoint;

/**
 * Auto-configuration for the Dev Services Actuator endpoint.
 */
@AutoConfiguration(after = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevMode
@ConditionalOnClass(Endpoint.class)
public class DevServicesEndpointAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnAvailableEndpoint
    DevServicesEndpoint devServicesEndpoint(ObjectProvider<DevServiceRegistration> registrations) {
        return new DevServicesEndpoint(registrations.orderedStream().collect(Collectors.toMap(
                DevServiceRegistration::name, Function.identity(), (first, second) -> first)));
    }

}
