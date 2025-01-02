package io.arconia.ai.core.tools.aot;

import com.fasterxml.jackson.module.jsonSchema.JsonSchemaIdResolver;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * Registers runtime hints for Jackson JSON Schema classes.
 */
public class JacksonJsonSchemaRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(@NonNull RuntimeHints hints, @Nullable ClassLoader classLoader) {
        var mcs = MemberCategory.values();
        hints.reflection().registerType(JsonSchemaIdResolver.class, mcs);
    }

}
