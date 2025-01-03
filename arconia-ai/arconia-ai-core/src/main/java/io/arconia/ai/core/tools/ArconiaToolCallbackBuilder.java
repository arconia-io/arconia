package io.arconia.ai.core.tools;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.model.function.DefaultCommonCallbackInvokingSpec;
import org.springframework.ai.model.function.DefaultFunctionCallbackBuilder;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.FunctionInvokingFunctionCallback;
import org.springframework.ai.model.function.MethodInvokingFunctionCallback;
import org.springframework.ai.util.ParsingUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Builds a {@link ToolCallback} instance. Implementation based on
 * {@link DefaultFunctionCallbackBuilder}.
 */
public class ArconiaToolCallbackBuilder implements FunctionCallback.Builder {

    private final static Logger logger = LoggerFactory.getLogger(ArconiaToolCallbackBuilder.class);

    @Override
    public <I, O> ArconiaFunctionInvokingSpec<I, O> function(String name, Function<I, O> function) {
        return new ArconiaFunctionInvokingSpec<>(name, function);
    }

    @Override
    public <I, O> ArconiaFunctionInvokingSpec<I, O> function(String name, BiFunction<I, ToolContext, O> biFunction) {
        return new ArconiaFunctionInvokingSpec<>(name, biFunction);
    }

    @Override
    public <O> ArconiaFunctionInvokingSpec<Void, O> function(String name, Supplier<O> supplier) {
        Function<Void, O> function = input -> supplier.get();
        return new ArconiaFunctionInvokingSpec<>(name, function).inputType(Void.class);
    }

    @Override
    public <I> ArconiaFunctionInvokingSpec<I, Void> function(String name, Consumer<I> consumer) {
        Function<I, Void> function = (I input) -> {
            consumer.accept(input);
            return null;
        };
        return new ArconiaFunctionInvokingSpec<>(name, function);
    }

    @Override
    public ArconiaMethodInvokingSpec method(String methodName, Class<?>... argumentTypes) {
        throw new UnsupportedOperationException("Use the 'method(Method method)' method instead");
    }

    /**
     * Create a {@link FunctionCallback.MethodInvokingSpec} for the given method.
     */
    public ArconiaMethodInvokingSpec method(Method method) {
        return new ArconiaMethodInvokingSpec(method);
    }

    private String generateDescription(String fromName) {
        String generatedDescription = ParsingUtils.reConcatenateCamelCase(fromName, " ");

        logger.info("Description is not set! A best effort attempt to generate a description:'{}' from the:'{}'",
                generatedDescription, fromName);
        logger.info("It is recommended to set the Description explicitly! Use the 'description()' method!");

        return generatedDescription;
    }

    /**
     * Arconia {@link FunctionCallback.FunctionInvokingSpec} implementation.
     */
    public final class ArconiaFunctionInvokingSpec<I, O>
            extends DefaultCommonCallbackInvokingSpec<FunctionCallback.FunctionInvokingSpec<I, O>>
            implements FunctionCallback.FunctionInvokingSpec<I, O> {

        private final String name;

        @Nullable
        private final BiFunction<I, ToolContext, O> biFunction;

        @Nullable
        private final Function<I, O> function;

        private Type inputType;

        private ArconiaFunctionInvokingSpec(String name, @NonNull BiFunction<I, ToolContext, O> biFunction) {
            Assert.hasText(name, "name cannot be null or empty");
            Assert.notNull(biFunction, "biFunction cannot be null");
            this.name = name;
            this.biFunction = biFunction;
            this.function = null;
        }

        private ArconiaFunctionInvokingSpec(String name, @NonNull Function<I, O> function) {
            Assert.hasText(name, "name cannot be null or empty");
            Assert.notNull(function, "function cannot be null");
            this.name = name;
            this.biFunction = null;
            this.function = function;
        }

        @Override
        public ArconiaFunctionInvokingSpec<I, O> inputType(Class<?> inputType) {
            Assert.notNull(inputType, "inputType cannot be null");
            this.inputType = inputType;
            return this;
        }

        @Override
        public ArconiaFunctionInvokingSpec<I, O> inputType(ParameterizedTypeReference<?> inputType) {
            Assert.notNull(inputType, "inputType cannot be null");
            this.inputType = inputType.getType();
            return this;
        }

        @Override
        public FunctionCallback build() {
            Assert.notNull(this.inputType, "inputType cannot be null");

            if (this.getInputTypeSchema() == null) {
                boolean upperCaseTypeValues = schemaType == FunctionCallback.SchemaType.OPEN_API_SCHEMA;
                this.inputTypeSchema = ModelOptionsUtils.getJsonSchema(this.inputType, upperCaseTypeValues);
            }

            BiFunction<I, ToolContext, O> finalBiFunction = (this.biFunction != null) ? this.biFunction
                    : (request, context) -> this.function.apply(request);

            try {
                var constructor = FunctionInvokingFunctionCallback.class.getDeclaredConstructor(String.class,
                        String.class, String.class, Type.class, Function.class, ObjectMapper.class, BiFunction.class);
                constructor.setAccessible(true);
                return constructor.newInstance(this.name, this.getToolDescription(), this.getInputTypeSchema(),
                        this.inputType, this.getResponseConverter(), this.getObjectMapper(), finalBiFunction);
            }
            catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("Failed to create FunctionInvokingFunctionCallback instance", ex);
            }
        }

