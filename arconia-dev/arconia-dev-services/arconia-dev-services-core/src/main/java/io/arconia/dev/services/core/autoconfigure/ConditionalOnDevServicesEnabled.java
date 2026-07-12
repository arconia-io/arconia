package io.arconia.dev.services.core.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;
import org.springframework.core.annotation.AliasFor;

/**
 * Whether the specified dev services module should be enabled.
 * <p>
 * The condition matches only when the application is running in dev or test mode
 * and dev services are not globally disabled via {@code arconia.dev.services.enabled}.
 * When a dev services name is specified, the condition additionally honors the
 * {@code <prefix>.<name>.enabled} property, where the prefix defaults to
 * {@code arconia.dev.services}.
 * <p>
 * Custom dev services using their own configuration property namespace can specify
 * it via {@link #prefix()} so that their {@code <prefix>.<name>.enabled} toggle is
 * honored with the same semantics as the built-in dev services.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@Conditional(OnDevServicesEnabledCondition.class)
public @interface ConditionalOnDevServicesEnabled {

    /**
     * The logical name of the dev services module to enable. When empty, only the global
     * enablement property and the bootstrap mode are checked.
     */
    @AliasFor("name")
    String value() default "";

    /**
     * The logical name of the dev services module to enable. When empty, only the global
     * enablement property and the bootstrap mode are checked.
     */
    @AliasFor("value")
    String name() default "";

    /**
     * The configuration property namespace the dev service's {@code enabled} toggle lives under,
     * forming the {@code <prefix>.<name>.enabled} property together with the name.
     * When empty, defaults to {@code arconia.dev.services}. Requires a name to be specified.
     */
    String prefix() default "";

}
