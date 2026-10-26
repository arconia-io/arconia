package io.arconia.observation.opentelemetry.instrumentation.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenTelemetryHttpOptions}.
 */
class OpenTelemetryHttpOptionsTests {

    private static final String SENSITIVE_QUERY_PARAMETERS_PROPERTY =
            "arconia.observations.conventions.opentelemetry.http.sensitive-query-parameters";

    @Test
    void sensitiveQueryParametersDefaultMatchesConfigurationMetadata() throws IOException {
        List<String> metadataDefault = metadataDefaultValue(SENSITIVE_QUERY_PARAMETERS_PROPERTY);

        assertThat(metadataDefault).isEqualTo(new OpenTelemetryHttpOptions().getSensitiveQueryParameters());
    }

    /**
     * Reads the default value of a property from the generated configuration metadata,
     * which the configuration processor can't infer for collections.
     */
    private static List<String> metadataDefaultValue(String propertyName) throws IOException {
        JsonMapper jsonMapper = JsonMapper.shared();
        for (URL resource : Collections.list(OpenTelemetryHttpOptionsTests.class.getClassLoader()
                .getResources("META-INF/spring-configuration-metadata.json"))) {
            try (InputStream inputStream = resource.openStream()) {
                for (JsonNode property : jsonMapper.readTree(inputStream).path("properties")) {
                    if (propertyName.equals(property.path("name").asString())) {
                        List<String> values = new ArrayList<>();
                        property.path("defaultValue").forEach(value -> values.add(value.asString()));
                        return values;
                    }
                }
            }
        }
        throw new AssertionError("Property not found in the configuration metadata: " + propertyName);
    }

}
