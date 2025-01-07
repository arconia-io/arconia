package io.arconia.ai.tools.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.arconia.ai.tools.execution.ToolExecutionMode;

/**
 * Marks a method as a tool in Spring AI.
 */
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Tool {

    /**
     * The name of the tool. If not provided, the method name will be used.
     */
    String name() default "";

    /**
     * The description of the tool. If not provided, the method name will be used.
     */
    String value() default "";

    /**
     * How the tool should be executed.
     */
    ToolExecutionMode executionMode() default ToolExecutionMode.BLOCKING;

    /**
     * Whether the tool result should be returned directly or passed back to the model.
     */
    boolean returnDirect() default false;

}
