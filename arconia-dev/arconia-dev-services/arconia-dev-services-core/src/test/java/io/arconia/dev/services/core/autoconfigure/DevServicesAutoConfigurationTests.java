package io.arconia.dev.services.core.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevServicesAutoConfiguration}.
 */
class DevServicesAutoConfigurationTests {

    private static final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DevServicesAutoConfiguration.class));

    @Test
    void propertiesBeanIsAvailable() {
        contextRunner
                .run(context -> {
                    assertThat(context).hasSingleBean(DevServicesProperties.class);
                });
    }

}

