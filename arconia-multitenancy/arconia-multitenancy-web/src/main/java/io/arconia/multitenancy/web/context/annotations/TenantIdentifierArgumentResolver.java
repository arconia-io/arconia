package io.arconia.multitenancy.web.context.annotations;

import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import io.arconia.core.support.Incubating;
import io.arconia.multitenancy.core.context.TenantContext;

/**
 * Allows resolving the current tenant identifier using the {@link TenantIdentifier}
 * annotation.
 * <p>
 * Example:
 *
 * <pre>
 * &#64;RestController
 * class MyRestController {
 *     &#64;GetMapping("/tenant")
 *     String getCurrentTenant(@TenantIdentifier String tenantIdentifier) {
 *         return tenantIdentifier;
 *     }
 * }
 * </pre>
 */
@Incubating
public final class TenantIdentifierArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        if (parameter.getParameterAnnotation(TenantIdentifier.class) == null) {
            return false;
        }
        return parameter.getParameterType() == String.class || isOptionalString(parameter);
    }

    /**
     * Returns the tenant identifier bound to the current context, or {@code null} when no
     * tenant is bound, as happens on an ignored path. Declare the parameter
     * as {@code Optional<String>} to make the absence explicit.
     */
    @Nullable
    @Override
    public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) {
        String tenantIdentifier = TenantContext.getTenantIdentifier();
        return isOptionalString(parameter) ? Optional.ofNullable(tenantIdentifier) : tenantIdentifier;
    }

    private static boolean isOptionalString(MethodParameter parameter) {
        return parameter.getParameterType() == Optional.class
                && ResolvableType.forMethodParameter(parameter).getGeneric(0).resolve() == String.class;
    }

}
