package io.arconia.dev.services.ollama;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(OnOllamaNativeUnavailable.class);

    private static final String DEFAULT_BASE_URL = "http://localhost:11434";

    /**
     * Probe results cached per base URL: the condition can be evaluated multiple times
     * in the same JVM (context refreshes, cached test contexts), and the availability
     * of a native Ollama service is not expected to change mid-run.
     */
    private static final Map<String, Boolean> nativeConnectionCache = new ConcurrentHashMap<>();

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment environment = context.getEnvironment();

        try {
            OllamaDevServicesProperties devServicesProperties = Binder.get(environment)
                    .bindOrCreate(OllamaDevServicesProperties.CONFIG_PREFIX, OllamaDevServicesProperties.class);

            if (devServicesProperties.isIgnoreNativeService()) {
                return ConditionOutcome.match("Usage of Ollama native service is ignored: %s=%s".formatted(
                        OllamaDevServicesProperties.CONFIG_PREFIX + ".ignore-native-service", devServicesProperties.isIgnoreNativeService()));
            }

            String ollamaBaseUrl = resolveBaseUrl(environment);

            boolean isNativeConnection = nativeConnectionCache.computeIfAbsent(ollamaBaseUrl, this::isOllamaNativeConnection);
            if (!isNativeConnection) {
                logger.debug("No Ollama native service detected at {}. The dev service will provide a container.", ollamaBaseUrl);
                return ConditionOutcome.match("Ollama native connection is not available");
            }

            logger.debug("Ollama native service detected at {}. The dev service will not provide a container.", ollamaBaseUrl);
            return ConditionOutcome.noMatch(String.format("Ollama native connection detected at %s", ollamaBaseUrl));
        } catch (Exception e) {
            return ConditionOutcome.match("Failed to evaluate Ollama condition: " + e.getMessage());
        }
    }

    /**
     * Clears the cached probe results. For testing purposes only.
     */
    static void clearCache() {
        nativeConnectionCache.clear();
    }

    /**
     * Resolves the Ollama base URL from the Spring AI Ollama connection properties if available,
     * otherwise falls back to the default.
     */
    private String resolveBaseUrl(Environment environment) {
        if (ClassUtils.isPresent("org.springframework.ai.model.ollama.autoconfigure.OllamaConnectionProperties", null)) {
            OllamaConnectionProperties ollamaProperties = Binder.get(environment)
                    .bindOrCreate(OllamaConnectionProperties.CONFIG_PREFIX, OllamaConnectionProperties.class);
            if (StringUtils.hasText(ollamaProperties.getBaseUrl())) {
                return ollamaProperties.getBaseUrl();
            }
        }
        return DEFAULT_BASE_URL;
    }

    /**
     * Checks if Ollama native connection is available at the specified base URL.
     */
    boolean isOllamaNativeConnection(String baseUrl) {
        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(500))
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