        private String getToolDescription() {
            if (StringUtils.hasText(this.getDescription())) {
                return this.getDescription();
            }
            return ParsingUtils.reConcatenateCamelCase(this.name, " ");
        }

    }

    /**
     * Arconia {@link FunctionCallback.MethodInvokingSpec} implementation.
     */
    public static final class ArconiaMethodInvokingSpec
            extends DefaultCommonCallbackInvokingSpec<FunctionCallback.MethodInvokingSpec>
            implements FunctionCallback.MethodInvokingSpec {

        private final Method method;

        private String name;

        @Nullable
        private Object source;

        private ArconiaMethodInvokingSpec(Method method) {
            Assert.notNull(method, "method cannot be null");
            Assert.hasText(method.getName(), "method name cannot be null or empty");

            this.method = method;
            this.name = getToolName(method.getAnnotation(Tool.class), method.getName());
            this.description = getToolDescription(method.getAnnotation(Tool.class), method.getName());
            this.schemaType = getToolSchemaType(method.getAnnotation(Tool.class));
        }

        @Override
        public ArconiaMethodInvokingSpec name(String name) {
            Assert.hasText(name, "name cannot be null or empty");
            this.name = name;
            return this;
        }

        public ArconiaMethodInvokingSpec source(Object source) {
            Assert.notNull(source, "source cannot be null");
            this.source = source;
            return this;
        }

        @Override
        public ArconiaMethodInvokingSpec targetClass(Class<?> targetClass) {
            return this;
        }

        @Override
        public ArconiaMethodInvokingSpec targetObject(Object targetObject) {
            return source(targetObject);
        }

        @Override
        public FunctionCallback build() {
            try {
                var constructor = MethodInvokingFunctionCallback.class.getDeclaredConstructor(Object.class,
                        Method.class, String.class, ObjectMapper.class, String.class, Function.class);
                constructor.setAccessible(true);
                return constructor.newInstance(this.source, method, this.getDescription(), this.getObjectMapper(),
                        this.name, this.getResponseConverter());
            }
            catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("Failed to create MethodInvokingFunctionCallback instance", ex);
            }
        }

        private static String getToolName(@Nullable Tool tool, String methodName) {
            if (tool == null) {
                return methodName;
            }
            return StringUtils.hasText(tool.name()) ? tool.name() : methodName;
        }

        private static String getToolDescription(@Nullable Tool tool, String methodName) {
            if (tool == null) {
                return ParsingUtils.reConcatenateCamelCase(methodName, " ");
            }
            return StringUtils.hasText(tool.value()) ? tool.value() : methodName;
        }

        private static FunctionCallback.SchemaType getToolSchemaType(@Nullable Tool tool) {
            if (tool == null) {
                return FunctionCallback.SchemaType.JSON_SCHEMA;
            }
            return tool.schemaType();
        }

    }

}
