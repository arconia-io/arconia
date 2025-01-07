package io.arconia.ai.tools.util;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.util.ParsingUtils;
import org.springframework.util.StringUtils;

import io.arconia.ai.tools.annotation.Tool;
import io.arconia.ai.tools.execution.ToolExecutionMode;

/**
 * Miscellaneous tool utility methods. Mainly for internal use within the framework.
 */
public final class ToolUtils {

    public static String getToolName(Method method) {
        var tool = method.getAnnotation(Tool.class);
        if (tool == null) {
            return method.getName();
        }
        return StringUtils.hasText(tool.name()) ? tool.name() : method.getName();
    }

    public static String getToolDescription(Method method) {
        var tool = method.getAnnotation(Tool.class);
        if (tool == null) {
            return ParsingUtils.reConcatenateCamelCase(method.getName(), " ");
        }
        return StringUtils.hasText(tool.value()) ? tool.value() : method.getName();
    }

    public static ToolExecutionMode getToolExecutionMode(Method method) {
        var tool = method.getAnnotation(Tool.class);
        return tool != null ? tool.executionMode() : ToolExecutionMode.BLOCKING;
    }

    public static boolean getToolReturnDirect(Method method) {
        var tool = method.getAnnotation(Tool.class);
        return tool != null && tool.returnDirect();
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
