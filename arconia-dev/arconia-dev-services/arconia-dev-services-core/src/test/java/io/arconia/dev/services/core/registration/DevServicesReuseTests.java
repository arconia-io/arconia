package io.arconia.dev.services.core.registration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.env.StandardEnvironment;
import org.testcontainers.containers.GenericContainer;

import io.arconia.dev.services.api.config.BaseDevServicesProperties;
import io.arconia.dev.services.api.config.ReuseStrategy;
import io.arconia.dev.services.core.TestDevServicesProperties;
import io.arconia.dev.services.core.registration.DevServicesTestSupport.TestConnectionDetails;
import io.arconia.dev.services.core.registration.DevServicesTestSupport.TestPostgresContainer;

import static io.arconia.dev.services.core.registration.DevServicesTestSupport.enableDevMode;
import static io.arconia.dev.services.core.registration.DevServicesTestSupport.registerPostgresDevService;
import static io.arconia.dev.services.core.registration.DevServicesTestSupport.registryWithReuseSupport;
import static io.arconia.dev.services.core.registration.DevServicesTestSupport.resetBootstrapMode;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevServicesRegistry}: the reuse strategy in effect for a dev service,
 * and its application to the container.
 */
class DevServicesReuseTests {

    private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

    private final DevServicesRegistry registry = new DevServicesRegistry(beanFactory, new StandardEnvironment());

    @BeforeEach
    @AfterEach
    void reset() {
        resetBootstrapMode();
    }

    @Test
    void whenNoneStrategyThenContainerIsNotReusable() {
        enableDevMode();

        registerPostgresDevService(registry);

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(container.isShouldBeReused()).isFalse();
    }

    @Test
    void whenTestcontainersStrategyIsEffectiveThenContainerIsReusable() {
        enableDevMode();
        DevServicesRegistry registry = registryWithReuseSupport(beanFactory, true);

        registry.registerDevService(service -> service
                .name("postgres")
                .description("Test Dev Service")
                .properties(TestDevServicesProperties.TESTCONTAINERS)
                .container(TestPostgresContainer.class, TestPostgresContainer::new));

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(container.isShouldBeReused()).isTrue();
    }

    @Test
    void whenReuseStrategyCanBeAppliedThenItIsInEffect() {
        enableDevMode();

        var decision = registry.reuseDecision(serviceSpec(TestDevServicesProperties.FRAMEWORK, true));

        assertThat(decision.effective()).isEqualTo(ReuseStrategy.FRAMEWORK);
        assertThat(decision.reason()).isNull();
    }

    @Test
    void whenNotInDevModeThenNoReuseStrategyApplies() {
        var decision = registry.reuseDecision(serviceSpec(TestDevServicesProperties.FRAMEWORK, true));

        assertThat(decision.effective()).isEqualTo(ReuseStrategy.NONE);
        assertThat(decision.reason()).isNull();
    }

    @Test
    void whenFrameworkStrategyWithoutDiscoveryThenReuseStrategyIsNotApplied() {
        enableDevMode();

        var decision = registry.reuseDecision(serviceSpec(TestDevServicesProperties.FRAMEWORK, false));

        assertThat(decision.effective()).isEqualTo(ReuseStrategy.NONE);
        assertThat(decision.reason()).contains("not supported");
    }

    @Test
    void whenTestcontainersReuseIsNotEnabledThenReuseStrategyIsNotApplied() {
        enableDevMode();

        var decision = registryWithReuseSupport(beanFactory, false).reuseDecision(serviceSpec(TestDevServicesProperties.TESTCONTAINERS, false));

        assertThat(decision.effective()).isEqualTo(ReuseStrategy.NONE);
        assertThat(decision.reason()).contains("testcontainers.reuse.enable").contains("TESTCONTAINERS_REUSE_ENABLE");
    }

    @Test
    void whenTestcontainersReuseIsEnabledThenReuseStrategyIsInEffect() {
        enableDevMode();

        var decision = registryWithReuseSupport(beanFactory, true).reuseDecision(serviceSpec(TestDevServicesProperties.TESTCONTAINERS, false));

        assertThat(decision.effective()).isEqualTo(ReuseStrategy.TESTCONTAINERS);
        assertThat(decision.reason()).isNull();
    }

    private static DevServiceSpec serviceSpec(BaseDevServicesProperties properties, boolean discovery) {
        DevServiceSpec service = new DevServiceSpec()
                .name("postgres")
                .description("Test Dev Service")
                .properties(properties)
                .container(TestPostgresContainer.class, TestPostgresContainer::new);
        if (discovery) {
            service.discovery(TestConnectionDetails.class, container -> new TestConnectionDetails(container.host()));
        }
        return service;
    }

}
