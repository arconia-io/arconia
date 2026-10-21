package io.arconia.dev.services.core.actuate.endpoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ssl.PemSslBundleProperties;
import org.springframework.boot.autoconfigure.ssl.PropertiesSslBundle;
import org.springframework.boot.info.SslInfo;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.boot.ssl.SslBundle;

import io.arconia.dev.services.api.registration.ContainerInfo;
import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.core.TestSslDevServicesProperties;
import io.arconia.dev.services.core.actuate.endpoint.DevServicesEndpoint.ServiceInfo;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevServicesEndpoint}.
 */
class DevServicesEndpointTests {

    private final Map<String, DevServiceRegistration> registrations = new LinkedHashMap<>();

    @Test
    void devServicesReturnsEmptyMapWhenNoRegistrations() {
        Map<String, ServiceInfo> result = endpoint().devServices();

        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    void endpointDefensivelyCopiesRegistrationsAtConstruction() {
        DevServicesEndpoint endpoint = endpoint();
        registrations.put("postgres", registration("postgres", "PostgreSQL Database", () -> containerInfo("container-1", "postgres:18")));

        assertThat(endpoint.devServices()).isEmpty();
    }

    @Test
    void devServicesReturnsMultipleServicesInRegistrationOrder() {
        registrations.put("postgres", registration("postgres", "PostgreSQL Database", () -> containerInfo("container-1", "postgres:18")));
        registrations.put("docling", registration("docling", "Docling Serve", () -> containerInfo("container-2", "docling:1.10")));
        registrations.put("artemis", registration("artemis", "Artemis Broker", () -> containerInfo("container-3", "artemis:2.40")));

        Map<String, ServiceInfo> result = endpoint().devServices();

        assertThat(result.keySet()).containsExactly("postgres", "docling", "artemis");
        assertThat(result.get("postgres").description()).isEqualTo("PostgreSQL Database");
        assertThat(result.get("postgres").containerInfo().getImageName()).isEqualTo("postgres:18");
        assertThat(result.get("docling").description()).isEqualTo("Docling Serve");
        assertThat(result.get("docling").containerInfo().getImageName()).isEqualTo("docling:1.10");
    }

    @Test
    void devServiceReturnsServiceInfoWhenServiceExists() {
        registrations.put("postgres", registration("postgres", "PostgreSQL Database", () -> containerInfo("container-1", "postgres:18")));

        ServiceInfo result = endpoint().devService("postgres");

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("postgres");
        assertThat(result.description()).isEqualTo("PostgreSQL Database");
        assertThat(result.origin()).isEqualTo(DevServiceRegistration.Origin.MANAGED);
        assertThat(result.containerInfo()).isNotNull();
        assertThat(result.containerInfo().getId()).isEqualTo("container-1");
        assertThat(result.containerInfo().getImageName()).isEqualTo("postgres:18");
        assertThat(result.containerInfo().getState()).isEqualTo("running");
        assertThat(result.containerInfo().getStatus()).isEqualTo("Up 5 minutes");
        assertThat(result.containerInfo().getNames()).containsExactly("postgres-container");
        assertThat(result.containerInfo().getLabels()).containsEntry("app", "test");
        assertThat(result.containerInfo().getPorts()).hasSize(1);
    }

    @Test
    void devServiceReturnsNullWhenServiceNotFound() {
        registrations.put("postgres", registration("postgres", "PostgreSQL Database", () -> containerInfo("container-1", "postgres:18")));

        assertThat(endpoint().devService("docling")).isNull();
    }

    @Test
    void containerInfoIsNullWhenContainerCannotBeInspected() {
        registrations.put("postgres", registration("postgres", "PostgreSQL Database", () -> {
            throw new IllegalStateException("Container not found");
        }));

        assertThat(endpoint().devServices().get("postgres").containerInfo()).isNull();
        assertThat(endpoint().devService("postgres").containerInfo()).isNull();
    }

    @Test
    void sslIsReportedFromTheRegisteredBundleOfTheService() {
        registrations.put("redis", registration("redis", "Redis Dev Service", () -> containerInfo("container-1", "redis:8"), "dev-services-redis"));
        var sslBundles = new DefaultSslBundleRegistry();
        sslBundles.registerBundle("dev-services-redis", sslBundle());

        ServiceInfo result = new DevServicesEndpoint(registrations, sslBundles).devService("redis");

        assertThat(result.ssl()).isNotNull();
        assertThat(result.ssl().getName()).isEqualTo("dev-services-redis");
        // The application presents no certificate: the one of the dev service is what it trusts.
        assertThat(result.ssl().getCertificateChains()).isEmpty();
        assertThat(result.ssl().getTrustStoreCertificateChains()).singleElement().satisfies(chain ->
                assertThat(chain.getCertificates()).singleElement().satisfies(certificate -> {
                    assertThat(certificate.getSubject()).contains("CN=localhost");
                    assertThat(certificate.getValidity().getStatus()).isEqualTo(SslInfo.CertificateValidityInfo.Status.VALID);
                }));
    }

    @Test
    void sslIsNullWhenTheServiceHasNoBundle() {
        registrations.put("postgres", registration("postgres", "PostgreSQL Database", () -> containerInfo("container-1", "postgres:18")));

        assertThat(new DevServicesEndpoint(registrations, new DefaultSslBundleRegistry()).devService("postgres").ssl()).isNull();
    }

    @Test
    void sslIsNullWhenTheBundlesAreNotAvailableOrTheBundleIsNotRegistered() {
        registrations.put("redis", registration("redis", "Redis Dev Service", () -> containerInfo("container-1", "redis:8"), "dev-services-redis"));

        // The application defines its own SslBundles, to which the dev service bundle is not added.
        assertThat(endpoint().devService("redis").ssl()).isNull();
        assertThat(new DevServicesEndpoint(registrations, new DefaultSslBundleRegistry()).devService("redis").ssl()).isNull();
    }

    private DevServicesEndpoint endpoint() {
        return new DevServicesEndpoint(registrations, null);
    }

    private static DevServiceRegistration registration(String name, String description, Supplier<ContainerInfo> containerInfo) {
        return registration(name, description, containerInfo, null);
    }

    private static DevServiceRegistration registration(String name, String description, Supplier<ContainerInfo> containerInfo, String sslBundle) {
        return DevServiceRegistration.builder()
                .name(name)
                .description(description)
                .origin(DevServiceRegistration.Origin.MANAGED)
                .containerInfo(containerInfo)
                .sslBundleName(sslBundle)
                .build();
    }

    /**
     * The bundle registered for a dev service: it trusts the certificate the dev service presents.
     */
    private static SslBundle sslBundle() {
        PemSslBundleProperties properties = new PemSslBundleProperties();
        properties.getTruststore().setCertificate(TestSslDevServicesProperties.CERTIFICATE);
        return PropertiesSslBundle.get(properties);
    }

    private static ContainerInfo containerInfo(String id, String imageName) {
        return ContainerInfo.builder()
                .id(id)
                .imageName(imageName)
                .names(List.of("postgres-container"))
                .ports(List.of(new ContainerInfo.ContainerPort("0.0.0.0", 5432, 5432, "tcp")))
                .labels(Map.of("app", "test"))
                .state("running")
                .status("Up 5 minutes")
                .build();
    }

}
