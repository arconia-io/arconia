package io.arconia.multitenancy.web.context.annotations;

import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
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
        return parameter.getParameterAnnotation(TenantIdentifier.class) != null
                && parameter.getParameterType().getTypeName().equals(String.class.getTypeName());
    }

    @Nullable
    @Override
    public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) {
        return TenantContext.getTenantIdentifier();
    }

}
