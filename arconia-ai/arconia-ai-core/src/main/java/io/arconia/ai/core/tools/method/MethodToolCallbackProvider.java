package io.arconia.ai.core.tools.method;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import io.arconia.ai.core.tools.Tool;
import io.arconia.ai.core.tools.ToolCallback;
import io.arconia.ai.core.tools.ToolCallbackProvider;
import io.arconia.ai.core.tools.ToolUtils;

/**
 * A {@link ToolCallbackProvider} that builds {@link ToolCallback} instances from
 * {@link Tool}-annotated methods.
 */
public class MethodToolCallbackProvider implements ToolCallbackProvider {

    private static final Logger logger = LoggerFactory.getLogger(MethodToolCallbackProvider.class);

    @Nullable
    private final List<Object> sourceObjects;

    @Nullable
    private final List<Class<?>> sourceTypes;

    private MethodToolCallbackProvider(@Nullable List<Object> sourceObjects, @Nullable List<Class<?>> sourceTypes) {
        Assert.isTrue(sourceObjects != null || sourceTypes != null, "sourceObjects or sourceTypes cannot be null");
        if (sourceObjects != null) {
            Assert.noNullElements(sourceObjects, "sourceObjects cannot contain null elements");
        }
        if (sourceTypes != null) {
            Assert.noNullElements(sourceTypes, "sourceTypes cannot contain null elements");
        }
        this.sourceObjects = sourceObjects;
        this.sourceTypes = sourceTypes;
    }

    @Override
    public FunctionCallback[] getToolCallbacks() {
        if (sourceObjects != null) {
            return getToolCallbacksFromObjects();
        }
        return getToolCallbacksFromTypes();
    }

    private FunctionCallback[] getToolCallbacksFromObjects() {
        var toolCallbacks = sourceObjects.stream()
            .map(sourceObject -> getDeclaredMethodsWithToolAnnotation(sourceObject.getClass())
                .map(method -> ToolCallback.builder().method(method).source(sourceObject).build())
                .toArray(FunctionCallback[]::new))
            .flatMap(Stream::of)
            .toArray(FunctionCallback[]::new);

        if (ToolUtils.hasDuplicateToolNames(toolCallbacks)) {
            throw new IllegalStateException("Multiple tools with the same name found in sources: "
                    + sourceObjects.stream().map(o -> o.getClass().getName()).collect(Collectors.joining(", ")));
        }

        return toolCallbacks;
    }

    private FunctionCallback[] getToolCallbacksFromTypes() {
        var toolCallbacks = sourceTypes.stream()
            .map(sourceType -> getDeclaredMethodsWithToolAnnotation(sourceType)
                .filter(method -> Modifier.isStatic(method.getModifiers()))
                .map(method -> ToolCallback.builder().method(method).build())
                .toArray(FunctionCallback[]::new))
            .flatMap(Stream::of)
            .toArray(FunctionCallback[]::new);

        if (ToolUtils.hasDuplicateToolNames(toolCallbacks)) {
            throw new IllegalStateException("Multiple tools with the same name found in sources: "
                    + sourceTypes.stream().map(Class::getName).collect(Collectors.joining(", ")));
        }

        return toolCallbacks;
    }

    private Stream<Method> getDeclaredMethodsWithToolAnnotation(Class<?> sourceType) {
        return Stream.of(ReflectionUtils.getDeclaredMethods(sourceType))
            .filter(method -> method.isAnnotationPresent(Tool.class))
            .filter(method -> !isFunctionalType(method));
    }

    private static boolean isFunctionalType(Method method) {
        var isFunction = ClassUtils.isAssignable(method.getReturnType(), Function.class)
                || ClassUtils.isAssignable(method.getReturnType(), Supplier.class)
                || ClassUtils.isAssignable(method.getReturnType(), Consumer.class);

        if (isFunction) {
            logger.warn("Method {} is annotated with @Tool but returns a functional type. "
                    + "This is not supported and the method will be ignored.", method.getName());
        }

        return isFunction;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private List<Object> sourceObjects;

        private List<Class<?>> sourceTypes;

        public Builder sources(Object... sourceObjects) {
            Assert.isNull(this.sourceTypes, "only one of sourceObjects or sourceTypes can be set");
            this.sourceObjects = Arrays.asList(sourceObjects);
            return this;
        }

        public Builder sources(Class<?>... sourceTypes) {
            Assert.isNull(this.sourceObjects, "only one of sourceObjects or sourceTypes can be set");
            this.sourceTypes = Arrays.asList(sourceTypes);
            return this;
        }

        public MethodToolCallbackProvider build() {
            return new MethodToolCallbackProvider(sourceObjects, sourceTypes);
        }

    }

}
