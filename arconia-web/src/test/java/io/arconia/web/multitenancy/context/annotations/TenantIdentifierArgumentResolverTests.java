package io.arconia.web.multitenancy.context.annotations;

import java.lang.reflect.Method;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.util.ReflectionUtils;

import io.arconia.core.multitenancy.context.TenantContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TenantIdentifierArgumentResolver}.
 */
class TenantIdentifierArgumentResolverTests {

    private final TenantIdentifierArgumentResolver argumentResolver = new TenantIdentifierArgumentResolver();

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
    }

    @Test
    void doesNotSupportParameterWithoutAnnotation() {
        assertThat(argumentResolver.supportsParameter(showTenantIdentifierNoAnnotation())).isFalse();
    }

    @Test
    void supportsParameterWithAnnotation() {
        assertThat(argumentResolver.supportsParameter(showTenantIdentifierAnnotation())).isTrue();
    }

    @Test
    void doesNotSupportParameterWithWrongType() {
        assertThat(argumentResolver.supportsParameter(showTenantIdentifierErrorOnInvalidType())).isFalse();
    }

    @Test
    void resolveTenantIdentifierArgument() {
        String expectedTenantIdentifier = "acme";
        TenantContextHolder.setTenantIdentifier(expectedTenantIdentifier);
        String actualTenantIdentifier = (String) argumentResolver.resolveArgument(showTenantIdentifierAnnotation(),
                null, null, null);
        assertThat(actualTenantIdentifier).isEqualTo(expectedTenantIdentifier);
    }

    private MethodParameter showTenantIdentifierNoAnnotation() {
        return getMethodParameter("showTenantIdentifierNoAnnotation", String.class);
    }

    private MethodParameter showTenantIdentifierAnnotation() {
        return getMethodParameter("showTenantIdentifierAnnotation", String.class);
    }

    private MethodParameter showTenantIdentifierErrorOnInvalidType() {
        return getMethodParameter("showTenantIdentifierErrorOnInvalidType", Long.class);
    }

    private MethodParameter getMethodParameter(String methodName, Class<?>... paramTypes) {
        Method method = ReflectionUtils.findMethod(TestController.class, methodName, paramTypes);
        return new MethodParameter(method, 0);
    }

    static class TestController {

        public void showTenantIdentifierNoAnnotation(String tenantIdentifier) {
        }

        public void showTenantIdentifierAnnotation(@TenantIdentifier String tenantIdentifier) {
        }

        public void showTenantIdentifierErrorOnInvalidType(@TenantIdentifier Long tenantIdentifier) {
        }

    }

}
