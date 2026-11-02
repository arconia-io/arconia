package io.arconia.multitenancy.details.jdbc.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.annotation.Qualifier;

import io.arconia.core.annotations.Incubating;

/**
 * Qualifier annotation for a {@link javax.sql.DataSource} to be used for the tenant
 * details instead of the application's main one. When no data source carries it, the
 * single data source of the application is used.
 */
@Incubating
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Qualifier
public @interface TenantDetailsDataSource {

}
