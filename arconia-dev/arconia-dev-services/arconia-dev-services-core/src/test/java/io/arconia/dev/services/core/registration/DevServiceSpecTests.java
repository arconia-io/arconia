package io.arconia.dev.services.core.registration;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NullUnmarked;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.ssl.SslBundle;

import io.arconia.dev.services.api.config.ResourceMapping;
import io.arconia.dev.services.api.config.ReuseStrategy;
import io.arconia.dev.services.api.config.VolumeMapping;
import io.arconia.dev.services.core.TestDevServicesProperties;
import io.arconia.dev.services.core.TestSslDevServicesProperties;
import io.arconia.dev.services.core.registration.DevServiceDefinition.LinkDefinition;
import io.arconia.dev.services.core.registration.DevServiceDefinition.PortBinding;
import io.arconia.dev.services.core.registration.DevServicesTestSupport.TestConnectionDetails;
import io.arconia.dev.services.core.registration.DevServicesTestSupport.TestPostgresContainer;
import io.arconia.dev.services.core.ssl.DevServiceSslConfigurer;
import io.arconia.dev.services.core.ssl.SslKeyStoreRequirement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link DevServiceSpec}.
 */
class DevServiceSpecTests {

    @Nested
    class Build {

        @Test
        void whenSpecIsCompleteThenDefinitionCarriesItsValues() {
            DevServiceDefinition definition = completeSpec()
                    .category("jdbc")
                    .serviceConnectionName("postgres")
                    .build();

            assertThat(definition.name()).isEqualTo("postgres");
            assertThat(definition.description()).isEqualTo("Test Dev Service");
            assertThat(definition.category()).isEqualTo("jdbc");
            assertThat(definition.properties()).isSameAs(TestDevServicesProperties.DEFAULT);
            assertThat(definition.containerType()).isEqualTo(TestPostgresContainer.class);
            assertThat(definition.containerSupplier().get()).isInstanceOf(TestPostgresContainer.class);
            assertThat(definition.serviceConnection()).isTrue();
            assertThat(definition.serviceConnectionName()).isEqualTo("postgres");
            assertThat(definition.discovery()).isNull();
            assertThat(definition.links()).isEmpty();
        }

        @Test
        void whenOptionalValuesAreNotSetThenDefinitionHasDefaults() {
            DevServiceDefinition definition = completeSpec().build();

            assertThat(definition.category()).isNull();
            assertThat(definition.serviceConnection()).isTrue();
            assertThat(definition.serviceConnectionName()).isNull();
        }

        @Test
        void whenNameIsNullThenThrow() {
            assertThatIllegalArgumentException()
                    .isThrownBy(completeSpec().name(null)::build)
                    .withMessageContaining("Dev Service name cannot be null or empty");
        }

        @Test
        void whenNameIsEmptyThenThrow() {
            assertThatIllegalArgumentException()
                    .isThrownBy(completeSpec().name("")::build)
                    .withMessageContaining("Dev Service name cannot be null or empty");
        }

        @Test
        void whenNameIsInvalidThenThrow() {
            assertThatIllegalArgumentException()
                    .isThrownBy(completeSpec().name("Fancy_DB")::build)
                    .withMessageContaining("Dev Service name must contain only lowercase letters, digits, and dashes: Fancy_DB");
        }

        @Test
        void whenDescriptionIsMissingThenThrowNamingTheService() {
            DevServiceSpec spec = new DevServiceSpec()
                    .name("postgres")
                    .properties(TestDevServicesProperties.DEFAULT)
                    .container(TestPostgresContainer.class, TestPostgresContainer::new);

            assertThatIllegalArgumentException()
                    .isThrownBy(spec::build)
                    .withMessageContaining("Dev Service 'postgres': description cannot be null or empty");
        }

        @Test
        void whenPropertiesAreMissingThenThrow() {
            DevServiceSpec spec = new DevServiceSpec()
                    .name("postgres")
                    .description("Test Dev Service")
                    .container(TestPostgresContainer.class, TestPostgresContainer::new);

            assertThatIllegalArgumentException()
                    .isThrownBy(spec::build)
                    .withMessageContaining("Dev Service 'postgres': properties cannot be null");
        }

        @Test
        void whenContainerIsMissingThenThrow() {
            DevServiceSpec spec = new DevServiceSpec()
                    .name("postgres")
                    .description("Test Dev Service")
                    .properties(TestDevServicesProperties.DEFAULT);

            assertThatIllegalArgumentException()
                    .isThrownBy(spec::build)
                    .withMessageContaining("Dev Service 'postgres': container type cannot be null");
        }

