package io.arconia.dev.services.core.registration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.arconia.dev.services.api.config.BaseDevServicesProperties;
import io.arconia.dev.services.api.config.ReuseStrategy;
import io.arconia.dev.services.core.TestDevServicesProperties;
import io.arconia.dev.services.core.registration.DevServicesTestSupport.TestConnectionDetails;
import io.arconia.dev.services.core.registration.DevServicesTestSupport.TestPostgresContainer;

import static io.arconia.dev.services.core.registration.DevServicesTestSupport.enableDevMode;
import static io.arconia.dev.services.core.registration.DevServicesTestSupport.resetBootstrapMode;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ReuseDecision}.
 */
class ReuseDecisionTests {

    @BeforeEach
    @AfterEach
    void reset() {
        resetBootstrapMode();
    }

    @Test
    void whenNoneStrategyThenNoReuseStrategyApplies() {
        enableDevMode();

        var decision = ReuseDecision.decide(serviceSpec(TestDevServicesProperties.DEFAULT, true), () -> true);

        assertThat(decision.effective()).isEqualTo(ReuseStrategy.NONE);
        assertThat(decision.reason()).isNull();
    }

    @Test
    void whenReuseStrategyCanBeAppliedThenItIsInEffect() {
        enableDevMode();

        var decision = ReuseDecision.decide(serviceSpec(TestDevServicesProperties.FRAMEWORK, true), () -> false);

        assertThat(decision.effective()).isEqualTo(ReuseStrategy.FRAMEWORK);
        assertThat(decision.reason()).isNull();
    }

    @Test
    void whenNotInDevModeThenNoReuseStrategyApplies() {
        var decision = ReuseDecision.decide(serviceSpec(TestDevServicesProperties.FRAMEWORK, true), () -> true);

        assertThat(decision.effective()).isEqualTo(ReuseStrategy.NONE);
        assertThat(decision.reason()).isNull();
    }

    @Test
    void whenFrameworkStrategyWithoutDiscoveryThenReuseStrategyIsNotApplied() {
        enableDevMode();

        var decision = ReuseDecision.decide(serviceSpec(TestDevServicesProperties.FRAMEWORK, false), () -> false);

        assertThat(decision.effective()).isEqualTo(ReuseStrategy.NONE);
        assertThat(decision.reason()).contains("not supported");
    }

    @Test
    void whenTestcontainersReuseIsNotEnabledThenReuseStrategyIsNotApplied() {
        enableDevMode();

        var decision = ReuseDecision.decide(serviceSpec(TestDevServicesProperties.TESTCONTAINERS, false), () -> false);

        assertThat(decision.effective()).isEqualTo(ReuseStrategy.NONE);
        assertThat(decision.reason()).contains("testcontainers.reuse.enable").contains("TESTCONTAINERS_REUSE_ENABLE");
    }

    @Test
    void whenTestcontainersReuseIsEnabledThenReuseStrategyIsInEffect() {
        enableDevMode();

        var decision = ReuseDecision.decide(serviceSpec(TestDevServicesProperties.TESTCONTAINERS, false), () -> true);

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
