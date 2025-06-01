package io.arconia.core.config.adapter;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.ConfigurableEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PropertyAdapter}.
 */
@ExtendWith(MockitoExtension.class)
class PropertyAdapterTests {

    @Mock
    private ConfigurableEnvironment environment;

    // ARGUMENT CHECKS

    @Test
    void whenNullEnvironmentThenThrow() {
        assertThatThrownBy(() -> PropertyAdapter.builder(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("environment cannot be null");
    }

    @Test
    void whenNullExternalKeyThenThrow() {
        assertThatThrownBy(() -> PropertyAdapter.builder(environment)
            .mapString(null, "arconia.string"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("externalKey cannot be null or empty");
    }

    @Test
    void whenEmptyExternalKeyThenThrow() {
        assertThatThrownBy(() -> PropertyAdapter.builder(environment)
            .mapString("", "arconia.string"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("externalKey cannot be null or empty");
    }

    @Test
    void whenNullArconiaKeyThenThrow() {
        assertThatThrownBy(() -> PropertyAdapter.builder(environment)
            .mapString("external.string", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("arconiaKey cannot be null or empty");
    }

    @Test
    void whenEmptyArconiaKeyThenThrow() {
        assertThatThrownBy(() -> PropertyAdapter.builder(environment)
            .mapString("external.string", ""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("arconiaKey cannot be null or empty");
    }

    @Test
    void whenNullConverterThenThrow() {
        assertThatThrownBy(() -> PropertyAdapter.builder(environment)
            .mapProperty("external.custom", "arconia.custom", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("converter cannot be null");
    }

    @Test
    void whenNullConverterFactoryThenThrow() {
        assertThatThrownBy(() -> PropertyAdapter.builder(environment)
            .mapEnum("external.enum", "arconia.enum", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("converterFactory cannot be null");
    }

    // STRING

    @Test
    void shouldMapStringProperty() {
        when(environment.getProperty("external.string")).thenReturn("test-value");

        var adapter = PropertyAdapter.builder(environment)
            .mapString("external.string", "arconia.string")
            .build();

        assertThat(adapter.getArconiaProperties())
            .containsEntry("arconia.string", "test-value");
    }

    @Test
    void shouldHandleNullInputForString() {
        when(environment.getProperty("external.string")).thenReturn(null);

        var adapter = PropertyAdapter.builder(environment)
            .mapString("external.string", "arconia.string")
            .build();

        assertThat(adapter.getArconiaProperties())
            .doesNotContainKey("arconia.string");
    }

    @Test
    void shouldHandleEmptyInputForString() {
        when(environment.getProperty("external.string")).thenReturn("");

        var adapter = PropertyAdapter.builder(environment)
            .mapString("external.string", "arconia.string")
            .build();

        assertThat(adapter.getArconiaProperties())
            .doesNotContainKey("arconia.string");
    }

    @Test
    void shouldHandleBlankInputForString() {
        when(environment.getProperty("external.string")).thenReturn("   ");

        var adapter = PropertyAdapter.builder(environment)
            .mapString("external.string", "arconia.string")
            .build();

        assertThat(adapter.getArconiaProperties())
            .doesNotContainKey("arconia.string");
    }

    // BOOLEAN

    @Test
    void shouldMapBooleanProperty() {
        when(environment.getProperty("external.boolean")).thenReturn("true");

        var adapter = PropertyAdapter.builder(environment)
            .mapBoolean("external.boolean", "arconia.boolean")
            .build();

        assertThat(adapter.getArconiaProperties())
            .containsEntry("arconia.boolean", true);
    }

    @Test
    void shouldHandleNullInputForBoolean() {
        when(environment.getProperty("external.boolean")).thenReturn(null);

        var adapter = PropertyAdapter.builder(environment)
            .mapBoolean("external.boolean", "arconia.boolean")
            .build();

        assertThat(adapter.getArconiaProperties())
            .doesNotContainKey("arconia.boolean");
    }

    @Test
    void shouldHandleEmptyInputForBoolean() {
        when(environment.getProperty("external.boolean")).thenReturn("");

        var adapter = PropertyAdapter.builder(environment)
            .mapBoolean("external.boolean", "arconia.boolean")
            .build();

        assertThat(adapter.getArconiaProperties())
            .doesNotContainKey("arconia.boolean");
    }

    // DOUBLE

    @Test
    void shouldMapDoubleProperty() {
        when(environment.getProperty("external.double")).thenReturn("42.5");

        var adapter = PropertyAdapter.builder(environment)
            .mapDouble("external.double", "arconia.double")
            .build();

        assertThat(adapter.getArconiaProperties())
            .containsEntry("arconia.double", 42.5);
    }

    @Test
    void shouldHandleInvalidDoubleProperty() {
        when(environment.getProperty("external.double")).thenReturn("invalid");

        var adapter = PropertyAdapter.builder(environment)
            .mapDouble("external.double", "arconia.double")
            .build();

        assertThat(adapter.getArconiaProperties())
            .doesNotContainKey("arconia.double");
    }

    @Test
    void shouldHandleNullInputForDouble() {
        when(environment.getProperty("external.double")).thenReturn(null);

        var adapter = PropertyAdapter.builder(environment)
            .mapDouble("external.double", "arconia.double")
            .build();

        assertThat(adapter.getArconiaProperties())
            .doesNotContainKey("arconia.double");
    }

    @Test
    void shouldHandleEmptyInputForDouble() {
        when(environment.getProperty("external.double")).thenReturn("");

        var adapter = PropertyAdapter.builder(environment)
            .mapDouble("external.double", "arconia.double")
            .build();

        assertThat(adapter.getArconiaProperties())
            .doesNotContainKey("arconia.double");
    }

    // DURATION

    @Test
    void shouldMapDurationPropertyWithMilliseconds() {
        when(environment.getProperty("external.duration")).thenReturn("60000");

        var adapter = PropertyAdapter.builder(environment)
            .mapDuration("external.duration", "arconia.duration")
            .build();

        assertThat(adapter.getArconiaProperties())
            .containsEntry("arconia.duration", Duration.ofMillis(60000));
    }

    @Test
    void shouldMapDurationPropertyWithMillisecondsUnit() {
        when(environment.getProperty("external.duration")).thenReturn("60000ms");

        var adapter = PropertyAdapter.builder(environment)
            .mapDuration("external.duration", "arconia.duration")
            .build();

        assertThat(adapter.getArconiaProperties())
            .containsEntry("arconia.duration", Duration.ofMillis(60000));
    }

    @Test
    void shouldMapDurationPropertyWithSecondsUnit() {
        when(environment.getProperty("external.duration")).thenReturn("60s");

        var adapter = PropertyAdapter.builder(environment)
            .mapDuration("external.duration", "arconia.duration")
            .build();

        assertThat(adapter.getArconiaProperties())
            .containsEntry("arconia.duration", Duration.ofSeconds(60));
    }

    @Test
    void shouldMapDurationPropertyWithMinutesUnit() {
        when(environment.getProperty("external.duration")).thenReturn("60m");

        var adapter = PropertyAdapter.builder(environment)
            .mapDuration("external.duration", "arconia.duration")
            .build();

        assertThat(adapter.getArconiaProperties())
            .containsEntry("arconia.duration", Duration.ofMinutes(60));
    }

    @Test
    void shouldMapDurationPropertyWithHoursUnit() {
        when(environment.getProperty("external.duration")).thenReturn("24h");

        var adapter = PropertyAdapter.builder(environment)
            .mapDuration("external.duration", "arconia.duration")
            .build();

        assertThat(adapter.getArconiaProperties())
            .containsEntry("arconia.duration", Duration.ofHours(24));
    }

    @Test
    void shouldHandleInvalidDurationUnitProperty() {
        when(environment.getProperty("external.duration")).thenReturn("60x");

        var adapter = PropertyAdapter.builder(environment)
            .mapDuration("external.duration", "arconia.duration")
            .build();

        assertThat(adapter.getArconiaProperties())
            .doesNotContainKey("arconia.duration");
    }

    @Test
    void shouldHandleInvalidDurationProperty() {
        when(environment.getProperty("external.duration")).thenReturn("invalid");

        var adapter = PropertyAdapter.builder(environment)
            .mapDuration("external.duration", "arconia.duration")
            .build();

        assertThat(adapter.getArconiaProperties())
            .doesNotContainKey("arconia.duration");
    }

    @Test
    void shouldHandleNullInputForDuration() {
        when(environment.getProperty("external.duration")).thenReturn(null);

        var adapter = PropertyAdapter.builder(environment)
            .mapDuration("external.duration", "arconia.duration")
            .build();

        assertThat(adapter.getArconiaProperties())
            .doesNotContainKey("arconia.duration");
    }

    @Test
    void shouldHandleEmptyInputForDuration() {
        when(environment.getProperty("external.duration")).thenReturn("");

        var adapter = PropertyAdapter.builder(environment)
            .mapDuration("external.duration", "arconia.duration")
            .build();

        assertThat(adapter.getArconiaProperties())
            .doesNotContainKey("arconia.duration");
    }

    // INTEGER

    @Test
    void shouldMapIntegerProperty() {
        when(environment.getProperty("external.integer")).thenReturn("42");

        var adapter = PropertyAdapter.builder(environment)
            .mapInteger("external.integer", "arconia.integer")
            .build();

        assertThat(adapter.getArconiaProperties())
            .containsEntry("arconia.integer", 42);
    }

    @Test
    void shouldHandleInvalidIntegerProperty() {
        when(environment.getProperty("external.integer")).thenReturn("invalid");

        var adapter = PropertyAdapter.builder(environment)
            .mapInteger("external.integer", "arconia.integer")
            .build();

        assertThat(adapter.getArconiaProperties())
            .doesNotContainKey("arconia.integer");
    }

    @Test
    void shouldHandleNullInputForInteger() {
        when(environment.getProperty("external.integer")).thenReturn(null);

        var adapter = PropertyAdapter.builder(environment)
            .mapInteger("external.integer", "arconia.integer")
            .build();

        assertThat(adapter.getArconiaProperties())
            .doesNotContainKey("arconia.integer");
    }

    @Test
    void shouldHandleEmptyInputForInteger() {
        when(environment.getProperty("external.integer")).thenReturn("");

        var adapter = PropertyAdapter.builder(environment)
            .mapInteger("external.integer", "arconia.integer")
            .build();

        assertThat(adapter.getArconiaProperties())
            .doesNotContainKey("arconia.integer");
    }

    // LIST

    @Test
    void shouldMapListProperty() {
        when(environment.getProperty("external.list")).thenReturn("value1,value2,value3");

        var adapter = PropertyAdapter.builder(environment)
            .mapList("external.list", "arconia.list")
            .build();

        assertThat(adapter.getArconiaProperties())
            .containsEntry("arconia.list", List.of("value1", "value2", "value3"));
    }

    @Test
    void shouldHandleNullInputForList() {
        when(environment.getProperty("external.list")).thenReturn(null);

        var adapter = PropertyAdapter.builder(environment)
            .mapList("external.list", "arconia.list")
            .build();

        assertThat(adapter.getArconiaProperties())
            .doesNotContainKey("arconia.list");
    }

    @Test
    void shouldHandleEmptyInputForList() {
        when(environment.getProperty("external.list")).thenReturn("");

        var adapter = PropertyAdapter.builder(environment)
            .mapList("external.list", "arconia.list")
            .build();

        assertThat(adapter.getArconiaProperties())
            .doesNotContainKey("arconia.list");
    }

    @Test
    void shouldHandleEmptyListValue() {
        when(environment.getProperty("external.list")).thenReturn(",");

        var adapter = PropertyAdapter.builder(environment)
            .mapList("external.list", "arconia.list")
            .build();

        assertThat(adapter.getArconiaProperties())
            .doesNotContainKey("arconia.list");
    }

    // MAP

    @Test
    void shouldMapMapProperty() {
        when(environment.getProperty("external.map")).thenReturn("key1=value1,key2=value2");

        var adapter = PropertyAdapter.builder(environment)
            .mapMap("external.map", "arconia.map")
            .build();

        assertThat(adapter.getArconiaProperties())
            .containsEntry("arconia.map", Map.of("key1", "value1", "key2", "value2"));
    }

    @Test
    void shouldHandleInvalidMapProperty() {
        when(environment.getProperty("external.map")).thenReturn("invalid,key2=value2");

        var adapter = PropertyAdapter.builder(environment)
            .mapMap("external.map", "arconia.map")
            .build();

        assertThat(adapter.getArconiaProperties())
            .containsEntry("arconia.map", Map.of("key2", "value2"));
    }

    @Test
    void shouldHandleEmptyMapProperty() {
        when(environment.getProperty("external.map")).thenReturn("");

        var adapter = PropertyAdapter.builder(environment)
            .mapMap("external.map", "arconia.map")
            .build();

        assertThat(adapter.getArconiaProperties())
            .doesNotContainKey("arconia.map");
    }

    @Test
    void shouldHandleNullInputForMap() {
        when(environment.getProperty("external.map")).thenReturn(null);

        var adapter = PropertyAdapter.builder(environment)
            .mapMap("external.map", "arconia.map")
            .build();

        assertThat(adapter.getArconiaProperties())
            .doesNotContainKey("arconia.map");
    }

    @Test
    void shouldProcessMapWithPostProcessor() {
        when(environment.getProperty("external.map")).thenReturn("key1=value1,key2=value2");

        var adapter = PropertyAdapter.builder(environment)
            .mapMap("external.map", "arconia.map", map -> {
                map.put("key3", "value3");
                return map;
            })
            .build();

        assertThat(adapter.getArconiaProperties())
            .containsEntry("arconia.map", Map.of("key1", "value1", "key2", "value2", "key3", "value3"));
    }

    @Test
    void shouldHandleEmptyKeyValuePairsInMap() {
        when(environment.getProperty("external.map")).thenReturn("=value1,key2=");

        var adapter = PropertyAdapter.builder(environment)
            .mapMap("external.map", "arconia.map")
            .build();

        assertThat(adapter.getArconiaProperties())
            .doesNotContainKey("arconia.map");
    }

    @Test
    void shouldHandleMultipleInvalidEntriesInMap() {
        when(environment.getProperty("external.map")).thenReturn("invalid1,key1=value1,invalid2,key2=value2");

        var adapter = PropertyAdapter.builder(environment)
            .mapMap("external.map", "arconia.map")
            .build();

        assertThat(adapter.getArconiaProperties())
            .containsEntry("arconia.map", Map.of("key1", "value1", "key2", "value2"));
    }

    @Test
    void shouldHandleAllInvalidEntriesInMap() {
        when(environment.getProperty("external.map")).thenReturn("invalid1,invalid2,invalid3");

        var adapter = PropertyAdapter.builder(environment)
            .mapMap("external.map", "arconia.map")
            .build();

        assertThat(adapter.getArconiaProperties())
            .doesNotContainKey("arconia.map");
    }

    @Test
    void shouldStripWhitespaceInMapKeyValues() {
        when(environment.getProperty("external.map")).thenReturn(" key1 = value1 , key2 = value2 ");

        var adapter = PropertyAdapter.builder(environment)
            .mapMap("external.map", "arconia.map")
            .build();

        assertThat(adapter.getArconiaProperties())
            .containsEntry("arconia.map", Map.of("key1", "value1", "key2", "value2"));
    }

    @Test
    void shouldHandleNullMapFromPostProcessor() {
        when(environment.getProperty("external.map")).thenReturn("key1=value1,key2=value2");

        var adapter = PropertyAdapter.builder(environment)
            .mapMap("external.map", "arconia.map", map -> null)
            .build();

        assertThat(adapter.getArconiaProperties())
            .doesNotContainKey("arconia.map");
    }

    @Test
    void shouldHandleEmptyMapFromPostProcessor() {
        when(environment.getProperty("external.map")).thenReturn("key1=value1,key2=value2");

        var adapter = PropertyAdapter.builder(environment)
            .mapMap("external.map", "arconia.map", map -> Map.of())
            .build();

        assertThat(adapter.getArconiaProperties())
            .doesNotContainKey("arconia.map");
    }

    // ENUM
    
    @Test
    void shouldMapEnum() {
        when(environment.getProperty("external.enum")).thenReturn("TEST_VALUE");

        var adapter = PropertyAdapter.builder(environment)
            .mapEnum("external.enum", "arconia.enum", key -> value -> {
                try {
                    return TestEnum.valueOf(value);
                } catch (IllegalArgumentException e) {
                    return null;
                }
            })
            .build();

        assertThat(adapter.getArconiaProperties())
            .containsEntry("arconia.enum", TestEnum.TEST_VALUE);
    }

    @Test
    void shouldHandleInvalidEnum() {
        when(environment.getProperty("external.enum")).thenReturn("INVALID_VALUE");

        var adapter = PropertyAdapter.builder(environment)
            .mapEnum("external.enum", "arconia.enum", key -> value -> {
                try {
                    return TestEnum.valueOf(value);
                } catch (IllegalArgumentException e) {
                    return null;
                }
            })
            .build();

        assertThat(adapter.getArconiaProperties())
            .doesNotContainKey("arconia.enum");
    }

    // CUSTOM

    @Test
    void shouldMapCustomProperty() {
        when(environment.getProperty("external.custom")).thenReturn("42");

        var adapter = PropertyAdapter.builder(environment)
            .mapProperty("external.custom", "arconia.custom", Integer::parseInt)
            .build();

        assertThat(adapter.getArconiaProperties())
            .containsEntry("arconia.custom", 42);
    }

    // MULTIPLE

    @Test
    void shouldMapMultipleProperties() {
        when(environment.getProperty("external.string")).thenReturn("test");
        when(environment.getProperty("external.boolean")).thenReturn("true");
        when(environment.getProperty("external.double")).thenReturn("42.5");

        var adapter = PropertyAdapter.builder(environment)
            .mapString("external.string", "arconia.string")
            .mapBoolean("external.boolean", "arconia.boolean")
            .mapDouble("external.double", "arconia.double")
            .build();

        assertThat(adapter.getArconiaProperties())
            .containsEntry("arconia.string", "test")
            .containsEntry("arconia.boolean", true)
            .containsEntry("arconia.double", 42.5);
    }

    private enum TestEnum {
        TEST_VALUE
    }

}