        @Test
        void whenContainerTypeIsNullThenThrow() {
            assertThatIllegalArgumentException()
                    .isThrownBy(completeSpec().container(null, TestPostgresContainer::new)::build)
                    .withMessageContaining("container type cannot be null");
        }

        @Test
        void whenContainerSupplierIsNullThenThrow() {
            assertThatIllegalArgumentException()
                    .isThrownBy(completeSpec().container(TestPostgresContainer.class, null)::build)
                    .withMessageContaining("container supplier cannot be null");
        }

        @Test
        void whenCategoryIsEmptyThenThrow() {
            assertThatIllegalArgumentException()
                    .isThrownBy(completeSpec().category("")::build)
                    .withMessageContaining("Dev Service 'postgres': category cannot be empty");
        }

    }

    @Nested
    class ServiceConnection {

        @Test
        void whenServiceConnectionIsDisabledThenDefinitionSaysSo() {
            DevServiceDefinition definition = completeSpec().serviceConnection(false).build();

            assertThat(definition.serviceConnection()).isFalse();
        }

        @Test
        void whenServiceConnectionNameIsEmptyThenThrow() {
            assertThatIllegalArgumentException()
                    .isThrownBy(completeSpec().serviceConnectionName("")::build)
                    .withMessageContaining("Dev Service 'postgres': service connection name cannot be empty");
        }

        @Test
        void whenServiceConnectionIsDisabledWithANameThenThrow() {
            DevServiceSpec spec = completeSpec()
                    .serviceConnection(false)
                    .serviceConnectionName("postgres");

            assertThatIllegalStateException()
                    .isThrownBy(spec::build)
                    .withMessageContaining("Dev Service 'postgres': a service connection name has no effect");
        }

    }

    @Nested
    class Discovery {

        @Test
        void whenDiscoveryIsDeclaredThenDefinitionCarriesIt() {
            DevServiceDefinition definition = completeSpec()
                    .discovery(TestConnectionDetails.class, container -> new TestConnectionDetails(container.host()))
                    .build();

            assertThat(definition.discovery()).isNotNull();
            assertThat(definition.discovery().type()).isEqualTo(TestConnectionDetails.class);
            assertThat(definition.discovery().factory().apply(DevServicesTestSupport.discoveredContainer()))
                    .isEqualTo(new TestConnectionDetails("localhost"));
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
        void whenDiscoveryTypeIsNullThenThrow() {
            DevServiceSpec spec = completeSpec()
                    .discovery(null, container -> new TestConnectionDetails(container.host()));

            // Declaring a null type is an error, not a way to opt out of discovery.
            assertThatIllegalArgumentException()
                    .isThrownBy(spec::build)
                    .withMessageContaining("Dev Service 'postgres': discovery connection details type cannot be null");
        }

        @Test
        void whenDiscoveryFactoryIsNullThenThrow() {
            DevServiceSpec spec = completeSpec().discovery(TestConnectionDetails.class, null);

            assertThatIllegalArgumentException()
                    .isThrownBy(spec::build)
                    .withMessageContaining("Dev Service 'postgres': discovery connection details factory cannot be null");
        }

    }

    @Nested
    class Properties {

        @ParameterizedTest
        @ValueSource(strings = {"image-name", "environment", "network-aliases", "resources", "reuse-strategy", "startup-timeout", "volumes"})
        void whenAPropertyIsNullThenThrowNamingTheProperty(String property) {
            DevServiceSpec spec = completeSpec().properties(new NullReturningProperties(property));

            // A hand-written properties class may return null where the contract says it never does.
            assertThatIllegalArgumentException()
                    .isThrownBy(spec::build)
                    .withMessageContaining("Dev Service 'postgres': the '%s' property cannot be null".formatted(property));
        }

        @Test
        void whenStartupTimeoutIsNotPositiveThenThrow() {
            DevServiceSpec spec = completeSpec().properties(TestDevServicesProperties.DEFAULT.withStartupTimeout(Duration.ZERO));

            assertThatIllegalArgumentException()
                    .isThrownBy(spec::build)
                    .withMessageContaining("Dev Service 'postgres': the 'startup-timeout' property must be positive, but was: PT0S");
        }

    }

    @Nested
    class Ssl {

        @Test
        void whenSslSupportIsDeclaredThenDefinitionCarriesIt() {
            DevServiceSslConfigurer<TestPostgresContainer> configurer = (container, bundle) -> {};

            DevServiceDefinition definition = completeSpec()
                    .properties(TestSslDevServicesProperties.pem())
                    .ssl(configurer)
                    .build();

            assertThat(definition.sslSupport()).isNotNull();
            assertThat(definition.sslSupport().configurer()).isSameAs(configurer);
            assertThat(definition.sslSupport().keyStoreRequirement()).isEqualTo(SslKeyStoreRequirement.REQUIRED);
            assertThat(definition.isSslEnabled()).isTrue();
        }

