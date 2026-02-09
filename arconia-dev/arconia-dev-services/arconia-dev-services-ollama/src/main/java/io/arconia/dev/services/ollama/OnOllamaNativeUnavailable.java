package io.arconia.dev.services.ollama;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.ai.model.ollama.autoconfigure.OllamaConnectionProperties;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Condition to check if Ollama native connection is available.
 */
class OnOllamaNativeUnavailable extends SpringBootCondition {

    private static final String DEFAULT_BASE_URL = "http://localhost:11434";

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        if (!ClassUtils.isPresent("org.springframework.ai.model.ollama.autoconfigure.OllamaConnectionProperties", null)) {
            return ConditionOutcome.match("The Spring AI Ollama module is not available in the classpath.");
        }

        Environment environment = context.getEnvironment();

        try {
            OllamaDevServicesProperties devServicesProperties = Binder.get(environment)
                    .bindOrCreate(OllamaDevServicesProperties.CONFIG_PREFIX, OllamaDevServicesProperties.class);

            if (devServicesProperties.isIgnoreNativeService()) {
                return ConditionOutcome.match("Usage of Ollama native service is ignored: %s=%s".formatted(
                        OllamaDevServicesProperties.CONFIG_PREFIX + ".ignore-native-service", devServicesProperties.isIgnoreNativeService()));
            }

            OllamaConnectionProperties ollamaProperties = Binder.get(environment)
                    .bindOrCreate(OllamaConnectionProperties.CONFIG_PREFIX, OllamaConnectionProperties.class);

            String ollamaBaseUrl = StringUtils.hasText(ollamaProperties.getBaseUrl())
                    ? ollamaProperties.getBaseUrl()
                    : DEFAULT_BASE_URL;

            boolean isNativeConnection = isOllamaNativeConnection(ollamaBaseUrl);
            if (!isNativeConnection) {
                return ConditionOutcome.match("Ollama native connection is not available");
            }

            return ConditionOutcome.noMatch(String.format("Ollama native connection detected at %s", ollamaBaseUrl));
        } catch (Exception e) {
            return ConditionOutcome.match("Failed to evaluate Ollama condition: " + e.getMessage());
        }
    }

    /**
     * Checks if Ollama native connection is available at the specified base URL.
     */
    boolean isOllamaNativeConnection(String baseUrl) {
        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build()) {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .HEAD()
                    .timeout(Duration.ofSeconds(1))
                    .build();

            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());

            return response.statusCode() == 200;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            return false;
        }
    }

}
