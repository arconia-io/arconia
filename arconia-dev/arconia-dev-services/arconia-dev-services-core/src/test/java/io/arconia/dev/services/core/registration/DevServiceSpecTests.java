package io.arconia.dev.services.core.registration;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.core.TestDevServicesProperties;
import io.arconia.dev.services.core.registration.DevServiceSpec.LinkDefinition;
import io.arconia.dev.services.core.registration.DevServicesTestSupport.TestConnectionDetails;
import io.arconia.dev.services.core.registration.DevServicesTestSupport.TestPostgresContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Unit tests for {@link DevServiceSpec}: the validation of a dev service specification and
 * the links it declares.
 */
class DevServiceSpecTests {

    @Test
    void whenSpecIsCompleteThenValid() {
        assertThatNoException().isThrownBy(completeSpec()::validate);
    }

    @Test
    void whenNameIsNullThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(completeSpec().name(null)::validate)
                .withMessageContaining("service name cannot be null or empty");
    }

    @Test
    void whenNameIsEmptyThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(completeSpec().name("")::validate)
                .withMessageContaining("service name cannot be null or empty");
    }

    @Test
    void whenNameIsInvalidThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(completeSpec().name("Fancy_DB")::validate)
                .withMessageContaining("service name must contain only lowercase letters, digits, and dashes");
    }

    @Test
    void whenDescriptionIsMissingThenThrow() {
        DevServiceSpec spec = new DevServiceSpec()
                .name("postgres")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(TestPostgresContainer.class, TestPostgresContainer::new);

        assertThatIllegalArgumentException()
                .isThrownBy(spec::validate)
                .withMessageContaining("service description cannot be null or empty");
    }

    @Test
    void whenPropertiesAreMissingThenThrow() {
        DevServiceSpec spec = new DevServiceSpec()
                .name("postgres")
                .description("Test Dev Service")
                .container(TestPostgresContainer.class, TestPostgresContainer::new);

        assertThatIllegalArgumentException()
                .isThrownBy(spec::validate)
                .withMessageContaining("service properties cannot be null");
    }

    @Test
    void whenContainerIsMissingThenThrow() {
        DevServiceSpec spec = new DevServiceSpec()
                .name("postgres")
                .description("Test Dev Service")
                .properties(TestDevServicesProperties.DEFAULT);

        assertThatIllegalArgumentException()
                .isThrownBy(spec::validate)
                .withMessageContaining("service container type cannot be null");
    }

    @Test
    void whenContainerTypeIsNullThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(completeSpec().container(null, TestPostgresContainer::new)::validate)
                .withMessageContaining("service container type cannot be null");
    }

    @Test
    void whenContainerSupplierIsNullThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(completeSpec().container(TestPostgresContainer.class, null)::validate)
                .withMessageContaining("service container supplier cannot be null");
    }

    @Test
    void whenCategoryIsEmptyThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> completeSpec().category(""))
                .withMessageContaining("category cannot be null or empty");
    }

    @Test
    void whenServiceConnectionNameIsEmptyThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> completeSpec().serviceConnectionName(""))
                .withMessageContaining("serviceConnectionName cannot be null or empty");
    }

    @Test
    void whenServiceConnectionIsDisabledWithANameThenThrow() {
        DevServiceSpec spec = completeSpec()
                .serviceConnection(false)
                .serviceConnectionName("postgres");

        assertThatIllegalStateException()
                .isThrownBy(spec::validate)
                .withMessageContaining("service connection name has no effect");
    }

    @Test
    void whenDiscoveryIsDeclaredThenItIsSupported() {
        DevServiceSpec spec = completeSpec()
                .discovery(TestConnectionDetails.class, container -> new TestConnectionDetails(container.host()));

        assertThat(spec.supportsDiscovery()).isTrue();
        assertThat(completeSpec().supportsDiscovery()).isFalse();
    }

    @Test
    void whenDiscoveryIsDeclaredTwiceThenThrow() {
        DevServiceSpec spec = completeSpec()
                .discovery(TestConnectionDetails.class, container -> new TestConnectionDetails(container.host()));

        assertThatIllegalStateException()
                .isThrownBy(() -> spec.discovery(TestConnectionDetails.class, container -> new TestConnectionDetails(container.host())))
                .withMessageContaining("discovery can be declared only once");
    }

    @Test
    void whenLinksDeclaredThenKeptInOrder() {
        DevServiceSpec spec = completeSpec()
                .link("UI", 8080)
                .link("Docs", 8080, "/docs");

        assertThat(spec.getLinks()).containsExactly(
                new LinkDefinition("UI", 8080, ""),
                new LinkDefinition("Docs", 8080, "/docs"));
    }

    @Test
    void whenLinkPathIsInvalidThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> completeSpec().link("UI", 8080, "docs"))
                .withMessageContaining("path must be empty or start with '/'");
    }

    @Test
    void whenLinkPortIsInvalidThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> completeSpec().link("UI", 0))
                .withMessageContaining("port must be between 1 and 65535");
    }

    @Test
    void whenLinkResolvedThenUrlPointsAtTheMappedPort() {
        LinkDefinition link = new LinkDefinition("Docs", 8080, "/docs");

        assertThat(link.resolve("localhost", 1234)).isEqualTo(new DevServiceLink("Docs", "http://localhost:1234/docs"));
    }

    private static DevServiceSpec completeSpec() {
        return new DevServiceSpec()
                .name("postgres")
                .description("Test Dev Service")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(TestPostgresContainer.class, TestPostgresContainer::new);
    }

}
