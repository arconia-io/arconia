package io.arconia.ai.core.tools.util;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.util.ParsingUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import io.arconia.ai.core.tools.annotation.Tool;

/**
 * Miscellaneous tool utility methods. Mainly for internal use within the framework.
 */
public final class ToolUtils {

    public static String getToolName(@Nullable Tool tool, String methodName) {
        if (tool == null) {
            return methodName;
        }
        return StringUtils.hasText(tool.name()) ? tool.name() : methodName;
    }

    public static String getToolDescription(@Nullable Tool tool, String methodName) {
        if (tool == null) {
            return ParsingUtils.reConcatenateCamelCase(methodName, " ");
        }
        return StringUtils.hasText(tool.value()) ? tool.value() : methodName;
    }

    public static FunctionCallback.SchemaType getToolSchemaType(@Nullable Tool tool) {
        if (tool == null) {
            return FunctionCallback.SchemaType.JSON_SCHEMA;
        }
        return tool.schemaType();
    }

    public static List<String> getDuplicateToolNames(FunctionCallback... functionCallbacks) {
        return Stream.of(functionCallbacks)
            .collect(Collectors.groupingBy(FunctionCallback::getName, Collectors.counting()))
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue() > 1)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

}
