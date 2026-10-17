package io.arconia.multitenancy.web.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.arconia.core.support.Incubating;

/**
 * Annotation that is used to resolve the current tenant identifier as a method argument.
 */
@Incubating
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TenantIdentifier {

}
