package io.arconia.dev.ui.server.pages;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.actuate.endpoint.SanitizingFunction;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SensitiveProperties}.
 */
class SensitivePropertiesTests {

    private final SensitiveProperties sensitiveProperties = new SensitiveProperties(List.of());

    @ParameterizedTest
    @ValueSource(strings = { "spring.datasource.password", "SPRING_DATASOURCE_PASSWORD", "app.client-secret", "AWS_SECRET_ACCESS_KEY",
            "github.token", "arconia.docling.bearer-token", "aws.credentials.value", "openai.api-key", "openai.apiKey", "OPENAI_API_KEY",
            "jwt.signing-key", "sun.java.command", "spring.application.json", "vcap.services.db.name" })
    void propertyThatIsLikelyToHoldSecretIsSensitive(String name) {
        assertThat(this.sensitiveProperties.isSensitive(null, name, "value")).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = { "server.port", "spring.application.name", "spring.datasource.url", "spring.ai.ollama.base-url",
            "spring.cache.redis.key-prefix", "spring.ssl.bundle.jks.web.keystore.location", "user.home", "PATH" })
    void anyOtherPropertyIsNot(String name) {
        assertThat(this.sensitiveProperties.isSensitive(null, name, "value")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = { "postgresql://app:s3cret@localhost:5432/catalog", "jdbc:postgresql://app:s3cret@localhost/catalog",
            "https://user:token@example.com/repository.git" })
    void valueThatIsUrlWithCredentialsIsSensitiveWhateverThePropertyIsNamed(String value) {
        assertThat(this.sensitiveProperties.isSensitive(null, "app.location", value)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = { "jdbc:postgresql://localhost:5432/catalog", "http://localhost:11434", "mailto:someone@example.com" })
    void valueThatIsUrlWithoutCredentialsIsNot(String value) {
        assertThat(this.sensitiveProperties.isSensitive(null, "spring.datasource.url", value)).isFalse();
    }

    @Test
    void sanitizingFunctionsOfTheApplicationDecideToo() {
        SensitiveProperties withApplicationRule = new SensitiveProperties(
                List.of(SanitizingFunction.sanitizeValue().ifKeyEquals("app.customer-number")));

        assertThat(withApplicationRule.isSensitive(null, "app.customer-number", "12345")).isTrue();
        assertThat(withApplicationRule.isSensitive(null, "spring.datasource.password", "s3cret")).isTrue();
        assertThat(withApplicationRule.isSensitive(null, "server.port", "8080")).isFalse();
    }

}
