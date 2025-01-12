package io.arconia.ai.tools.aot;

import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;

import io.arconia.ai.tools.execution.DefaultToolCallResultConverter;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.aot.hint.predicate.RuntimeHintsPredicates.reflection;

/**
 * Unit tests for {@link ToolRuntimeHints}.
 */
class ToolRuntimeHintsTests {

    @Test
    void registerHints() {
        RuntimeHints runtimeHints = new RuntimeHints();
        ToolRuntimeHints toolRuntimeHints = new ToolRuntimeHints();
        toolRuntimeHints.registerHints(runtimeHints, null);
        assertThat(runtimeHints).matches(reflection().onType(DefaultToolCallResultConverter.class));
    }

}
