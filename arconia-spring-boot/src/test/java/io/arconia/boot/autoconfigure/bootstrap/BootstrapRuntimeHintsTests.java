package io.arconia.boot.autoconfigure.bootstrap;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.aot.hint.ResourcePatternHint;
import org.springframework.aot.hint.ResourcePatternHints;
import org.springframework.aot.hint.RuntimeHints;

import io.arconia.boot.bootstrap.BootstrapConfigurationFile;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link BootstrapRuntimeHints}.
 */
@DisabledInNativeImage
class BootstrapRuntimeHintsTests {

    @Test
    void shouldRegisterResources() {
        RuntimeHints hints = register();
        assertThat(hints.resources().resourcePatternHints()).hasSize(1)
                .anySatisfy(include(BootstrapConfigurationFile.LOCATION));
    }

    private RuntimeHints register() {
        RuntimeHints hints = new RuntimeHints();
        new BootstrapRuntimeHints().registerHints(hints, getClass().getClassLoader());
        return hints;
    }

    private Consumer<ResourcePatternHints> include(String... patterns) {
        return (hint) -> assertThat(hint.getIncludes()).map(ResourcePatternHint::getPattern).contains(patterns);
    }

}
