package io.arconia.dev.services.core.autoconfigure;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.arconia.boot.bootstrap.BootstrapMode;
import io.arconia.dev.services.api.registration.ContainerInfo;
import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.core.actuate.endpoint.DevServicesEndpoint;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevServicesEndpointAutoConfiguration}.
 */
class DevServicesEndpointAutoConfigurationTests {

    private static final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DevServicesEndpointAutoConfiguration.class))
            .withPropertyValues("management.endpoints.web.exposure.include=devservices");

    @BeforeEach
    @AfterEach
    void resetBootstrapMode() {
        BootstrapMode.clear();
    }

    @Test
    void endpointBeanIsAvailableWhenDevModeIsEnabled() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .run(context -> assertThat(context).hasSingleBean(DevServicesEndpoint.class));
    }

    @Test
    void endpointBeanIsNotAvailableWhenProdModeIsEnabled() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=prod")
                .run(context -> assertThat(context).doesNotHaveBean(DevServicesEndpoint.class));
    }

    @Test
    void endpointBeanUsesCustomBeanWhenProvided() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .withUserConfiguration(CustomEndpointConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(DevServicesEndpoint.class);
                    assertThat(context.getBean(DevServicesEndpoint.class))
                            .isSameAs(CustomEndpointConfiguration.customEndpoint);
                });
    }

    @Test
    void endpointBeanIsCreatedWithRegistrations() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .withUserConfiguration(RegistrationsConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(DevServicesEndpoint.class);
                    DevServicesEndpoint endpoint = context.getBean(DevServicesEndpoint.class);
                    assertThat(endpoint.devServices()).containsOnlyKeys("postgresql", "docling");
                });
    }

    @Test
    void endpointBeanIsCreatedWhenRegistrationsShareAName() {
        contextRunner
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .withUserConfiguration(RegistrationsConfiguration.class, DuplicateRegistrationConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(DevServicesEndpoint.class).devServices()).containsOnlyKeys("postgresql", "docling");
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomEndpointConfiguration {

        static final DevServicesEndpoint customEndpoint = new DevServicesEndpoint(Map.of());

        @Bean
        DevServicesEndpoint devServicesEndpoint() {
            return customEndpoint;
        }

    }

    @Configuration(proxyBeanMethods = false)
    static class RegistrationsConfiguration {

        @Bean
        DevServiceRegistration postgresqlRegistration() {
            return registration("postgresql", "PostgreSQL Database", "postgres:18", "1234");
        }

        @Bean
        DevServiceRegistration doclingRegistration() {
            return registration("docling", "Docling Serve", "docling:1.10", "5678");
        }

    }

    @Configuration(proxyBeanMethods = false)
    static class DuplicateRegistrationConfiguration {

        @Bean
        DevServiceRegistration anotherPostgresqlRegistration() {
            return registration("postgresql", "Another PostgreSQL Database", "postgres:17", "9999");
        }

    }

    private static DevServiceRegistration registration(String name, String description, String imageName, String containerId) {
        Supplier<ContainerInfo> containerInfo = () -> new ContainerInfo(containerId, imageName, List.of("/" + imageName.split(":")[0]),
                List.of(new ContainerInfo.ContainerPort("0.0.0.0", 5432, 5432, "tcp")), Map.of("test", "label"), "running", "Up 5 minutes");
        return new DevServiceRegistration(name, description, DevServiceRegistration.Origin.MANAGED, containerInfo, List.of());
    }

}
