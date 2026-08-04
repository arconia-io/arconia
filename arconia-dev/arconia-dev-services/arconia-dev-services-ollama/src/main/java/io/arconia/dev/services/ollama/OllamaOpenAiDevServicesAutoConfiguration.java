package io.arconia.dev.services.ollama;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.model.ollama.autoconfigure.OllamaConnectionDetails;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;
import org.testcontainers.ollama.OllamaContainer;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.ollama.OllamaOpenAiDevServicesAutoConfiguration.OllamaOpenAiPropertyRegistrar;

/**
 * Auto-configuration for Ollama Dev Services with Spring AI OpenAI compatibility.
 * <p>
 * When the Ollama Dev Service is active and the Spring AI OpenAI module is on the classpath,
 * this auto-configuration registers dynamic properties to connect the OpenAI client
 * to Ollama's OpenAI-compatible API endpoint.
 * <p>
 * The endpoint is resolved from the Ollama container when one is available
 * (either owned by this application or a shared container discovered from
 * another application), or from the native Ollama service otherwise.
 * <p>
 * The registered properties take precedence over any user-provided value, mirroring how a
 * {@code ConnectionDetails} bean supersedes the corresponding configuration properties for
 * the other dev services. Set
 * {@code arconia.dev.services.ollama.openai-compatibility=false} to opt out and keep your
 * own OpenAI configuration, without having to disable the Ollama dev service altogether.
 */
@AutoConfiguration(after = OllamaDevServicesAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("ollama")
@ConditionalOnClass(name = "org.springframework.ai.model.openai.autoconfigure.OpenAiCommonProperties")
@ConditionalOnProperty(prefix = OllamaDevServicesProperties.CONFIG_PREFIX, name = "openai-compatibility",
        havingValue = "true", matchIfMissing = true)
@Import(OllamaOpenAiPropertyRegistrar.class)
public final class OllamaOpenAiDevServicesAutoConfiguration {

    private static final String DEFAULT_OLLAMA_BASE_URL = "http://localhost:11434";

    private static final String OLLAMA_BASE_URL_PROPERTY = "spring.ai.ollama.base-url";

    static class OllamaOpenAiPropertyRegistrar extends DevServicesRegistrar {

        private static final String OLLAMA_CONNECTION_DETAILS_CLASS =
                "org.springframework.ai.model.ollama.autoconfigure.OllamaConnectionDetails";

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            // Spring AI's OpenAI module has no ConnectionDetails abstraction to contribute to
            // (unlike its Ollama module, which ships OllamaConnectionDetails), so the endpoint is
            // published as a dynamic property instead. That is what gives it precedence over
            // user-provided values, matching ConnectionDetails semantics.
            addDynamicProperty("spring.ai.openai.base-url",
                    () -> resolveOllamaEndpoint(getBeanFactory(), environment));
            addDynamicProperty("spring.ai.openai.api-key", () -> "ollama");
        }

        private static String resolveOllamaEndpoint(BeanFactory beanFactory, Environment environment) {
            try {
                OllamaContainer container = beanFactory.getBean(OllamaContainer.class);
                return container.getEndpoint();
            }
            catch (NoSuchBeanDefinitionException ex) {
                // No container bean: the dev service adopted a shared container discovered
                // from another application, or a native Ollama service is used.
            }

            if (ClassUtils.isPresent(OLLAMA_CONNECTION_DETAILS_CLASS, null)) {
                String baseUrl = resolveBaseUrlFromConnectionDetails(beanFactory);
                if (baseUrl != null) {
                    return baseUrl;
                }
            }

            String baseUrl = environment.getProperty(OLLAMA_BASE_URL_PROPERTY);
            return (baseUrl != null && !baseUrl.isBlank()) ? baseUrl : DEFAULT_OLLAMA_BASE_URL;
        }

        /**
         * Kept in a separate method so the Spring AI Ollama types are only loaded when present.
         */
        @Nullable
        private static String resolveBaseUrlFromConnectionDetails(BeanFactory beanFactory) {
            try {
                return beanFactory.getBean(OllamaConnectionDetails.class).getBaseUrl();
            } catch (NoSuchBeanDefinitionException ex) {
                return null;
            }
        }

    }

}
