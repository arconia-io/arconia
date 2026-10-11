package io.arconia.dev.services.core.registration;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.StandardEnvironment;
import org.testcontainers.containers.GenericContainer;

import io.arconia.dev.services.api.registration.DevServiceLabels;
import io.arconia.dev.services.core.TestDevServicesProperties;
import io.arconia.dev.services.core.container.DevServiceContainerCustomizer;
import io.arconia.dev.services.core.registration.DevServicesTestSupport.TestLinkContainer;
import io.arconia.dev.services.core.registration.DevServicesTestSupport.TestPostgresContainer;

import static io.arconia.dev.services.core.registration.DevServicesTestSupport.registerPostgresDevService;
import static io.arconia.dev.services.core.registration.DevServicesTestSupport.resetBootstrapMode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link DevServicesRegistry}: the validation of a dev service specification,
 * the registration of its beans, and the customization of its container.
 *
 * @see DevServicesDiscoveryTests
 * @see DevServicesReuseTests
 * @see DevServicesNetworkTests
 * @see DevServicesLinksTests
 * @see DevServicesRestartTests
 */
class DevServicesRegistryTests {

    private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

    private final DevServicesRegistry registry = new DevServicesRegistry(beanFactory, new StandardEnvironment());

    @BeforeEach
    @AfterEach
    void reset() {
        resetBootstrapMode();
    }

    @Test
    void whenServiceNameIsNullThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> registry.registerDevService(service -> service
                        .name(null)
                        .container(TestPostgresContainer.class, TestPostgresContainer::new)))
                .withMessageContaining("service name cannot be null or empty");
    }

    @Test
    void whenServiceNameIsEmptyThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> registry.registerDevService(service -> service
                        .name("")
                        .description("Test Dev Service")
                        .properties(TestDevServicesProperties.DEFAULT)
                        .container(TestPostgresContainer.class, TestPostgresContainer::new)))
                .withMessageContaining("service name cannot be null or empty");
    }

    @Test
    void whenServiceNameIsInvalidThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> registry.registerDevService(service -> service
                        .name("Fancy_DB")
                        .description("Test Dev Service")
                        .properties(TestDevServicesProperties.DEFAULT)
                        .container(TestPostgresContainer.class, TestPostgresContainer::new)))
                .withMessageContaining("service name must contain only lowercase letters, digits, and dashes");
    }

    @Test
    void whenDescriptionIsMissingThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> registry.registerDevService(service -> service
                        .name("postgres")
                        .properties(TestDevServicesProperties.DEFAULT)
                        .container(TestPostgresContainer.class, TestPostgresContainer::new)))
                .withMessageContaining("service description cannot be null or empty");
    }

    @Test
    void whenContainerIsMissingThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> registry.registerDevService(service -> service
                        .name("postgres")
                        .description("Test Dev Service")
                        .properties(TestDevServicesProperties.DEFAULT)))
                .withMessageContaining("service container type cannot be null");
    }

    @Test
    void whenContainerTypeIsNullThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> registry.registerDevService(service -> service
                        .name("postgres")
                        .description("Test Dev Service")
                        .properties(TestDevServicesProperties.DEFAULT)
                        .container(null, TestPostgresContainer::new)))
                .withMessageContaining("container type cannot be null");
    }

    @Test
    void whenContainerSupplierIsNullThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> registry.registerDevService(service -> service
                        .name("postgres")
                        .description("Test Dev Service")
                        .properties(TestDevServicesProperties.DEFAULT)
                        .container(TestPostgresContainer.class, null)))
                .withMessageContaining("container supplier cannot be null");
    }

    @Test
    void whenServiceConnectionNameIsEmptyThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> registry.registerDevService(service -> service
                        .name("postgres")
                        .description("Test Dev Service")
                        .properties(TestDevServicesProperties.DEFAULT)
                        .container(TestPostgresContainer.class, TestPostgresContainer::new)
                        .serviceConnectionName("")))
                .withMessageContaining("serviceConnectionName cannot be null or empty");
    }

    @Test
    void whenServiceConnectionIsDisabledWithANameThenThrow() {
        assertThatIllegalStateException().isThrownBy(() -> registry.registerDevService(service -> service
                .name("postgres")
                .description("Test Dev Service")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(TestPostgresContainer.class, TestPostgresContainer::new)
                .serviceConnection(false)
                .serviceConnectionName("postgres")))
                .withMessageContaining("service connection name has no effect");
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

        var beanDefinition = (DevServiceContainerBeanDefinition) beanFactory.getBeanDefinition("devService.container.postgres");

        assertThat(beanDefinition.getAnnotations().get(ServiceConnection.class).getString("name")).isEqualTo("postgres");
    }

    @Test
    void whenServiceConnectionIsDisabledThenContainerHasNoServiceConnectionAnnotation() {
        registry.registerDevService(service -> service
                .name("postgres")
                .description("Test Dev Service")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(TestPostgresContainer.class, TestPostgresContainer::new)
                .serviceConnection(false));

        var beanDefinition = (DevServiceContainerBeanDefinition) beanFactory.getBeanDefinition("devService.container.postgres");

        assertThat(beanDefinition.getAnnotations().isPresent(ServiceConnection.class)).isFalse();
    }

    @Test
    void whenServiceRegisteredThenContainerIsIdentifiedButNotDiscoverable() {
        registerPostgresDevService(registry);

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(container.getLabels())
                .containsEntry(DevServiceLabels.NAME, "postgres")
                .doesNotContainKey(DevServiceLabels.DISCOVERABLE)
                .doesNotContainKey(DevServiceLabels.OWNER);
    }

    @Test
    void whenContainerCreatedThenCommonPropertiesAreApplied() {
        registry.registerDevService(service -> service
                .name("postgres")
                .description("Test Dev Service")
                .properties(TestDevServicesProperties.WITH_ENVIRONMENT)
                .container(TestPostgresContainer.class, TestPostgresContainer::new));

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(container.getEnvMap()).containsEntry("KEY", "value");
        assertThat(container.getNetworkAliases()).contains("db");
    }

    @Test
    @SuppressWarnings({"unchecked"})
    void whenCustomizerWithoutBeanDefinitionIsTypedForOtherContainerThenItIsSkipped() {
        // A lambda registered as a singleton has no bean definition to resolve its type from.
        beanFactory.registerSingleton("linkContainerCustomizer",
                (DevServiceContainerCustomizer<TestLinkContainer>) container -> container.withLabel("customized", "true"));

        registerPostgresDevService(registry);

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

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
        beanFactory.getBean("devService.container.linky", GenericContainer.class);

        assertThat(applied).isTrue();
    }

    @Test
    void whenCustomizerRegisteredInParentContextThenNotAppliedToOtherContainer() {
        AtomicBoolean applied = registerTypedCustomizerInParentContext();

        registerPostgresDevService(registry);
        beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(applied).isFalse();
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
