package io.arconia.dev.services.core.registration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.env.StandardEnvironment;

import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.core.TestDevServicesProperties;
import io.arconia.dev.services.core.registration.DevServicesTestSupport.TestLinkContainer;

import static io.arconia.dev.services.core.registration.DevServicesTestSupport.discoveredContainer;
import static io.arconia.dev.services.core.registration.DevServicesTestSupport.enableDevMode;
import static io.arconia.dev.services.core.registration.DevServicesTestSupport.registerPostgresDevService;
import static io.arconia.dev.services.core.registration.DevServicesTestSupport.registerSharedDevService;
import static io.arconia.dev.services.core.registration.DevServicesTestSupport.registryDiscovering;
import static io.arconia.dev.services.core.registration.DevServicesTestSupport.resetBootstrapMode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link DevServicesRegistry}: the links a dev service declares, resolved
 * against the container it runs in.
 */
@ExtendWith(OutputCaptureExtension.class)
class DevServicesLinksTests {

    private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

    private final DevServicesRegistry registry = new DevServicesRegistry(beanFactory, new StandardEnvironment());

    @BeforeEach
    @AfterEach
    void reset() {
        resetBootstrapMode();
    }

    @Test
    void whenLinksDeclaredThenRegistrationResolvesThemAgainstTheContainer() {
        registry.registerDevService(service -> service
                .name("linky")
                .description("Test Dev Service")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(TestLinkContainer.class, TestLinkContainer::new)
                .link("UI", 8080)
                .link("Docs", 8080, "/docs"));

        var registration = beanFactory.getBean("devService.registration.linky", DevServiceRegistration.class);

        // Links are reported in the order they are declared.
        assertThat(registration.links()).containsExactly(
                new DevServiceLink("UI", "http://localhost:1234"),
                new DevServiceLink("Docs", "http://localhost:1234/docs"));
    }

    @Test
    void whenManagedLinkPortIsNotExposedThenOnlyThatLinkIsSkipped(CapturedOutput output) {
        registry.registerDevService(service -> service
                .name("linky")
                .description("Test Dev Service")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(TestLinkContainer.class, TestLinkContainer::new)
                .link("UI", 8080)
                .link("Hidden", 9999));

        var registration = beanFactory.getBean("devService.registration.linky", DevServiceRegistration.class);

        // The dev service declares a port its container doesn't expose, which it can fix.
        assertThat(registration.links()).containsExactly(new DevServiceLink("UI", "http://localhost:1234"));
        assertThat(output).contains("Skipping the 'Hidden' link of the 'linky' dev service");
    }

    @Test
    void whenLinkIsInvalidThenRegistrationFails() {
        assertThatIllegalArgumentException().isThrownBy(() -> registry.registerDevService(service -> service
                .name("linky")
                .description("Test Dev Service")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(TestLinkContainer.class, TestLinkContainer::new)
                .link("UI", 8080, "docs")))
                .withMessageContaining("path must be empty or start with '/'");
    }

    @Test
    void whenNoLinksDeclaredThenRegistrationLinksAreEmpty() {
        registerPostgresDevService(registry);

        var registration = beanFactory.getBean("devService.registration.postgres", DevServiceRegistration.class);

        assertThat(registration.links()).isEmpty();
    }

    @Test
    void whenDiscoveredThenLinksAreResolvedAgainstTheDiscoveredContainer() {
        enableDevMode();
        DevServicesRegistry registry = registryDiscovering(beanFactory, discoveredContainer());

        registerSharedDevService(registry, service -> service.link("Console", 5432, "/console"));

        var registration = beanFactory.getBean("devService.registration.postgres", DevServiceRegistration.class);

        // The same declaration resolves against the host and published ports of the discovered container.
        assertThat(registration.origin()).isEqualTo(DevServiceRegistration.Origin.DISCOVERED);
        assertThat(registration.links()).containsExactly(new DevServiceLink("Console", "http://localhost:54321/console"));
    }

    @Test
    void whenDiscoveredLinkPortIsNotPublishedThenOnlyThatLinkIsSkipped(CapturedOutput output) {
        enableDevMode();
        DevServicesRegistry registry = registryDiscovering(beanFactory, discoveredContainer());

        // The application that started the container published 5432 but not 9999,
        // which is an expected condition rather than a failure.
        registerSharedDevService(registry, service -> service.link("UI", 5432).link("Hidden", 9999));

        var registration = beanFactory.getBean("devService.registration.postgres", DevServiceRegistration.class);

        assertThat(registration.links()).containsExactly(new DevServiceLink("UI", "http://localhost:54321"));
        assertThat(output).doesNotContain("Skipping the 'Hidden' link");
    }

}
