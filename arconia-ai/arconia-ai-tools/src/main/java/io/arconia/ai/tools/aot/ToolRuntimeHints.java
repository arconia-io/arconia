package io.arconia.ai.tools.aot;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import io.arconia.ai.tools.execution.DefaultToolCallResultConverter;

/**
 * Registers runtime hints for the tool calling APIs.
 */
public class ToolRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(@NonNull RuntimeHints hints, @Nullable ClassLoader classLoader) {
		var mcs = MemberCategory.values();
		hints.reflection().registerType(DefaultToolCallResultConverter.class, mcs);
	}

}
