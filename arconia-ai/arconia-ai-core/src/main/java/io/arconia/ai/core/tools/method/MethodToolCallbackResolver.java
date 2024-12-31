package io.arconia.ai.core.tools.method;

import java.util.stream.Stream;

import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import io.arconia.ai.core.tools.Tool;
import io.arconia.ai.core.tools.ToolCallback;
import io.arconia.ai.core.tools.ToolCallbackResolver;

/**
 * A {@link ToolCallbackResolver} that resolves {@link ToolCallback} instances from
 * methods annotated with {@link Tool}.
 */
public class MethodToolCallbackResolver implements ToolCallbackResolver {

    private final Object target;

    private MethodToolCallbackResolver(Object target) {
        Assert.notNull(target, "target cannot be null");
        this.target = target;
    }

    @Override
    public FunctionCallback[] getToolCallbacks() {
        return Stream.of(ReflectionUtils.getDeclaredMethods(target.getClass()))
            .filter(method -> method.isAnnotationPresent(Tool.class))
            .map(method -> FunctionCallback.builder()
                .method(method.getName(), method.getParameterTypes())
                .name(getToolName(method.getAnnotation(Tool.class), method.getName()))
                .description(getToolDescription(method.getAnnotation(Tool.class), method.getName()))
                .schemaType(method.getAnnotation(Tool.class).schemaType())
                .targetObject(target)
                .build())
            .toArray(FunctionCallback[]::new);
    }

    private static String getToolName(Tool tool, String methodName) {
        return StringUtils.hasText(tool.name()) ? tool.name() : methodName;
    }

    private static String getToolDescription(Tool tool, String methodName) {
        return StringUtils.hasText(tool.value()) ? tool.value() : methodName;
    }

    public static class Builder {

        private Object target;

        public Builder target(Object target) {
            this.target = target;
            return this;
        }

        public MethodToolCallbackResolver build() {
            return new MethodToolCallbackResolver(target);
        }

    }

    public static Builder builder() {
        return new Builder();
    }

}
