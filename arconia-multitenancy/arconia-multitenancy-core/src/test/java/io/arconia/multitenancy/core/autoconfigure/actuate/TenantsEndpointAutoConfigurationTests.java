package io.arconia.multitenancy.core.autoconfigure.actuate;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import io.arconia.multitenancy.core.actuate.endpoint.TenantsEndpoint;
import io.arconia.multitenancy.core.autoconfigure.MultitenancyCoreAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TenantsEndpointAutoConfiguration}.
 */
class TenantsEndpointAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(MultitenancyCoreAutoConfiguration.class,
                TenantsEndpointAutoConfiguration.class));

    @Test
    void tenantsEndpointWhenNoTenantDetailsService() {
        contextRunner.withPropertyValues("management.endpoints.web.exposure.include=tenants").run(context -> {
            assertThat(context).doesNotHaveBean(TenantsEndpoint.class);
        });
    }

    @Test
    void tenantsEndpointWhenNotExposed() {
        contextRunner.withPropertyValues("arconia.multitenancy.details.tenants[0].identifier=acme").run(context -> {
            assertThat(context).doesNotHaveBean(TenantsEndpoint.class);
        });
    }

    @Test
    void tenantsEndpointWhenExposed() {
        contextRunner
            .withPropertyValues("arconia.multitenancy.details.tenants[0].identifier=acme",
                    "management.endpoints.web.exposure.include=tenants")
            .run(context -> {
                assertThat(context).hasSingleBean(TenantsEndpoint.class);
            });
    }

    @Test
    void tenantsEndpointWhenActuatorNotOnClasspath() {
        contextRunner.withClassLoader(new FilteredClassLoader(Endpoint.class))
            .withPropertyValues("arconia.multitenancy.details.tenants[0].identifier=acme")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(TenantsEndpoint.class);
            });
    }

}
