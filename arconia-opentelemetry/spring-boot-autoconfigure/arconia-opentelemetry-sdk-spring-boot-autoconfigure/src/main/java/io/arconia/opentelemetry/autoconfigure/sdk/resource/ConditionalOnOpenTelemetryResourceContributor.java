package io.arconia.opentelemetry.autoconfigure.sdk.resource;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;

/**
 * Indicates when the given resource contributor is eligible for registration.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@Conditional(OnOpenTelemetryResourceContributorCondition.class)
public @interface ConditionalOnOpenTelemetryResourceContributor {

    /**
     * The name of the resource contributor.
     */
	String value();

    /**
     * If the condition should match if no property is defined for the resource contributor.
     */
    boolean matchIfMissing() default false;

}
