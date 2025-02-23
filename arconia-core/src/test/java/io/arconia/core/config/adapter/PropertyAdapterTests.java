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
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PropertyAdapter}.
 */
@ExtendWith(MockitoExtension.class)
class PropertyAdapterTests {

    @Mock
    private ConfigurableEnvironment environment;

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
    void shouldMapBooleanProperty() {
        when(environment.getProperty("external.boolean")).thenReturn("true");

        var adapter = PropertyAdapter.builder(environment)
            .mapBoolean("external.boolean", "arconia.boolean")
            .build();

        assertThat(adapter.getArconiaProperties())
            .containsEntry("arconia.boolean", true);
    }

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
    void shouldMapListProperty() {
        when(environment.getProperty("external.list")).thenReturn("value1,value2,value3");

        var adapter = PropertyAdapter.builder(environment)
            .mapList("external.list", "arconia.list")
            .build();

        assertThat(adapter.getArconiaProperties())
            .containsEntry("arconia.list", List.of("value1", "value2", "value3"));
    }

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
    void shouldMapCustomProperty() {
        when(environment.getProperty("external.custom")).thenReturn("42");

        var adapter = PropertyAdapter.builder(environment)
            .mapProperty("external.custom", "arconia.custom", Integer::parseInt)
            .build();

        assertThat(adapter.getArconiaProperties())
            .containsEntry("arconia.custom", 42);
    }

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
    void shouldHandleEmptyInputForMap() {
        when(environment.getProperty("external.map")).thenReturn("");

        var adapter = PropertyAdapter.builder(environment)
            .mapMap("external.map", "arconia.map")
            .build();

        assertThat(adapter.getArconiaProperties())
            .doesNotContainKey("arconia.map");
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
}
