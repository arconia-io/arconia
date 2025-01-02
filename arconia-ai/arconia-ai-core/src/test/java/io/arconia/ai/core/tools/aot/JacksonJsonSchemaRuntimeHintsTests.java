package io.arconia.ai.core.tools.aot;

import com.fasterxml.jackson.module.jsonSchema.JsonSchemaIdResolver;

import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.aot.hint.predicate.RuntimeHintsPredicates.reflection;

/**
 * Unit tests for {@link JacksonJsonSchemaRuntimeHints}.
 */
class JacksonJsonSchemaRuntimeHintsTests {

    @Test
    void registerHints() {
        RuntimeHints runtimeHints = new RuntimeHints();
        JacksonJsonSchemaRuntimeHints jacksonJsonSchemaRuntimeHints = new JacksonJsonSchemaRuntimeHints();
        jacksonJsonSchemaRuntimeHints.registerHints(runtimeHints, null);

        assertThat(runtimeHints).matches(reflection().onType(JsonSchemaIdResolver.class));
    }

}
