package io.arconia.ai.core.tools.method;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import io.arconia.ai.core.tools.ToolCallback;
import io.arconia.ai.core.tools.ToolCallbackProvider;
import io.arconia.ai.core.tools.annotation.Tool;
import io.arconia.ai.core.tools.metadata.ToolMetadata;
import io.arconia.ai.core.tools.util.ToolUtils;

/**
 * A {@link ToolCallbackProvider} that builds {@link ToolCallback} instances from
 * {@link Tool}-annotated methods.
 */
public class MethodToolCallbackProvider implements ToolCallbackProvider {

    private static final Logger logger = LoggerFactory.getLogger(MethodToolCallbackProvider.class);

    private final List<Object> toolObjects;

    private MethodToolCallbackProvider(List<Object> toolObjects) {
        Assert.notNull(toolObjects, "toolObjects cannot be null");
        Assert.noNullElements(toolObjects, "toolObjects cannot contain null elements");
        this.toolObjects = toolObjects;
    }

    @Override
    public ToolCallback[] getToolCallbacks() {
        var toolCallbacks = toolObjects.stream()
            .map(toolObject -> Stream.of(ReflectionUtils.getDeclaredMethods(toolObject.getClass()))
                .filter(toolMethod -> toolMethod.isAnnotationPresent(Tool.class))
                .filter(toolMethod -> !isFunctionalType(toolMethod))
                .map(toolMethod -> MethodToolCallback.builder()
                    .toolMetadata(ToolMetadata.from(toolMethod))
                    .toolMethod(toolMethod)
                    .toolObject(toolObject)
                    .build())
                .toArray(ToolCallback[]::new))
            .flatMap(Stream::of)
            .toArray(ToolCallback[]::new);

        validateToolCallbacks(toolCallbacks);

        return toolCallbacks;
    }

    private static boolean isFunctionalType(Method toolMethod) {
        var isFunction = ClassUtils.isAssignable(toolMethod.getReturnType(), Function.class)
                || ClassUtils.isAssignable(toolMethod.getReturnType(), Supplier.class)
                || ClassUtils.isAssignable(toolMethod.getReturnType(), Consumer.class);

        if (isFunction) {
            logger.warn("Method {} is annotated with @Tool but returns a functional type. "
                    + "This is not supported and the method will be ignored.", toolMethod.getName());
        }

        return isFunction;
    }

    private void validateToolCallbacks(ToolCallback[] toolCallbacks) {
        List<String> duplicateToolNames = ToolUtils.getDuplicateToolNames(toolCallbacks);
        if (!duplicateToolNames.isEmpty()) {
            throw new IllegalStateException("Multiple tools with the same name (%s) found in sources: %s".formatted(
                    String.join(", ", duplicateToolNames),
                    toolObjects.stream().map(o -> o.getClass().getName()).collect(Collectors.joining(", "))));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private List<Object> toolObjects;

        public Builder toolObjects(Object... toolObjects) {
            Assert.notNull(toolObjects, "toolObjects cannot be null");
            this.toolObjects = Arrays.asList(toolObjects);
            return this;
        }

        public MethodToolCallbackProvider build() {
            return new MethodToolCallbackProvider(toolObjects);
        }

    }

}
