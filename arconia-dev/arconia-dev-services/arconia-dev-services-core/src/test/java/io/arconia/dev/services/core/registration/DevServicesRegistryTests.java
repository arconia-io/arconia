package io.arconia.dev.services.core.registration;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.container.ContainerImageMetadata;
import org.springframework.boot.autoconfigure.ssl.SslBundleRegistrar;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.PemTrustStore;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.support.SimpleThreadScope;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.testcontainers.containers.GenericContainer;

import io.arconia.dev.services.api.config.BaseDevServicesProperties;
import io.arconia.dev.services.api.config.ReuseStrategy;
import io.arconia.dev.services.api.registration.ContainerInfo;
import io.arconia.dev.services.api.registration.DevServiceLabels;
import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.core.TestDevServicesProperties;
import io.arconia.dev.services.core.TestSslDevServicesProperties;
import io.arconia.dev.services.core.container.DevServiceContainerCustomizer;
import io.arconia.dev.services.core.ssl.DevServiceSslConfigurer;
import io.arconia.dev.services.core.ssl.SslKeyStoreRequirement;
import io.arconia.dev.services.core.registration.DevServicesRegistry.BeanAttribute;
import io.arconia.dev.services.core.registration.DevServicesRegistry.BeanName;
import io.arconia.dev.services.core.registration.DevServicesTestSupport.OtherConnectionDetails;
import io.arconia.dev.services.core.registration.DevServicesTestSupport.RestartingContainer;
import io.arconia.dev.services.core.registration.DevServicesTestSupport.TestConnectionDetails;
import io.arconia.dev.services.core.registration.DevServicesTestSupport.TestLinkContainer;
import io.arconia.dev.services.core.registration.DevServicesTestSupport.TestUnstartedLinkContainer;
import io.arconia.dev.services.core.registration.DevServicesTestSupport.TestNetwork;
import io.arconia.dev.services.core.registration.DevServicesTestSupport.TestPostgresContainer;

import static io.arconia.dev.services.core.registration.DevServicesTestSupport.discoveredContainer;
import static io.arconia.dev.services.core.registration.DevServicesTestSupport.enableDevMode;
import static io.arconia.dev.services.core.registration.DevServicesTestSupport.enableTestMode;
import static io.arconia.dev.services.core.registration.DevServicesTestSupport.registerNetworkBean;
import static io.arconia.dev.services.core.registration.DevServicesTestSupport.registerPostgresDevService;
import static io.arconia.dev.services.core.registration.DevServicesTestSupport.registerSharedDevService;
import static io.arconia.dev.services.core.registration.DevServicesTestSupport.registryDiscovering;
import static io.arconia.dev.services.core.registration.DevServicesTestSupport.registryWithNetworkEnabled;
import static io.arconia.dev.services.core.registration.DevServicesTestSupport.registryWithReuseSupport;
import static io.arconia.dev.services.core.registration.DevServicesTestSupport.resetBootstrapMode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link DevServicesRegistry}.
 */
@ExtendWith(OutputCaptureExtension.class)
class DevServicesRegistryTests {

    private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

    private final DevServicesRegistry registry = new DevServicesRegistry(beanFactory, new StandardEnvironment(), new DefaultResourceLoader());

    @BeforeEach
    @AfterEach
    void reset() {
        resetBootstrapMode();
    }

    @Nested
    class Registration {