        @Test
        void whenKeyStoreIsOptionalThenDefinitionSaysSo() {
            DevServiceDefinition definition = completeSpec()
                    .properties(TestSslDevServicesProperties.enabledWithoutKeyStore())
                    .ssl((TestPostgresContainer container, SslBundle bundle) -> {}, SslKeyStoreRequirement.OPTIONAL)
                    .build();

            assertThat(definition.sslSupport().keyStoreRequirement()).isEqualTo(SslKeyStoreRequirement.OPTIONAL);
            assertThat(definition.isSslEnabled()).isTrue();
        }

        @Test
        void whenNoSslSupportIsDeclaredThenDefinitionHasNone() {
            DevServiceDefinition definition = completeSpec().build();

            assertThat(definition.sslSupport()).isNull();
            assertThat(definition.isSslEnabled()).isFalse();
        }

        @Test
        void whenSslIsDisabledThenDefinitionIsNotSecured() {
            DevServiceDefinition definition = completeSpec()
                    .properties(new TestSslDevServicesProperties())
                    .ssl((TestPostgresContainer container, SslBundle bundle) -> {})
                    .build();

            assertThat(definition.isSslEnabled()).isFalse();
        }

        @Test
        void whenConfigurerOrKeyStoreRequirementIsNullThenThrow() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> completeSpec().ssl(null))
                    .withMessageContaining("configurer cannot be null");
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> completeSpec().ssl((TestPostgresContainer container, SslBundle bundle) -> {}, null))
                    .withMessageContaining("keyStoreRequirement cannot be null");
        }

        @Test
        void whenSslSupportIsDeclaredWithoutServiceConnectionThenThrow() {
            // The application is connected over SSL through the service connection only.
            DevServiceSpec spec = completeSpec()
                    .properties(new TestSslDevServicesProperties())
                    .ssl((TestPostgresContainer container, SslBundle bundle) -> {})
                    .serviceConnection(false);

            assertThatIllegalStateException()
                    .isThrownBy(spec::build)
                    .withMessageContaining("Dev Service 'postgres': SSL support requires the service connection");
        }

        @Test
        void whenSslPropertiesAreInvalidThenBuildThrowsNamingTheService() {
            DevServiceSpec spec = completeSpec().properties(TestSslDevServicesProperties.pem());

            assertThatIllegalArgumentException()
                    .isThrownBy(spec::build)
                    .withMessageContaining("Dev Service 'postgres': the 'ssl.enabled' property is true, but the dev service declares no SSL support");
        }

    }

    @Nested
    class Port {

        @Test
        void whenPortIsDeclaredThenDefinitionCarriesIt() {
            assertThat(completeSpec().port(5432).build().port()).isEqualTo(5432);
            assertThat(completeSpec().build().port()).isNull();
        }

        @Test
        void whenPortIsOutOfRangeThenThrow() {
            assertThatIllegalArgumentException()
                    .isThrownBy(completeSpec().port(0)::build)
                    .withMessageContaining("Dev Service 'postgres': container port must be between 1 and 65535, but was: 0");
            assertThatIllegalArgumentException()
                    .isThrownBy(completeSpec().port(65536)::build)
                    .withMessageContaining("container port must be between 1 and 65535, but was: 65536");
        }

        @Test
        void whenFixedPortIsConfiguredWithADeclaredPortThenValid() {
            DevServiceDefinition definition = completeSpec()
                    .properties(TestDevServicesProperties.DEFAULT.withPort(1234))
                    .port(5432)
                    .build();

            assertThat(definition.properties().getPort()).isEqualTo(1234);
        }

        @Test
        void whenFixedPortIsConfiguredWithoutADeclaredPortThenThrow() {
            DevServiceSpec spec = completeSpec().properties(TestDevServicesProperties.DEFAULT.withPort(1234));

            // The property would otherwise be silently ignored.
            assertThatIllegalStateException()
                    .isThrownBy(spec::build)
                    .withMessageContaining("Dev Service 'postgres': the 'port' property is set to 1234, but the dev service declares no container port");
        }

        @Test
        void whenFixedPortIsOutOfRangeThenThrow() {
            DevServiceSpec spec = completeSpec().properties(TestDevServicesProperties.DEFAULT.withPort(70000)).port(5432);

            assertThatIllegalArgumentException()
                    .isThrownBy(spec::build)
                    .withMessageContaining("Dev Service 'postgres': the 'port' property must be between 0 and 65535, but was: 70000");
        }

        @Test
        void whenAdditionalPortsDeclaredThenKeptInOrder() {
            DevServiceDefinition definition = completeSpec()
                    .port(5432)
                    .additionalPort(8080, 1234)
                    .additionalPort(9090, 0)
                    .build();

            assertThat(definition.additionalPorts()).containsExactly(
                    new PortBinding(8080, 1234),
                    new PortBinding(9090, 0));
        }

        @Test
        void whenAdditionalPortIsOutOfRangeThenThrow() {
            assertThatIllegalArgumentException()
                    .isThrownBy(completeSpec().additionalPort(0, 1234)::build)
                    .withMessageContaining("Dev Service 'postgres': additional container port must be between 1 and 65535, but was: 0");
            assertThatIllegalArgumentException()
                    .isThrownBy(completeSpec().additionalPort(8080, 70000)::build)
                    .withMessageContaining("Dev Service 'postgres': the host port for container port 8080 must be between 0 and 65535, but was: 70000");
        }

        @Test
        void whenContainerPortIsDeclaredTwiceThenThrow() {
            assertThatIllegalArgumentException()
                    .isThrownBy(completeSpec().port(5432).additionalPort(5432, 1234)::build)
                    .withMessageContaining("Dev Service 'postgres': container port 5432 is declared more than once");
            assertThatIllegalArgumentException()
                    .isThrownBy(completeSpec().additionalPort(8080, 1234).additionalPort(8080, 5678)::build)
                    .withMessageContaining("container port 8080 is declared more than once");
        }

    }

    @Nested
    class Links {

        @Test
        void whenLinksDeclaredThenKeptInOrder() {
            DevServiceDefinition definition = completeSpec()
                    .link("UI", 8080)
                    .link("Docs", 8080, "/docs")
                    .build();

            assertThat(definition.links()).containsExactly(
                    new LinkDefinition("UI", 8080, ""),
                    new LinkDefinition("Docs", 8080, "/docs"));
        }

        @Test
        void whenLinkLabelIsEmptyThenThrow() {
            assertThatIllegalArgumentException()
                    .isThrownBy(completeSpec().link("", 8080)::build)
                    .withMessageContaining("Dev Service 'postgres': link label cannot be null or empty");
        }

        @Test
        void whenLinkPathIsInvalidThenThrow() {
            assertThatIllegalArgumentException()
                    .isThrownBy(completeSpec().link("UI", 8080, "docs")::build)
                    .withMessageContaining("Dev Service 'postgres': link path must be empty or start with '/': docs");
        }

        @Test
        void whenLinkPortIsInvalidThenThrow() {
            assertThatIllegalArgumentException()
                    .isThrownBy(completeSpec().link("UI", 0)::build)
                    .withMessageContaining("Dev Service 'postgres': link port must be between 1 and 65535, but was: 0");
        }

    }

    private static DevServiceSpec completeSpec() {
        return new DevServiceSpec()
                .name("postgres")
                .description("Test Dev Service")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(TestPostgresContainer.class, TestPostgresContainer::new);
    }

    /**
     * Properties returning null for the one property named at construction, as a hand-written
     * properties class not honoring the contract would.
     */
    @NullUnmarked
    private static final class NullReturningProperties extends TestDevServicesProperties {

        private final String nullProperty;

        NullReturningProperties(String nullProperty) {
            this.nullProperty = nullProperty;
        }

        @Override
        public String getImageName() {
            return "image-name".equals(nullProperty) ? null : super.getImageName();
        }

        @Override
        public Map<String, String> getEnvironment() {
            return "environment".equals(nullProperty) ? null : super.getEnvironment();
        }

        @Override
        public List<String> getNetworkAliases() {
            return "network-aliases".equals(nullProperty) ? null : super.getNetworkAliases();
        }

        @Override
        public List<ResourceMapping> getResources() {
            return "resources".equals(nullProperty) ? null : super.getResources();
        }

        @Override
        public ReuseStrategy getReuseStrategy() {
            return "reuse-strategy".equals(nullProperty) ? null : super.getReuseStrategy();
        }

        @Override
        public Duration getStartupTimeout() {
            return "startup-timeout".equals(nullProperty) ? null : super.getStartupTimeout();
        }

        @Override
        public List<VolumeMapping> getVolumes() {
            return "volumes".equals(nullProperty) ? null : super.getVolumes();
        }

    }

}
