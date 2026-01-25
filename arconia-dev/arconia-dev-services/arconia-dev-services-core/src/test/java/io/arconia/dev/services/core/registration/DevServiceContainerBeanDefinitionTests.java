package io.arconia.dev.services.core.registration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevServiceContainerBeanDefinition}.
 */
class DevServiceContainerBeanDefinitionTests {

    @Test
    void instantiate() {
        DevServiceContainerBeanDefinition definition = new DevServiceContainerBeanDefinition();
        assertThat(definition).isNotNull();
        assertThat(definition.getAnnotations()).isNotNull();
    }

}