        @Test
        void whenSpecIsInvalidThenNothingIsRegistered() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> registry.registerDevService(service -> service
                            .name("postgres")
                            .properties(TestDevServicesProperties.DEFAULT)
                            .container(TestPostgresContainer.class, TestPostgresContainer::new)))
                    .withMessageContaining("Dev Service 'postgres': description cannot be null or empty");
            assertThat(beanFactory.getBeanDefinitionCount()).isZero();
        }

        @Test
        void whenServiceRegisteredThenContainerAndRegistrationBeansAreDefined() {
            registerPostgresDevService(registry);

            // The bean names are part of the contract: they show up in the bean graph of the application.
            assertThat(beanFactory.getBeanDefinitionNames())
                    .containsExactlyInAnyOrder("devService.container.postgres", "devService.registration.postgres");

            BeanDefinition container = beanFactory.getBeanDefinition("devService.container.postgres");
            assertThat(container.getBeanClassName()).isEqualTo(TestPostgresContainer.class.getName());
            assertThat(container.getDescription()).isEqualTo("Test Dev Service");
            assertThat(container.getRole()).isEqualTo(BeanDefinition.ROLE_INFRASTRUCTURE);
            assertThat(BeanAttribute.AOT_EXCLUDED.isPresent(container)).isTrue();

            BeanDefinition registration = beanFactory.getBeanDefinition("devService.registration.postgres");
            assertThat(registration.getBeanClassName()).isEqualTo(DevServiceRegistration.class.getName());
            assertThat(registration.getRole()).isEqualTo(BeanDefinition.ROLE_SUPPORT);
            assertThat(registration.getDependsOn()).containsExactly("devService.container.postgres");
            assertThat(BeanAttribute.AOT_EXCLUDED.isPresent(registration)).isTrue();
        }

        @Test
        void whenServiceRegisteredTwiceThenThrow() {
            registerPostgresDevService(registry);

            // A second registration under the same name is never silently discarded, whatever it declares.
            assertThatIllegalStateException()
                    .isThrownBy(() -> registerPostgresDevService(registry))
                    .withMessageContaining("A dev service named 'postgres' is already registered");
            assertThat(beanFactory.getBeanNamesForType(TestPostgresContainer.class)).hasSize(1);
        }

        @Test
        void whenServiceConnectionIsEnabledThenContainerHasServiceConnectionAnnotation() {
            registry.registerDevService(service -> service
                    .name("postgres")
                    .description("Test Dev Service")
                    .properties(TestDevServicesProperties.DEFAULT)
                    .container(TestPostgresContainer.class, TestPostgresContainer::new)
                    .serviceConnectionName("postgres"));

            var beanDefinition = (DevServiceContainerBeanDefinition) beanFactory.getBeanDefinition(BeanName.CONTAINER.of("postgres"));

            assertThat(beanDefinition.getAnnotations().get(ServiceConnection.class).getString("name")).isEqualTo("postgres");
        }

        @Test
        void whenServiceConnectionHasNoNameThenContainerHasUnnamedServiceConnectionAnnotation() {
            registerPostgresDevService(registry);

            var beanDefinition = (DevServiceContainerBeanDefinition) beanFactory.getBeanDefinition(BeanName.CONTAINER.of("postgres"));

            // Spring Boot determines the connection details from the container type.
            assertThat(beanDefinition.getAnnotations().get(ServiceConnection.class).getString("name")).isEmpty();
        }

        @Test
        void whenServiceConnectionIsDisabledThenContainerHasNoServiceConnectionAnnotation() {
            registry.registerDevService(service -> service
                    .name("postgres")
                    .description("Test Dev Service")
                    .properties(TestDevServicesProperties.DEFAULT)
                    .container(TestPostgresContainer.class, TestPostgresContainer::new)
                    .serviceConnection(false));

            var beanDefinition = (DevServiceContainerBeanDefinition) beanFactory.getBeanDefinition(BeanName.CONTAINER.of("postgres"));

            assertThat(beanDefinition.getAnnotations().isPresent(ServiceConnection.class)).isFalse();
        }

        @Test
        void whenSameCategoryRegisteredTwiceThenThrowBeforeAnyContainerIsCreated() {
            AtomicBoolean containerCreated = new AtomicBoolean(false);
            Supplier<TestPostgresContainer> containerSupplier = () -> {
                containerCreated.set(true);
                return new TestPostgresContainer();
            };
            registerCategorizedDevService("first", "jdbc", containerSupplier);

            assertThatExceptionOfType(MultipleDevServicesException.class)
                    .isThrownBy(() -> registerCategorizedDevService("second", "jdbc", containerSupplier))
                    .withMessageContaining("jdbc")
                    .satisfies(ex -> assertThat(ex.getServiceNames()).containsExactly("first", "second"));
            // Mutual exclusion is checked at registration time, with no container instantiated.
            assertThat(containerCreated).isFalse();
            assertThat(beanFactory.containsBeanDefinition(BeanName.REGISTRATION.of("second"))).isFalse();
        }

        @Test
        void whenDifferentCategoriesOrNoCategoryThenAllAreRegistered() {
            registerCategorizedDevService("first", "jdbc", TestPostgresContainer::new);
            registerCategorizedDevService("other", "opentelemetry", TestPostgresContainer::new);
            registerCategorizedDevService("uncategorized", null, TestPostgresContainer::new);

            assertThat(beanFactory.getBeanNamesForType(DevServiceRegistration.class)).hasSize(3);
        }

        @Test
        void whenApplicationDefinesConnectionDetailsThenManagedDevServiceIsNotRegistered(CapturedOutput output) {
            beanFactory.registerSingleton("userConnectionDetails", new TestConnectionDetails("user-defined"));

            // Outside dev mode no discovery happens, so the container would have been started by the application.
            registerSharedDevService(registry);

            // The application connects to the service of its choice: the dev service is neither
            // started nor registered.
            assertThat(beanFactory.getBeanDefinitionNames()).noneMatch(name -> name.startsWith(BeanName.PREFIX));
            assertThat(output).contains("Dev Service 'postgres' is not started: the application defines its own TestConnectionDetails");
        }

        @Test
        void whenDevServiceIsDisabledThenNothingIsRegistered(CapturedOutput output) {
            registry.registerDevService(service -> service
                    .name("postgres")
                    .description("Test Dev Service")
                    .properties(TestDevServicesProperties.DEFAULT.withEnabled(false))
                    .container(TestPostgresContainer.class, TestPostgresContainer::new));

            // The auto-configuration condition normally backs off earlier. The registry honors the
            // properties contract for a dev service whose condition doesn't read the property.
            assertThat(beanFactory.getBeanDefinitionNames()).noneMatch(name -> name.startsWith(BeanName.PREFIX));
            assertThat(output).contains("Dev Service 'postgres' is disabled");
        }

        @Test
        void whenApplicationDefinesConnectionDetailsThenNoDiscoveryHappens() {
            enableDevMode();
            AtomicInteger lookups = new AtomicInteger();
            DevServicesRegistry registry = new DevServicesRegistry(beanFactory, new StandardEnvironment(), new DefaultResourceLoader(), serviceName -> {
                lookups.incrementAndGet();
                return List.of(discoveredContainer());
            }, () -> false);
            beanFactory.registerSingleton("userConnectionDetails", new TestConnectionDetails("user-defined"));

            registerSharedDevService(registry);

            // The container runtime is never queried either.
            assertThat(beanFactory.getBeanDefinitionNames()).noneMatch(name -> name.startsWith(BeanName.PREFIX));
            assertThat(lookups.get()).isZero();
        }

        @Test
        void whenApplicationDefinesOtherConnectionDetailsThenDevServiceIsRegistered() {
            beanFactory.registerSingleton("otherConnectionDetails", (OtherConnectionDetails) () -> {});

            registerSharedDevService(registry);

            assertThat(beanFactory.containsBeanDefinition(BeanName.CONTAINER.of("postgres"))).isTrue();
        }

        private void registerCategorizedDevService(String name, String category, Supplier<TestPostgresContainer> containerSupplier) {
            registry.registerDevService(service -> {
                service.name(name)
                        .description("Test Dev Service")
                        .properties(TestDevServicesProperties.DEFAULT)
                        .container(TestPostgresContainer.class, containerSupplier);
                if (category != null) {
                    service.category(category);
                }
            });
        }

    }

    @Nested
    class ManagedContainer {

        @Test
        void whenContainerCreatedThenCommonPropertiesAreApplied() {
            registry.registerDevService(service -> service
                    .name("postgres")
                    .description("Test Dev Service")
                    .properties(TestDevServicesProperties.WITH_ENVIRONMENT)
                    .container(TestPostgresContainer.class, TestPostgresContainer::new));

            var container = beanFactory.getBean(BeanName.CONTAINER.of("postgres"), GenericContainer.class);

            assertThat(container.getEnvMap()).containsEntry("KEY", "value");
            assertThat(container.getNetworkAliases()).contains("db");
        }

        @Test
        void whenPortDeclaredAndConfiguredThenContainerBindsItToTheHost() {
            registry.registerDevService(service -> service
                    .name("postgres")
                    .description("Test Dev Service")
                    .properties(TestDevServicesProperties.DEFAULT.withPort(1234))
                    .container(TestPostgresContainer.class, TestPostgresContainer::new)
                    .port(5432));

            var container = beanFactory.getBean(BeanName.CONTAINER.of("postgres"), GenericContainer.class);

            assertThat(container.getPortBindings()).containsExactly("1234:5432/tcp");
        }

        @Test
        void whenAdditionalPortsDeclaredThenContainerBindsTheConfiguredOnes() {
            registry.registerDevService(service -> service
                    .name("postgres")
                    .description("Test Dev Service")
                    .properties(TestDevServicesProperties.DEFAULT.withPort(1234))
                    .container(TestPostgresContainer.class, TestPostgresContainer::new)
                    .port(5432)
                    .additionalPort(8080, 5678)
                    // A host port of 0 is a fixed-port property left unset: nothing is bound.
                    .additionalPort(9090, 0));

            var container = beanFactory.getBean(BeanName.CONTAINER.of("postgres"), GenericContainer.class);

            assertThat(container.getPortBindings()).containsExactlyInAnyOrder("1234:5432/tcp", "5678:8080/tcp");
        }

        @Test
        void whenContainerDeclaresBindingsThenTheyAreKept() {
            registry.registerDevService(service -> service
                    .name("postgres")
                    .description("Test Dev Service")
                    .properties(TestDevServicesProperties.DEFAULT.withPort(1234))
                    .container(TestPostgresContainer.class, () -> {
                        var container = new TestPostgresContainer();
                        container.setPortBindings(List.of("4321:2345"));
                        return container;
                    })
                    .port(5432));

            var container = beanFactory.getBean(BeanName.CONTAINER.of("postgres"), GenericContainer.class);

            assertThat(container.getPortBindings()).containsExactlyInAnyOrder("4321:2345/tcp", "1234:5432/tcp");
        }

        @Test
        void whenPortDeclaredButNotConfiguredThenContainerHasNoFixedPort() {
            registry.registerDevService(service -> service
                    .name("postgres")
                    .description("Test Dev Service")
                    .properties(TestDevServicesProperties.DEFAULT)
                    .container(TestPostgresContainer.class, TestPostgresContainer::new)
                    .port(5432));

            var container = beanFactory.getBean(BeanName.CONTAINER.of("postgres"), GenericContainer.class);

            assertThat(container.getPortBindings()).isEmpty();
        }

        @Test
        void whenServiceRegisteredThenContainerIsIdentifiedButNotDiscoverable() {
            registerPostgresDevService(registry);

            var container = beanFactory.getBean(BeanName.CONTAINER.of("postgres"), GenericContainer.class);

            assertThat(container.getLabels())
                    .containsEntry(DevServiceLabels.NAME, "postgres")
                    .doesNotContainKey(DevServiceLabels.DISCOVERABLE)
                    .doesNotContainKey(DevServiceLabels.OWNER);
        }

        @Test
        void whenFrameworkStrategyInDevModeThenContainerIsDiscoverable() {
            enableDevMode();
            DevServicesRegistry registry = registryDiscovering(beanFactory, null);

            registerSharedDevService(registry);

            var container = beanFactory.getBean(BeanName.CONTAINER.of("postgres"), GenericContainer.class);

            // A discoverable container carries the identifier of the application that started it,
            // so that the application never discovers its own containers.
            assertThat(container.getLabels())
                    .containsEntry(DevServiceLabels.DISCOVERABLE, "true")
                    .containsEntry(DevServiceLabels.OWNER, DevServiceLabels.ownerId());
        }

        @Test
        void whenFrameworkStrategyOutsideDevModeThenContainerIsNotDiscoverable() {
            registerSharedDevService(registry);

            var container = beanFactory.getBean(BeanName.CONTAINER.of("postgres"), GenericContainer.class);

            assertThat(container.getLabels()).doesNotContainKey(DevServiceLabels.DISCOVERABLE);
        }

        @Test
        void whenFrameworkStrategyWithoutDiscoveryThenContainerIsNotDiscoverable() {
            enableDevMode();

            registry.registerDevService(service -> service
                    .name("postgres")
                    .description("Test Dev Service")
                    .properties(TestDevServicesProperties.FRAMEWORK)
                    .container(TestPostgresContainer.class, TestPostgresContainer::new));

            var container = beanFactory.getBean(BeanName.CONTAINER.of("postgres"), GenericContainer.class);

            assertThat(container.getLabels()).doesNotContainKey(DevServiceLabels.DISCOVERABLE);
        }

        @Test
        void whenTestcontainersStrategyThenContainerIsReusableButNotDiscoverable() {
            enableDevMode();
            DevServicesRegistry registry = registryWithReuseSupport(beanFactory, true);

            registry.registerDevService(service -> service
                    .name("postgres")
                    .description("Test Dev Service")
                    .properties(TestDevServicesProperties.TESTCONTAINERS)
                    .container(TestPostgresContainer.class, TestPostgresContainer::new)
                    .discovery(TestConnectionDetails.class, container -> new TestConnectionDetails(container.host())));

            var container = beanFactory.getBean(BeanName.CONTAINER.of("postgres"), GenericContainer.class);

            // The reuse strategies are mutually exclusive: a container reused via Testcontainers
            // is never discoverable. It carries no per-application label either, which would
            // change the Testcontainers reuse hash at every run.
            assertThat(container.isShouldBeReused()).isTrue();
            assertThat(container.getLabels())
                    .doesNotContainKey(DevServiceLabels.DISCOVERABLE)
                    .doesNotContainKey(DevServiceLabels.OWNER);
        }

        @Test
        void whenTestcontainersReuseIsNotEnabledThenContainerIsNotReusable() {
            enableDevMode();
            DevServicesRegistry registry = registryWithReuseSupport(beanFactory, false);

            registry.registerDevService(service -> service
                    .name("postgres")
                    .description("Test Dev Service")
                    .properties(TestDevServicesProperties.TESTCONTAINERS)
                    .container(TestPostgresContainer.class, TestPostgresContainer::new));

            var container = beanFactory.getBean(BeanName.CONTAINER.of("postgres"), GenericContainer.class);

            assertThat(container.isShouldBeReused()).isFalse();
        }

        @Test
        void whenTestcontainersStrategyWithSharedNetworkThenContainerIsNotReusable() {
            enableDevMode();
            DevServicesRegistry registry = new DevServicesRegistry(beanFactory, DevServicesTestSupport.networkEnabledEnvironment(), new DefaultResourceLoader(),
                    serviceName -> List.of(), () -> true);

            registry.registerDevService(service -> service
                    .name("postgres")
                    .description("Test Dev Service")
                    .properties(TestDevServicesProperties.TESTCONTAINERS)
                    .container(TestPostgresContainer.class, TestPostgresContainer::new));

            var container = beanFactory.getBean(BeanName.CONTAINER.of("postgres"), GenericContainer.class);

            // A reusable container on the shared network would be left running after every run,
            // since the Testcontainers reuse hash includes the network, created anew with every run.
            assertThat(container.isShouldBeReused()).isFalse();
            assertThat(container.getNetwork()).isNotNull();
        }

        @Test
        void whenNoneStrategyThenContainerIsNotReusable() {
            enableDevMode();

            registerPostgresDevService(registry);

            var container = beanFactory.getBean(BeanName.CONTAINER.of("postgres"), GenericContainer.class);

            assertThat(container.isShouldBeReused()).isFalse();
        }

        @Test
        @SuppressWarnings({"unchecked"})
        void whenCustomizerWithoutBeanDefinitionIsTypedForOtherContainerThenItIsSkipped() {
            // A lambda registered as a singleton has no bean definition to resolve its type from.
            beanFactory.registerSingleton("linkContainerCustomizer",
                    (DevServiceContainerCustomizer<TestLinkContainer>) container -> container.withLabel("customized", "true"));

            registerPostgresDevService(registry);

            var container = beanFactory.getBean(BeanName.CONTAINER.of("postgres"), GenericContainer.class);

            assertThat(container.getLabels()).doesNotContainKey("customized");
        }

        @Test
        void whenCustomizerRegisteredInParentContextThenAppliedToMatchingContainer() {
            AtomicBoolean applied = registerTypedCustomizerInParentContext();

            registry.registerDevService(service -> service
                    .name("linky")
                    .description("Test Dev Service")
                    .properties(TestDevServicesProperties.DEFAULT)
                    .container(TestLinkContainer.class, TestLinkContainer::new));
            beanFactory.getBean(BeanName.CONTAINER.of("linky"), GenericContainer.class);

            assertThat(applied).isTrue();
        }

        @Test
        void whenCustomizerRegisteredInParentContextThenNotAppliedToOtherContainer() {
            AtomicBoolean applied = registerTypedCustomizerInParentContext();

            registerPostgresDevService(registry);
            beanFactory.getBean(BeanName.CONTAINER.of("postgres"), GenericContainer.class);

            assertThat(applied).isFalse();
        }

        @Test
        void whenNetworkEnabledThenContainerJoinsTheNetwork() {
            DevServicesRegistry registry = registryWithNetworkEnabled(beanFactory);
            TestNetwork network = new TestNetwork("net-1");
            registerNetworkBean(beanFactory, network);

            registerPostgresDevService(registry);

            var container = beanFactory.getBean(BeanName.CONTAINER.of("postgres"), GenericContainer.class);

            assertThat(container.getNetwork()).isSameAs(network);
            assertThat(container.getNetworkAliases()).contains("postgres");
        }

        @Test
        void whenNetworkSetByCustomizerThenItIsHonored() {
            DevServicesRegistry registry = registryWithNetworkEnabled(beanFactory);
            TestNetwork customizerNetwork = new TestNetwork("net-customizer");
            registerNetworkBean(beanFactory, new TestNetwork("net-bean"));
            beanFactory.registerSingleton("networkCustomizer",
                    (DevServiceContainerCustomizer<GenericContainer<?>>) container -> container.withNetwork(customizerNetwork));

            registerPostgresDevService(registry);

            var container = beanFactory.getBean(BeanName.CONTAINER.of("postgres"), GenericContainer.class);

            // Customizers run before the container joins the shared network.
            assertThat(container.getNetwork()).isSameAs(customizerNetwork);
        }

        @Test
        void whenRestartScopeIsActiveThenContainerIsRestartScoped() {
            enableDevMode();
            beanFactory.registerScope(DevServiceRestartSupport.SCOPE_NAME, new SimpleThreadScope());

            registerPostgresDevService(registry);

            assertThat(beanFactory.getBeanDefinition(BeanName.CONTAINER.of("postgres")).getScope()).isEqualTo("restart");
        }

        @Test
        void whenRestartScopeIsNotActiveThenContainerIsSingleton() {
            enableDevMode();

            registerPostgresDevService(registry);

            assertThat(beanFactory.getBeanDefinition(BeanName.CONTAINER.of("postgres")).getScope()).isEqualTo("singleton");
        }

        /**
         * Register a {@link DevServiceContainerCustomizer} lambda typed for {@link TestLinkContainer}
         * as a bean definition in a parent bean factory, as a {@code @Bean} method in a parent context
         * would, and make it the parent of the test bean factory. Returns a flag set when the
         * customizer is actually applied.
         */
        private AtomicBoolean registerTypedCustomizerInParentContext() {
            AtomicBoolean applied = new AtomicBoolean(false);
            var parent = new DefaultListableBeanFactory();
            var customizerDefinition = new RootBeanDefinition();
            customizerDefinition.setTargetType(
                    ResolvableType.forClassWithGenerics(DevServiceContainerCustomizer.class, TestLinkContainer.class));
            customizerDefinition.setInstanceSupplier(() ->
                    (DevServiceContainerCustomizer<TestLinkContainer>) container -> applied.set(true));
            parent.registerBeanDefinition("typedContainerCustomizer", customizerDefinition);
            beanFactory.setParentBeanFactory(parent);
            return applied;
        }

    }

    @Nested
    class Discovery {

        @BeforeEach
        void devMode() {
            enableDevMode();
        }

        @Test
        void whenContainerDiscoveredThenConnectionDetailsAndRegistrationAreRegistered() {
            DevServicesRegistry registry = registryDiscovering(beanFactory, discoveredContainer());

            registerSharedDevService(registry);

            assertThat(beanFactory.containsBeanDefinition(BeanName.CONTAINER.of("postgres"))).isFalse();
            assertThat(beanFactory.containsBeanDefinition("devService.connectionDetails.postgres")).isTrue();

            BeanDefinition connectionDetailsDefinition = beanFactory.getBeanDefinition(BeanName.CONNECTION_DETAILS.of("postgres"));
            assertThat(connectionDetailsDefinition.getRole()).isEqualTo(BeanDefinition.ROLE_INFRASTRUCTURE);
            assertThat(BeanAttribute.AOT_EXCLUDED.isPresent(connectionDetailsDefinition)).isTrue();
            assertThat(ContainerImageMetadata.isPresent(connectionDetailsDefinition)).isTrue();
            assertThat(ContainerImageMetadata.getFrom(connectionDetailsDefinition).imageName()).isEqualTo("postgres:latest");

            var connectionDetails = beanFactory.getBean(BeanName.CONNECTION_DETAILS.of("postgres"), TestConnectionDetails.class);
            assertThat(connectionDetails.host()).isEqualTo("localhost");

            BeanDefinition registrationDefinition = beanFactory.getBeanDefinition(BeanName.REGISTRATION.of("postgres"));
            assertThat(registrationDefinition.getDependsOn()).isNull();
            assertThat(BeanAttribute.AOT_EXCLUDED.isPresent(registrationDefinition)).isTrue();

            var registration = beanFactory.getBean(BeanName.REGISTRATION.of("postgres"), DevServiceRegistration.class);
            assertThat(registration.name()).isEqualTo("postgres");
            assertThat(registration.origin()).isEqualTo(DevServiceRegistration.Origin.DISCOVERED);
        }

        @Test
        void whenNoContainerDiscoveredThenManagedContainerIsRegistered() {
            DevServicesRegistry registry = registryDiscovering(beanFactory, null);

            registerSharedDevService(registry);

            assertThat(beanFactory.containsBeanDefinition(BeanName.CONTAINER.of("postgres"))).isTrue();
            assertThat(beanFactory.containsBeanDefinition(BeanName.CONNECTION_DETAILS.of("postgres"))).isFalse();
        }

        @Test
        @SuppressWarnings({"unchecked", "rawtypes"})
        void whenConnectionDetailsFactoryReturnsWrongTypeThenManagedContainerIsRegistered() {
            DevServicesRegistry registry = registryDiscovering(beanFactory, discoveredContainer());

            registry.registerDevService(service -> service
                    .name("postgres")
                    .description("Test Dev Service")
                    .properties(TestDevServicesProperties.FRAMEWORK)
                    .container(TestPostgresContainer.class, TestPostgresContainer::new)
                    // Raw type to bypass the compile-time check and exercise the runtime net.
                    .discovery((Class) OtherConnectionDetails.class,
                            container -> new TestConnectionDetails(container.host())));

            assertThat(beanFactory.containsBeanDefinition(BeanName.CONTAINER.of("postgres"))).isTrue();
            assertThat(beanFactory.containsBeanDefinition(BeanName.CONNECTION_DETAILS.of("postgres"))).isFalse();
        }

        @Test
        void whenConnectionDetailsFactoryFailsThenManagedContainerIsRegistered() {
            DevServicesRegistry registry = registryDiscovering(beanFactory, discoveredContainer());

            registry.registerDevService(service -> service
                    .name("postgres")
                    .description("Test Dev Service")
                    .properties(TestDevServicesProperties.FRAMEWORK)
                    .container(TestPostgresContainer.class, TestPostgresContainer::new)
                    .discovery(TestConnectionDetails.class,
                            container -> {
                                throw new IllegalStateException("boom");
                            }));

            assertThat(beanFactory.containsBeanDefinition(BeanName.CONTAINER.of("postgres"))).isTrue();
            assertThat(beanFactory.containsBeanDefinition(BeanName.CONNECTION_DETAILS.of("postgres"))).isFalse();
        }

        @Test
        void whenOldestDiscoveredContainerIsUnusableThenNextOneIsUsed() {
            DiscoveredContainer unusable = DiscoveredContainer.builder().containerInfo(ContainerInfo.builder()
                    .id("def456")
                    .imageName("postgres:latest")
                    .names(List.of("unusable-postgres"))
                    .labels(Map.of(DevServiceLabels.NAME, "postgres", DevServiceLabels.DISCOVERABLE, "true"))
                    .state("running")
                    .status("running")
                    .build()).host("localhost").build();
            DevServicesRegistry registry = new DevServicesRegistry(beanFactory, new StandardEnvironment(), new DefaultResourceLoader(),
                    serviceName -> List.of(unusable, discoveredContainer()), () -> false);

            registry.registerDevService(service -> service
                    .name("postgres")
                    .description("Test Dev Service")
                    .properties(TestDevServicesProperties.FRAMEWORK)
                    .container(TestPostgresContainer.class, TestPostgresContainer::new)
                    .discovery(TestConnectionDetails.class,
                            container -> new TestConnectionDetails(container.host() + ":" + container.mappedPort(5432))));

            assertThat(beanFactory.containsBeanDefinition(BeanName.CONTAINER.of("postgres"))).isFalse();
            assertThat(beanFactory.getBean(BeanName.CONNECTION_DETAILS.of("postgres"), TestConnectionDetails.class).host())
                    .isEqualTo("localhost:54321");
        }

        @Test
        void whenNoneStrategyThenNoDiscoveryHappens() {
            DevServicesRegistry registry = registryDiscovering(beanFactory, discoveredContainer());

            registry.registerDevService(service -> service
                    .name("postgres")
                    .description("Test Dev Service")
                    .properties(TestDevServicesProperties.DEFAULT)
                    .container(TestPostgresContainer.class, TestPostgresContainer::new)
                    .discovery(TestConnectionDetails.class,
                            container -> new TestConnectionDetails(container.host())));

            assertThat(beanFactory.containsBeanDefinition(BeanName.CONTAINER.of("postgres"))).isTrue();
            assertThat(beanFactory.containsBeanDefinition(BeanName.CONNECTION_DETAILS.of("postgres"))).isFalse();
        }

        @Test
        void whenTestcontainersStrategyThenNoDiscoveryHappens() {
            DevServicesRegistry registry = new DevServicesRegistry(beanFactory, new StandardEnvironment(), new DefaultResourceLoader(),
                    serviceName -> List.of(discoveredContainer()), () -> true);

            registry.registerDevService(service -> service
                    .name("postgres")
                    .description("Test Dev Service")
                    .properties(TestDevServicesProperties.TESTCONTAINERS)
                    .container(TestPostgresContainer.class, TestPostgresContainer::new)
                    .discovery(TestConnectionDetails.class,
                            container -> new TestConnectionDetails(container.host())));

            // A container started by another application is not discovered: reuse is left to Testcontainers.
            assertThat(beanFactory.containsBeanDefinition(BeanName.CONTAINER.of("postgres"))).isTrue();
            assertThat(beanFactory.containsBeanDefinition(BeanName.CONNECTION_DETAILS.of("postgres"))).isFalse();
        }

        @Test
        void whenNotInDevModeThenNoDiscoveryHappens() {
            enableTestMode();
            DevServicesRegistry registry = registryDiscovering(beanFactory, discoveredContainer());

            registerSharedDevService(registry);

            assertThat(beanFactory.containsBeanDefinition(BeanName.CONTAINER.of("postgres"))).isTrue();
            assertThat(beanFactory.containsBeanDefinition(BeanName.CONNECTION_DETAILS.of("postgres"))).isFalse();
        }

    }

    @Nested
    class Restart {

        /**
         * Stands in for the scope DevTools registers, which outlives the application contexts.
         */
        private final SimpleThreadScope restartScope = new SimpleThreadScope();

        @BeforeEach
        void devMode() {
            enableDevMode();
        }

        @Test
        void whenConfigurationChangedAcrossRestartsThenContainerIsKept() {
            RestartingContainer first = restart(Map.of("test.dev.services.postgres.image-name", "postgres:17"),
                    TestDevServicesProperties.DEFAULT, List.of());
            RestartingContainer second = restart(Map.of("test.dev.services.postgres.image-name", "postgres:18"),
                    TestDevServicesProperties.DEFAULT, List.of());

            assertThat(second).isSameAs(first);
            assertThat(first.stopped).isFalse();
        }

        @Test
        void whenKeptContainerIsNoLongerRunningThenANewOneTakesItsPlace() {
            RestartingContainer first = restart(Map.of(), TestDevServicesProperties.DEFAULT, List.of());
            // The container was removed outside the application.
            first.running = false;
            RestartingContainer second = restart(Map.of(), TestDevServicesProperties.DEFAULT, List.of());

            assertThat(second).isNotSameAs(first);
            assertThat(first.stopped).isTrue();
        }

        @Test
        void whenKeptContainerClassWasReloadedThenANewOneTakesItsPlace() {
            RestartingContainer first = restart(Map.of(), TestDevServicesProperties.DEFAULT, List.of());
            // DevTools reloaded the container class: the kept instance belongs to the previous class
            // loader, which the new bean definition's type stands in for.
            TestPostgresContainer second = restartedBeanFactory(Map.of(), TestDevServicesProperties.DEFAULT, List.of(),
                    TestPostgresContainer.class, TestPostgresContainer::new)
                    .getBean(BeanName.CONTAINER.of("postgres"), TestPostgresContainer.class);

            assertThat(second).isNotSameAs(first);
            assertThat(first.stopped).isTrue();
        }

        @Test
        void whenKeptContainerIsNoLongerRunningThenDiscoveryHappensAgain() {
            RestartingContainer first = restart(Map.of(), TestDevServicesProperties.FRAMEWORK, List.of());
            first.running = false;
            DefaultListableBeanFactory restarted = restartedBeanFactory(Map.of(),
                    TestDevServicesProperties.FRAMEWORK, List.of(discoveredContainer()));

            assertThat(restarted.containsBeanDefinition(BeanName.CONTAINER.of("postgres"))).isFalse();
            assertThat(restarted.containsBeanDefinition(BeanName.CONNECTION_DETAILS.of("postgres"))).isTrue();
        }

        @Test
        void whenDevServiceIsSkippedAfterRestartThenKeptContainerIsStopped() {
            RestartingContainer first = restart(Map.of(), TestDevServicesProperties.DEFAULT, List.of());
            // The application now defines its own connection details, so the dev service is not registered.
            var restarted = new DefaultListableBeanFactory();
            restarted.registerScope(DevServiceRestartSupport.SCOPE_NAME, restartScope);
            restarted.registerSingleton("userConnectionDetails", new TestConnectionDetails("user-defined"));

            new DevServicesRegistry(restarted, new StandardEnvironment(), new DefaultResourceLoader(), serviceName -> List.of(), () -> false)
                    .registerDevService(service -> service
                            .name("postgres")
                            .description("Test Dev Service")
                            .properties(TestDevServicesProperties.DEFAULT)
                            .container(RestartingContainer.class, RestartingContainer::new)
                            .discovery(TestConnectionDetails.class, container -> new TestConnectionDetails(container.host())));

            // The kept container would otherwise run until the JVM exits, holding e.g. its fixed port.
            assertThat(restarted.getBeanDefinitionNames()).noneMatch(name -> name.startsWith(BeanName.PREFIX));
            assertThat(first.stopped).isTrue();
            assertThat(restartScope.remove(BeanName.CONTAINER.of("postgres"))).isNull();
        }

        @Test
        void whenManagedContainerKeptAcrossRestartsThenNoDiscoveryHappens() {
            RestartingContainer first = restart(Map.of(), TestDevServicesProperties.FRAMEWORK, List.of());
            // Another application has started a discoverable container in the meantime.
            RestartingContainer second = restart(Map.of(), TestDevServicesProperties.FRAMEWORK, List.of(discoveredContainer()));

            assertThat(second).isSameAs(first);
        }

        /**
         * Simulate an application start, or a DevTools restart when called again: a new bean factory
         * sharing the same restart scope, in which the dev service is registered with the given
         * configuration, returning the container bean.
         */
        private RestartingContainer restart(Map<String, Object> configuration, BaseDevServicesProperties properties,
                List<DiscoveredContainer> discoveredContainers) {
            return restartedBeanFactory(configuration, properties, discoveredContainers)
                    .getBean(BeanName.CONTAINER.of("postgres"), RestartingContainer.class);
        }

        private DefaultListableBeanFactory restartedBeanFactory(Map<String, Object> configuration, BaseDevServicesProperties properties,
                List<DiscoveredContainer> discoveredContainers) {
            return restartedBeanFactory(configuration, properties, discoveredContainers, RestartingContainer.class, RestartingContainer::new);
        }

        private <C extends GenericContainer<?>> DefaultListableBeanFactory restartedBeanFactory(Map<String, Object> configuration,
                BaseDevServicesProperties properties, List<DiscoveredContainer> discoveredContainers, Class<C> containerType, Supplier<C> containerSupplier) {
            var restartedBeanFactory = new DefaultListableBeanFactory();
            restartedBeanFactory.registerScope(DevServiceRestartSupport.SCOPE_NAME, restartScope);
            var environment = new StandardEnvironment();
            environment.getPropertySources().addFirst(new MapPropertySource("test", configuration));
            new DevServicesRegistry(restartedBeanFactory, environment, new DefaultResourceLoader(), serviceName -> discoveredContainers, () -> false)
                    .registerDevService(service -> service
                            .name("postgres")
                            .description("Test Dev Service")
                            .properties(properties)
                            .container(containerType, containerSupplier)
                            .discovery(TestConnectionDetails.class,
                                    container -> new TestConnectionDetails(container.host())));
            return restartedBeanFactory;
        }

    }

    @Nested
    class Ssl {

        private final AtomicReference<SslBundle> configuredBundle = new AtomicReference<>();

        private final DevServiceSslConfigurer<TestLinkContainer> configurer = (container, bundle) -> {
            configuredBundle.set(bundle);
            container.withEnv("SSL", "true");
        };

        @Test
        void whenSslEnabledThenContainerHasTrustStoreAndSslAnnotationsNextToTheServiceConnection() {
            registerSslDevService(TestSslDevServicesProperties.pem());

            var beanDefinition = (DevServiceContainerBeanDefinition) beanFactory.getBeanDefinition(BeanName.CONTAINER.of("secure"));

            MergedAnnotations annotations = beanDefinition.getAnnotations();
            assertThat(annotations.isPresent(ServiceConnection.class)).isTrue();
            assertThat(annotations.get(PemTrustStore.class).getString("certificate")).startsWith("-----BEGIN CERTIFICATE-----");
            assertThat(annotations.isPresent(org.springframework.boot.testcontainers.service.connection.Ssl.class)).isTrue();
        }

        @Test
        void whenSslEnabledThenContainerIsConfiguredWithTheBundleBeforeStart() {
            registerSslDevService(TestSslDevServicesProperties.jks());

            var container = beanFactory.getBean(BeanName.CONTAINER.of("secure"), GenericContainer.class);

            assertThat(container.getEnvMap()).containsEntry("SSL", "true");
            assertThat(configuredBundle.get()).isNotNull();
            assertThat(configuredBundle.get().getStores().getKeyStore()).isNotNull();
        }

        @Test
        void whenSslEnabledThenBundleIsRegisteredInTheApplicationBundles() {
            registerSslDevService(TestSslDevServicesProperties.pem());

            BeanDefinition beanDefinition = beanFactory.getBeanDefinition("devService.sslBundle.secure");
            assertThat(beanDefinition.getRole()).isEqualTo(BeanDefinition.ROLE_INFRASTRUCTURE);
            assertThat(BeanAttribute.AOT_EXCLUDED.isPresent(beanDefinition)).isTrue();

            // Applied by Spring Boot's SslAutoConfiguration to the registry backing SslBundles.
            var registrar = beanFactory.getBean("devService.sslBundle.secure", SslBundleRegistrar.class);
            var sslBundles = new DefaultSslBundleRegistry();
            registrar.registerBundles(sslBundles);
            assertThat(sslBundles.getBundleNames()).containsExactly("dev-services-secure");
            // The bundle the application connects with: it trusts the dev service and presents nothing.
            SslBundle registered = sslBundles.getBundle("dev-services-secure");
            assertThat(registered.getStores().getKeyStore()).isNull();
            assertThat(registered.getStores().getTrustStore()).isNotNull();
        }

        @Test
        void whenSslEnabledThenRegistrationReportsTheBundle(CapturedOutput output) {
            registerSslDevService(TestSslDevServicesProperties.pem());

            var registration = beanFactory.getBean(BeanName.REGISTRATION.of("secure"), DevServiceRegistration.class);

            assertThat(registration.sslBundleName()).isEqualTo("dev-services-secure");
            assertThat(output).contains("Dev Service 'secure' is ready in container").contains("ssl");
        }

        @Test
        void whenSslEnabledWithoutKeyStoreThenOnlySslAnnotationIsDeclaredAndNoBundleExists() {
            // The container generates its own certificate, which Spring Boot extracts.
            registry.registerDevService(service -> service
                    .name("secure")
                    .description("Test Dev Service")
                    .properties(TestSslDevServicesProperties.enabledWithoutKeyStore())
                    .container(TestLinkContainer.class, TestLinkContainer::new)
                    .ssl(configurer, SslKeyStoreRequirement.OPTIONAL));

            var beanDefinition = (DevServiceContainerBeanDefinition) beanFactory.getBeanDefinition(BeanName.CONTAINER.of("secure"));
            assertThat(beanDefinition.getAnnotations().isPresent(org.springframework.boot.testcontainers.service.connection.Ssl.class)).isTrue();
            assertThat(beanDefinition.getAnnotations().isPresent(PemTrustStore.class)).isFalse();
            assertThat(beanFactory.containsBeanDefinition("devService.sslBundle.secure")).isFalse();

            var container = beanFactory.getBean(BeanName.CONTAINER.of("secure"), GenericContainer.class);
            assertThat(container.getEnvMap()).doesNotContainKey("SSL");
            assertThat(configuredBundle.get()).isNull();

            var registration = beanFactory.getBean(BeanName.REGISTRATION.of("secure"), DevServiceRegistration.class);
            assertThat(registration.sslBundleName()).isNull();
        }

        @Test
        void whenSslDisabledThenNothingIsConfigured() {
            registerSslDevService(new TestSslDevServicesProperties());

            var beanDefinition = (DevServiceContainerBeanDefinition) beanFactory.getBeanDefinition(BeanName.CONTAINER.of("secure"));
            assertThat(beanDefinition.getAnnotations().isPresent(org.springframework.boot.testcontainers.service.connection.Ssl.class)).isFalse();
            assertThat(beanFactory.containsBeanDefinition("devService.sslBundle.secure")).isFalse();

            var container = beanFactory.getBean(BeanName.CONTAINER.of("secure"), GenericContainer.class);
            assertThat(container.getEnvMap()).doesNotContainKey("SSL");

            var registration = beanFactory.getBean(BeanName.REGISTRATION.of("secure"), DevServiceRegistration.class);
            assertThat(registration.sslBundleName()).isNull();
        }

        @Test
        void whenSslEnabledAndContainerDiscoveredThenConnectionDetailsGetTheTrustBundle() {
            enableDevMode();
            AtomicReference<DiscoveredContainer> discovered = new AtomicReference<>();
            DevServicesRegistry registry = registryDiscovering(beanFactory, discoveredContainer());

            registry.registerDevService(service -> service
                    .name("postgres")
                    .description("Test Dev Service")
                    .properties(TestSslDevServicesProperties.pem().withReuseStrategy(ReuseStrategy.FRAMEWORK))
                    .container(TestLinkContainer.class, TestLinkContainer::new)
                    .ssl(configurer)
                    .discovery(TestConnectionDetails.class, container -> {
                        discovered.set(container);
                        return new TestConnectionDetails(container.host());
                    }));

            assertThat(beanFactory.containsBeanDefinition(BeanName.CONTAINER.of("postgres"))).isFalse();
            assertThat(beanFactory.containsBeanDefinition("devService.sslBundle.postgres")).isTrue();
            // The application trusts the certificate the dev service is configured to present, and presents none.
            assertThat(discovered.get().sslBundle()).isNotNull();
            assertThat(discovered.get().sslBundle().getStores().getTrustStore()).isNotNull();
            assertThat(discovered.get().sslBundle().getStores().getKeyStore()).isNull();

            var registration = beanFactory.getBean(BeanName.REGISTRATION.of("postgres"), DevServiceRegistration.class);
            assertThat(registration.sslBundleName()).isEqualTo("dev-services-postgres");
        }

        @Test
        void whenSslEnabledAndDevServiceSkippedThenNoBundleIsRegistered() {
            beanFactory.registerBeanDefinition("myConnectionDetails", new RootBeanDefinition(TestConnectionDetails.class, () -> new TestConnectionDetails("localhost")));

            registry.registerDevService(service -> service
                    .name("secure")
                    .description("Test Dev Service")
                    .properties(TestSslDevServicesProperties.pem())
                    .container(TestLinkContainer.class, TestLinkContainer::new)
                    .ssl(configurer)
                    .discovery(TestConnectionDetails.class, container -> new TestConnectionDetails(container.host())));

            assertThat(beanFactory.containsBeanDefinition("devService.sslBundle.secure")).isFalse();
        }

        private void registerSslDevService(TestSslDevServicesProperties properties) {
            registry.registerDevService(service -> service
                    .name("secure")
                    .description("Test Dev Service")
                    .properties(properties)
                    .container(TestLinkContainer.class, TestLinkContainer::new)
                    .ssl(configurer));
        }

    }

    @Nested
    class Links {

        @Test
        void whenLinksDeclaredThenRegistrationResolvesThemAgainstTheContainer() {
            registry.registerDevService(service -> service
                    .name("linky")
                    .description("Test Dev Service")
                    .properties(TestDevServicesProperties.DEFAULT)
                    .container(TestLinkContainer.class, TestLinkContainer::new)
                    .link("UI", 8080)
                    .link("Docs", 8080, "/docs"));

            var registration = beanFactory.getBean(BeanName.REGISTRATION.of("linky"), DevServiceRegistration.class);

            assertThat(registration.origin()).isEqualTo(DevServiceRegistration.Origin.MANAGED);
            assertThat(registration.links()).containsExactly(
                    new DevServiceLink("UI", "http://localhost:1234"),
                    new DevServiceLink("Docs", "http://localhost:1234/docs"));
        }

        @Test
        void whenContainerNotStartedThenLinksAreSkippedWithoutWarning(CapturedOutput output) {
            // As when an application context is created without the Testcontainers lifecycle.
            registry.registerDevService(service -> service
                    .name("linky")
                    .description("Test Dev Service")
                    .properties(TestDevServicesProperties.DEFAULT)
                    .container(TestUnstartedLinkContainer.class, TestUnstartedLinkContainer::new)
                    .link("UI", 8080));

            var registration = beanFactory.getBean(BeanName.REGISTRATION.of("linky"), DevServiceRegistration.class);

            assertThat(registration.links()).isEmpty();
            assertThat(output).doesNotContain("skipping the 'UI' link");
        }

        @Test
        void whenNoLinksDeclaredThenRegistrationLinksAreEmpty() {
            registerPostgresDevService(registry);

            var registration = beanFactory.getBean(BeanName.REGISTRATION.of("postgres"), DevServiceRegistration.class);

            assertThat(registration.links()).isEmpty();
        }

        @Test
        void whenDiscoveredThenLinksAreResolvedAgainstTheDiscoveredContainer() {
            enableDevMode();
            DevServicesRegistry registry = registryDiscovering(beanFactory, discoveredContainer());

            registerSharedDevService(registry, service -> service.link("Console", 5432, "/console"));

            var registration = beanFactory.getBean(BeanName.REGISTRATION.of("postgres"), DevServiceRegistration.class);

            // The same declaration resolves against the host and published ports of the discovered container.
            assertThat(registration.origin()).isEqualTo(DevServiceRegistration.Origin.DISCOVERED);
            assertThat(registration.links()).containsExactly(new DevServiceLink("Console", "http://localhost:54321/console"));
        }

    }

}
