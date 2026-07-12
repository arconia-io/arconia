package io.arconia.dev.services.core.autoconfigure;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
        BootstrapMode.clear();
    }

    @AfterEach
    void tearDown() {
        System.clearProperty(BootstrapMode.PROPERTY_KEY);
        BootstrapMode.clear();
    }

    @Test
    void shouldNotMatchWhenApplicationIsRunningInProdMode() {
        System.setProperty(BootstrapMode.PROPERTY_KEY, "prod");
        BootstrapMode.clear();
        environment.setProperty("arconia.dev.services.enabled", "true");
        environment.setProperty("arconia.dev.services.test-service.enabled", "true");
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", "test-service");
        when(metadata.getAnnotationAttributes(ConditionalOnDevServicesEnabled.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage())
                .contains("dev services are only available in dev and test mode");
    }

    @Test
    void shouldMatchWhenGlobalPropertyUsesAlternativeBooleanFormat() {
        environment.setProperty("arconia.dev.services.enabled", "on");
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", "test-service");
        when(metadata.getAnnotationAttributes(ConditionalOnDevServicesEnabled.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isTrue();
    }

    @Test
    void shouldFailWhenGlobalPropertyValueIsInvalid() {
        environment.setProperty("arconia.dev.services.enabled", "not a boolean");
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", "test-service");
        when(metadata.getAnnotationAttributes(ConditionalOnDevServicesEnabled.class.getName()))
                .thenReturn(attributes);

        assertThatThrownBy(() -> condition.getMatchOutcome(context, metadata))
                .isInstanceOf(BindException.class);
    }

    @Test
    void shouldFailWhenSpecificPropertyValueIsInvalid() {
        environment.setProperty("arconia.dev.services.test-service.enabled", "yes please");
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", "test-service");
        when(metadata.getAnnotationAttributes(ConditionalOnDevServicesEnabled.class.getName()))
                .thenReturn(attributes);

        assertThatThrownBy(() -> condition.getMatchOutcome(context, metadata))
                .isInstanceOf(BindException.class);
    }

    @Test
    void shouldMatchWhenGloballyEnabledAndSpecificDevServiceEnabled() {
        environment.setProperty("arconia.dev.services.enabled", "true");
        environment.setProperty("arconia.dev.services.test-service.enabled", "true");
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", "test-service");
        when(metadata.getAnnotationAttributes(ConditionalOnDevServicesEnabled.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage())
                .contains("arconia.dev.services.test-service.enabled is set to true");
    }

    @Test
    void shouldNotMatchWhenGloballyEnabledButSpecificDevServiceDisabled() {
        environment.setProperty("arconia.dev.services.enabled", "true");
        environment.setProperty("arconia.dev.services.test-service.enabled", "false");
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", "test-service");
        when(metadata.getAnnotationAttributes(ConditionalOnDevServicesEnabled.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage())
                .contains("arconia.dev.services.test-service.enabled is set to false");
    }

    @Test
    void shouldNotMatchWhenGloballyDisabledAndSpecificDevServiceEnabled() {
        environment.setProperty("arconia.dev.services.enabled", "false");
        environment.setProperty("arconia.dev.services.test-service.enabled", "true");
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", "test-service");
        when(metadata.getAnnotationAttributes(ConditionalOnDevServicesEnabled.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage())
                .contains("arconia.dev.services.enabled is set to false");
    }

    @Test
    void shouldNotMatchWhenGloballyDisabledAndSpecificDevServiceDisabled() {
        environment.setProperty("arconia.dev.services.enabled", "false");
        environment.setProperty("arconia.dev.services.test-service.enabled", "false");
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", "test-service");
        when(metadata.getAnnotationAttributes(ConditionalOnDevServicesEnabled.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage())
                .contains("arconia.dev.services.enabled is set to false");
    }

    @Test
    void shouldMatchByDefaultWhenPropertiesAreNotSet() {
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", "test-service");
        when(metadata.getAnnotationAttributes(ConditionalOnDevServicesEnabled.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage())
                .contains("enabled by default (arconia.dev.services.test-service.enabled is not set)");
    }

    @Test
    void shouldMatchWhenDevServicesNameIsEmptyAndGloballyEnabled() {
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", "");
        when(metadata.getAnnotationAttributes(ConditionalOnDevServicesEnabled.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage())
                .contains("dev services are globally enabled and no specific dev services name is specified");
    }

    @Test
    void shouldNotMatchWhenDevServicesNameIsEmptyAndGloballyDisabled() {
        environment.setProperty("arconia.dev.services.enabled", "false");
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", "");
        when(metadata.getAnnotationAttributes(ConditionalOnDevServicesEnabled.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage())
                .contains("arconia.dev.services.enabled is set to false");
    }

    @Test
    void shouldMatchWhenDevServicesNameIsBlankAndGloballyEnabled() {
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", "   ");
        when(metadata.getAnnotationAttributes(ConditionalOnDevServicesEnabled.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage())
                .contains("dev services are globally enabled and no specific dev services name is specified");
    }

    @Test
    void shouldMatchWhenAnnotationAttributesAreNullAndGloballyEnabled() {
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        when(metadata.getAnnotationAttributes(ConditionalOnDevServicesEnabled.class.getName()))
                .thenReturn(null);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage())
                .contains("dev services are globally enabled and no specific dev services name is specified");
    }

    @Test
    void shouldNotMatchWhenDevServicesNameIsEmptyAndProdMode() {
        System.setProperty(BootstrapMode.PROPERTY_KEY, "prod");
        BootstrapMode.clear();
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", "");
        when(metadata.getAnnotationAttributes(ConditionalOnDevServicesEnabled.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage())
                .contains("dev services are only available in dev and test mode");
    }

    @Test
    void shouldMatchWhenOnlyGlobalPropertyIsSetToTrue() {
        environment.setProperty("arconia.dev.services.enabled", "true");
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", "test-service");
        when(metadata.getAnnotationAttributes(ConditionalOnDevServicesEnabled.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage())
                .contains("enabled by default (arconia.dev.services.test-service.enabled is not set)");
    }

    @Test
    void shouldMatchWhenOnlyGlobalPropertyIsSetToFalse() {
        environment.setProperty("arconia.dev.services.enabled", "false");
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", "test-service");
        when(metadata.getAnnotationAttributes(ConditionalOnDevServicesEnabled.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage())
                .contains("arconia.dev.services.enabled is set to false");
    }

    @Test
    void shouldMatchWhenOnlySpecificPropertyIsSetToTrue() {
        environment.setProperty("arconia.dev.services.test-service.enabled", "true");
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("value", "test-service");
        when(metadata.getAnnotationAttributes(ConditionalOnDevServicesEnabled.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage())
                .contains("arconia.dev.services.test-service.enabled is set to true");
    }

    @Test
    void shouldResolveDevServicesNameFromNameAttribute() {
        environment.setProperty("arconia.dev.services.test-service.enabled", "false");
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("name", "test-service");
        when(metadata.getAnnotationAttributes(ConditionalOnDevServicesEnabled.class.getName()))
                .thenReturn(attributes);

        ConditionOutcome outcome = condition.getMatchOutcome(context, metadata);

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage())
                .contains("arconia.dev.services.test-service.enabled is set to false");
    }

    @Test
    void shouldMatchWhenCustomPrefixDevServiceEnabled() {
        environment.setProperty("acme.dev.services.keycloak.enabled", "true");
        when(context.getEnvironment()).thenReturn(environment);

        ConditionOutcome outcome = condition.getMatchOutcome(context, customPrefixMetadata());

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage())
                .contains("acme.dev.services.keycloak.enabled is set to true");
    }

    @Test
    void shouldNotMatchWhenCustomPrefixDevServiceDisabled() {
        environment.setProperty("acme.dev.services.keycloak.enabled", "false");
        when(context.getEnvironment()).thenReturn(environment);

        ConditionOutcome outcome = condition.getMatchOutcome(context, customPrefixMetadata());

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage())
                .contains("acme.dev.services.keycloak.enabled is set to false");
    }

    @Test
    void shouldMatchByDefaultWhenCustomPrefixPropertyIsNotSet() {
        when(context.getEnvironment()).thenReturn(environment);

        ConditionOutcome outcome = condition.getMatchOutcome(context, customPrefixMetadata());

        assertThat(outcome.isMatch()).isTrue();
        assertThat(outcome.getMessage())
                .contains("enabled by default (acme.dev.services.keycloak.enabled is not set)");
    }

    @Test
    void shouldNotMatchWhenGloballyDisabledAndCustomPrefixDevServiceEnabled() {
        environment.setProperty("arconia.dev.services.enabled", "false");
        environment.setProperty("acme.dev.services.keycloak.enabled", "true");
        when(context.getEnvironment()).thenReturn(environment);

        ConditionOutcome outcome = condition.getMatchOutcome(context, customPrefixMetadata());

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage())
                .contains("arconia.dev.services.enabled is set to false");
    }

    @Test
    void shouldFailWhenCustomPrefixPropertyValueIsInvalid() {
        environment.setProperty("acme.dev.services.keycloak.enabled", "not a boolean");
        when(context.getEnvironment()).thenReturn(environment);

        assertThatThrownBy(() -> condition.getMatchOutcome(context, customPrefixMetadata()))
                .isInstanceOf(BindException.class);
    }

    @Test
    void shouldFailWhenCustomPrefixIsSpecifiedWithoutName() {
        when(context.getEnvironment()).thenReturn(environment);

        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("prefix", "acme.dev.services");
        when(metadata.getAnnotationAttributes(ConditionalOnDevServicesEnabled.class.getName()))
                .thenReturn(attributes);

        assertThatThrownBy(() -> condition.getMatchOutcome(context, metadata))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("requires a name when a custom prefix is specified");
    }

    @Test
    void shouldNotMatchWhenCustomPrefixAndProdMode() {
        System.setProperty(BootstrapMode.PROPERTY_KEY, "prod");
        BootstrapMode.clear();
        environment.setProperty("acme.dev.services.keycloak.enabled", "true");
        when(context.getEnvironment()).thenReturn(environment);

        ConditionOutcome outcome = condition.getMatchOutcome(context, customPrefixMetadata());

        assertThat(outcome.isMatch()).isFalse();
        assertThat(outcome.getMessage())
                .contains("dev services are only available in dev and test mode");
    }

    private AnnotatedTypeMetadata customPrefixMetadata() {
        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("name", "keycloak");
        attributes.put("prefix", "acme.dev.services");
        when(metadata.getAnnotationAttributes(ConditionalOnDevServicesEnabled.class.getName()))
                .thenReturn(attributes);
        return metadata;
    }

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
        void shouldMatchByDefaultWithCustomPrefix() {
            contextRunner
                    .withUserConfiguration(CustomPrefixDevServiceConfiguration.class)
                    .run(context -> assertThat(context).hasBean("customPrefixDevServiceBean"));
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
