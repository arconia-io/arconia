package io.arconia.dev.services.ollama;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
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
 * The endpoint is resolved from the Ollama container when one is available,
 * or from the native Ollama service otherwise.
 */
@AutoConfiguration(after = OllamaDevServicesAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("ollama")
@ConditionalOnClass(name = "org.springframework.ai.model.openai.autoconfigure.OpenAiConnectionProperties")
@Import(OllamaOpenAiPropertyRegistrar.class)
public class OllamaOpenAiDevServicesAutoConfiguration {

    private static final String DEFAULT_OLLAMA_BASE_URL = "http://localhost:11434";

    private static final String OLLAMA_BASE_URL_PROPERTY = "spring.ai.ollama.base-url";

    static class OllamaOpenAiPropertyRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
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
                String baseUrl = environment.getProperty(OLLAMA_BASE_URL_PROPERTY);
                return (baseUrl != null && !baseUrl.isBlank()) ? baseUrl : DEFAULT_OLLAMA_BASE_URL;
            }
        }

    }

}
