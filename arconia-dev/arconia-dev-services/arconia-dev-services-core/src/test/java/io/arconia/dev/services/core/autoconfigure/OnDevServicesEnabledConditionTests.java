package io.arconia.dev.services.core.autoconfigure;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.mock.env.MockEnvironment;

import io.arconia.boot.bootstrap.BootstrapMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OnDevServicesEnabledCondition}.
 */
class OnDevServicesEnabledConditionTests {

    private final OnDevServicesEnabledCondition condition = new OnDevServicesEnabledCondition();

    private final MockEnvironment environment = new MockEnvironment();

    private final ConditionContext context = mock(ConditionContext.class);

    @BeforeEach
    void setUp() {
        when(context.getEnvironment()).thenReturn(environment);
        BootstrapMode.clear();
    }

    @AfterEach
    void tearDown() {
        System.clearProperty(BootstrapMode.PROPERTY_KEY);
        BootstrapMode.clear();
    }

    /**
     * The decision table of the condition: the bootstrap mode, the global switch, the toggle
     * of the dev service (under the default or a custom prefix), and the outcome. A blank
     * value means the property is not set.
     */
    @ParameterizedTest(name = "mode={0} global={1} service={2} prefix={3} name={4} -> match={5}")
    @CsvSource(nullValues = "-", textBlock = """
            # mode, global, service, prefix,            name,         match, message
            prod,   true,   true,    -,                 test-service, false, dev services are only available in dev and test mode
            prod,   -,      -,       -,                 -,            false, dev services are only available in dev and test mode
            prod,   -,      -,       acme.dev.services, keycloak,     false, dev services are only available in dev and test mode
            test,   -,      -,       -,                 test-service, true,  enabled by default
            test,   -,      -,       -,                 -,            true,  no specific dev services name is specified
            test,   true,   -,       -,                 -,            true,  no specific dev services name is specified
            test,   false,  -,       -,                 -,            false, arconia.dev.services.enabled is set to false
            test,   true,   true,    -,                 test-service, true,  arconia.dev.services.test-service.enabled is set to true
            test,   true,   false,   -,                 test-service, false, arconia.dev.services.test-service.enabled is set to false
            test,   false,  true,    -,                 test-service, false, arconia.dev.services.enabled is set to false
            test,   false,  false,   -,                 test-service, false, arconia.dev.services.enabled is set to false
            test,   -,      true,    -,                 test-service, true,  arconia.dev.services.test-service.enabled is set to true
            test,   on,     -,       -,                 test-service, true,  enabled by default
            test,   -,      true,    acme.dev.services, keycloak,     true,  acme.dev.services.keycloak.enabled is set to true
            test,   -,      false,   acme.dev.services, keycloak,     false, acme.dev.services.keycloak.enabled is set to false
            test,   -,      -,       acme.dev.services, keycloak,     true,  enabled by default
            test,   false,  true,    acme.dev.services, keycloak,     false, arconia.dev.services.enabled is set to false
            """)
    void decidesFromModeAndProperties(String mode, @Nullable String global, @Nullable String service, @Nullable String prefix,
            @Nullable String name, boolean match, String message) {
        System.setProperty(BootstrapMode.PROPERTY_KEY, mode);
        BootstrapMode.clear();
        if (global != null) {
            environment.setProperty("arconia.dev.services.enabled", global);
        }
        if (service != null) {
            environment.setProperty("%s.%s.enabled".formatted(prefix != null ? prefix : "arconia.dev.services", name), service);
        }

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata(name, prefix));

        assertThat(outcome.isMatch()).isEqualTo(match);
        assertThat(outcome.getMessage()).contains(message);
    }

    @Test
    void failsWhenGlobalPropertyIsInvalid() {
        environment.setProperty("arconia.dev.services.enabled", "not-a-boolean");

        assertThatThrownBy(() -> condition.getMatchOutcome(context, metadata("test-service", null)))
                .isInstanceOf(BindException.class);
    }

    @Test
    void failsWhenServicePropertyIsInvalid() {
        environment.setProperty("acme.dev.services.keycloak.enabled", "not-a-boolean");

        assertThatThrownBy(() -> condition.getMatchOutcome(context, metadata("keycloak", "acme.dev.services")))
                .isInstanceOf(BindException.class);
    }

    @Test
    void failsWhenPrefixIsSpecifiedWithoutName() {
        assertThatThrownBy(() -> condition.getMatchOutcome(context, metadata(null, "acme.dev.services")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("requires a name when a custom prefix is specified");
    }

    @Test
    void matchesWhenAnnotationAttributesAreNotAvailable() {
        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        when(metadata.getAnnotationAttributes(ConditionalOnDevServicesEnabled.class.getName())).thenReturn(null);

        assertThat(condition.getMatchOutcome(context, metadata).isMatch()).isTrue();
    }

    private static AnnotatedTypeMetadata metadata(@Nullable String name, @Nullable String prefix) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", name != null ? name : "");
        attributes.put("name", name != null ? name : "");
        attributes.put("prefix", prefix != null ? prefix : "");
        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        when(metadata.getAnnotationAttributes(ConditionalOnDevServicesEnabled.class.getName())).thenReturn(attributes);
        return metadata;
    }

    /**
     * The condition through the real annotation, so that attribute aliasing and the prefix
     * are exercised as Spring resolves them.
     */
    @Nested
    class RealAnnotationMetadataTests {

        private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

        @Test
        void shouldResolveNameAttributeAsAliasForValue() {
            contextRunner
                    .withUserConfiguration(NamedDevServiceConfiguration.class)
                    .withPropertyValues("arconia.dev.services.test-service.enabled=false")
                    .run(context -> assertThat(context).doesNotHaveBean("namedDevServiceBean"));
        }

        @Test
        void shouldHonorCustomPrefixToggle() {
            contextRunner
                    .withUserConfiguration(CustomPrefixDevServiceConfiguration.class)
                    .withPropertyValues("acme.dev.services.keycloak.enabled=false")
                    .run(context -> assertThat(context).doesNotHaveBean("customPrefixDevServiceBean"));
        }

        @Test
        void shouldIgnoreBuiltInNamespaceToggleWhenCustomPrefixIsSpecified() {
            contextRunner
                    .withUserConfiguration(CustomPrefixDevServiceConfiguration.class)
                    .withPropertyValues("arconia.dev.services.keycloak.enabled=false")
                    .run(context -> assertThat(context).hasBean("customPrefixDevServiceBean"));
        }

    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnDevServicesEnabled(name = "test-service")
    static class NamedDevServiceConfiguration {

        @Bean
        String namedDevServiceBean() {
            return "namedDevServiceBean";
        }

    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnDevServicesEnabled(name = "keycloak", prefix = "acme.dev.services")
    static class CustomPrefixDevServiceConfiguration {

        @Bean
        String customPrefixDevServiceBean() {
            return "customPrefixDevServiceBean";
        }

    }

}
