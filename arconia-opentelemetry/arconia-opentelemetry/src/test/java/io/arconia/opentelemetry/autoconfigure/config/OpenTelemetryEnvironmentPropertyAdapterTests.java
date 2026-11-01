package io.arconia.opentelemetry.autoconfigure.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link OpenTelemetryEnvironmentPropertyAdapter}.
 */
@ExtendWith(OutputCaptureExtension.class)
class OpenTelemetryEnvironmentPropertyAdapterTests {

    @Nested
    class ArgumentChecks {

        @Test
        void whenNullEnvironmentThenThrow() {
            assertThatThrownBy(() -> new OpenTelemetryEnvironmentPropertyAdapter(null, LogFactory.getLog(getClass())))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("environment cannot be null");
        }

        @Test
        void whenNullLoggerThenThrow() {
            assertThatThrownBy(() -> new OpenTelemetryEnvironmentPropertyAdapter(new MockEnvironment(), null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("logger cannot be null");
        }

        @Test
        void whenEmptyExternalKeyThenThrow() {
            assertThatThrownBy(() -> adapter(new MockEnvironment()).mapString("", "arconia.string"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("externalKey cannot be null or empty");
        }

        @Test
        void whenEmptyArconiaKeyThenThrow() {
            assertThatThrownBy(() -> adapter(new MockEnvironment()).mapString("external.string", ""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("arconiaKey cannot be null or empty");
        }

        @Test
        void whenNullConverterThenThrow() {
            assertThatThrownBy(() -> adapter(new MockEnvironment()).mapProperty("external.custom", "arconia.custom", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("converter cannot be null");
        }

        @Test
        void whenNullEnumValuesThenThrow() {
            assertThatThrownBy(() -> adapter(new MockEnvironment()).mapEnum("external.enum", "arconia.enum", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("values cannot be null");
        }

    }

    @Nested
    class Values {

        @ParameterizedTest
        @ValueSource(strings = { "", "   " })
        void shouldSkipEmptyValues(String value) {
            var environment = new MockEnvironment().withProperty("external.string", value);

            var adapter = adapter(environment).mapString("external.string", "arconia.string");

            assertThat(adapter.getArconiaProperties()).isEmpty();
        }

        @Test
        void shouldSkipMissingValues() {
            var adapter = adapter(new MockEnvironment()).mapString("external.string", "arconia.string");

            assertThat(adapter.getArconiaProperties()).isEmpty();
        }

        @Test
        void shouldMapStrippedString() {
            var environment = new MockEnvironment().withProperty("external.string", " test-value ");

            var adapter = adapter(environment).mapString("external.string", "arconia.string");

            assertThat(adapter.getArconiaProperties()).containsEntry("arconia.string", "test-value");
        }

        @Test
        void shouldMapInteger() {
            var environment = new MockEnvironment().withProperty("external.integer", "42");

            var adapter = adapter(environment).mapInteger("external.integer", "arconia.integer");

            assertThat(adapter.getArconiaProperties()).containsEntry("arconia.integer", 42);
        }

        @Test
        void shouldSkipInvalidInteger(CapturedOutput output) {
            var environment = new MockEnvironment().withProperty("external.integer", "invalid");

            var adapter = adapter(environment).mapInteger("external.integer", "arconia.integer");

            assertThat(adapter.getArconiaProperties()).isEmpty();
            assertThat(output).contains("Unsupported value for external.integer: invalid");
        }

        @Test
        void shouldMapDouble() {
            var environment = new MockEnvironment().withProperty("external.double", "0.5");

            var adapter = adapter(environment).mapDouble("external.double", "arconia.double");

            assertThat(adapter.getArconiaProperties()).containsEntry("arconia.double", 0.5);
        }

        @Test
        void shouldSkipInvalidDouble() {
            var environment = new MockEnvironment().withProperty("external.double", "invalid");

            var adapter = adapter(environment).mapDouble("external.double", "arconia.double");

            assertThat(adapter.getArconiaProperties()).isEmpty();
        }

        @Test
        void shouldMapCustomProperty() {
            var environment = new MockEnvironment().withProperty("external.custom", "true");

            var adapter = adapter(environment).mapProperty("external.custom", "arconia.custom", value -> !Boolean.parseBoolean(value));

            assertThat(adapter.getArconiaProperties()).containsEntry("arconia.custom", false);
        }

        @Test
        void shouldLetTheLastMappingWin() {
            var environment = new MockEnvironment()
                    .withProperty("external.general", "general")
                    .withProperty("external.specific", "specific");

            var adapter = adapter(environment)
                    .mapString("external.general", "arconia.string")
                    .mapString("external.specific", "arconia.string")
                    .mapString("external.missing", "arconia.string");

            assertThat(adapter.getArconiaProperties()).containsEntry("arconia.string", "specific");
        }

    }

    @Nested
    class Durations {

        @ParameterizedTest
        @CsvSource({
                "60000, PT1M",
                "60000ms, PT1M",
                "60s, PT1M",
                "60m, PT1H",
                "24h, PT24H"
        })
        void shouldMapDuration(String value, Duration expected) {
            var environment = new MockEnvironment().withProperty("external.duration", value);

            var adapter = adapter(environment).mapDuration("external.duration", "arconia.duration");

            assertThat(adapter.getArconiaProperties()).containsEntry("arconia.duration", expected);
        }

        @ParameterizedTest
        @ValueSource(strings = { "invalid", "60x", "-5", "9223372036854775807h", "99999999999999999999" })
        void shouldSkipUnsupportedDuration(String value, CapturedOutput output) {
            var environment = new MockEnvironment().withProperty("external.duration", value);

            var adapter = adapter(environment).mapDuration("external.duration", "arconia.duration");

            assertThat(adapter.getArconiaProperties()).isEmpty();
            assertThat(output).contains("Unsupported value for external.duration: " + value);
        }

    }

    @Nested
    class Maps {

        @Test
        void shouldMapPercentDecodedEntries() {
            var environment = new MockEnvironment().withProperty("external.map", " key1 = a%2Cb%3Dc , key2=dGVzdA==,key3=a+b");

            var adapter = adapter(environment).mapMap("external.map", "arconia.map");

            assertThat(adapter.getArconiaProperties())
                    .containsEntry("arconia.map", Map.of("key1", "a,b=c", "key2", "dGVzdA==", "key3", "a+b"));
        }

        @Test
        void shouldSkipInvalidEntriesWithoutLoggingTheirValues(CapturedOutput output) {
            var environment = new MockEnvironment().withProperty("external.map", "Authorization: Bearer secret-token,=value1,key2=,key3=value3");

            var adapter = adapter(environment).mapMap("external.map", "arconia.map");

            assertThat(adapter.getArconiaProperties()).containsEntry("arconia.map", Map.of("key3", "value3"));
            assertThat(output)
                    .contains("Ignoring an invalid key-value pair in external.map")
                    .doesNotContain("secret-token");
        }

        @Test
        void shouldSkipEntryWithInvalidPercentEncoding(CapturedOutput output) {
            var environment = new MockEnvironment().withProperty("external.map", "key1=secret%,key2=value%202");

            var adapter = adapter(environment).mapMap("external.map", "arconia.map");

            assertThat(adapter.getArconiaProperties()).containsEntry("arconia.map", Map.of("key2", "value 2"));
            assertThat(output)
                    .contains("Ignoring the 'key1' entry in external.map because its value is not correctly percent-encoded")
                    .doesNotContain("secret%");
        }

        @Test
        void shouldSkipMapWithoutValidEntries() {
            var environment = new MockEnvironment().withProperty("external.map", "invalid1,invalid2");

            var adapter = adapter(environment).mapMap("external.map", "arconia.map");

            assertThat(adapter.getArconiaProperties()).isEmpty();
        }

    }

    @Nested
    class Enums {

        private static final Map<String, TestEnum> VALUES = Map.of("first", TestEnum.FIRST, "second", TestEnum.SECOND);

        @Test
        void shouldMapEnumCaseInsensitively() {
            var environment = new MockEnvironment().withProperty("external.enum", "FIRST");

            var adapter = adapter(environment).mapEnum("external.enum", "arconia.enum", VALUES);

            assertThat(adapter.getArconiaProperties()).containsEntry("arconia.enum", TestEnum.FIRST);
        }

        @Test
        void shouldSkipUnsupportedEnum(CapturedOutput output) {
            var environment = new MockEnvironment().withProperty("external.enum", "third");

            var adapter = adapter(environment).mapEnum("external.enum", "arconia.enum", VALUES);

            assertThat(adapter.getArconiaProperties()).isEmpty();
            assertThat(output).contains("Unsupported value for external.enum: third");
        }

        @Test
        void shouldMapEnumListInOrderWithoutDuplicatesOrUnsupportedValues(CapturedOutput output) {
            var environment = new MockEnvironment().withProperty("external.enums", "second, FIRST,,third,second");

            var adapter = adapter(environment).mapEnumList("external.enums", "arconia.enums", VALUES);

            assertThat(adapter.getArconiaProperties()).containsEntry("arconia.enums", List.of(TestEnum.SECOND, TestEnum.FIRST));
            assertThat(output).contains("Unsupported value for external.enums: third");
        }

        @Test
        void shouldSkipEnumListWithoutSupportedValues() {
            var environment = new MockEnvironment().withProperty("external.enums", "third");

            var adapter = adapter(environment).mapEnumList("external.enums", "arconia.enums", VALUES);

            assertThat(adapter.getArconiaProperties()).isEmpty();
        }

    }

    @Nested
    class PropertySources {

        @Test
        void shouldRecordTheOriginOfEachProperty() {
            var environment = new MockEnvironment().withProperty("otel.service.name", "test-service");

            var propertySource = adapter(environment)
                    .mapString("otel.service.name", "arconia.otel.resource.service-name")
                    .toPropertySource("test");

            assertThat(propertySource.getName()).isEqualTo("test");
            assertThat(propertySource.getProperty("arconia.otel.resource.service-name")).isEqualTo("test-service");
            assertThat(((OriginLookup<String>) propertySource).getOrigin("arconia.otel.resource.service-name"))
                    .hasToString("Adapted from the OpenTelemetry property 'otel.service.name'");
        }

    }

    private static OpenTelemetryEnvironmentPropertyAdapter adapter(MockEnvironment environment) {
        return new OpenTelemetryEnvironmentPropertyAdapter(environment, LogFactory.getLog(OpenTelemetryEnvironmentPropertyAdapterTests.class));
    }

    private enum TestEnum {
        FIRST, SECOND
    }

}
