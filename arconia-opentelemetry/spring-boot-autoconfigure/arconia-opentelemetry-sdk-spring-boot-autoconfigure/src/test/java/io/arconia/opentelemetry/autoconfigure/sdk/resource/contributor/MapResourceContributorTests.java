package io.arconia.opentelemetry.autoconfigure.sdk.resource.contributor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.opentelemetry.sdk.resources.ResourceBuilder;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Unit tests for {@link MapResourceContributor}.
 */
class MapResourceContributorTests {

    private final ResourceBuilder resourceBuilder = mock(ResourceBuilder.class);

    @Test
    void shouldAddAttributes() {
        Map<String, String> attributes = Map.of("key1", "value1");
        MapResourceContributor contributor = new MapResourceContributor(attributes);

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put("key1", "value1");
        verifyNoMoreInteractions(resourceBuilder);
    }

    @Test
    void shouldAddMultipleAttributes() {
        Map<String, String> attributes = Map.of(
            "key1", "value1",
            "key2", "value2",
            "key3", "value3"
        );
        MapResourceContributor contributor = new MapResourceContributor(attributes);

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put("key1", "value1");
        verify(resourceBuilder).put("key2", "value2");
        verify(resourceBuilder).put("key3", "value3");
        verifyNoMoreInteractions(resourceBuilder);
    }

    @Test
    void shouldHandleEmptyMap() {
        MapResourceContributor contributor = new MapResourceContributor(Collections.emptyMap());

        contributor.contribute(resourceBuilder);

        verifyNoMoreInteractions(resourceBuilder);
    }

    @Test
    void shouldFailWhenAttributesIsNull() {
        assertThatThrownBy(() -> new MapResourceContributor(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("attributes cannot be null");
    }

    @Test
    void shouldFailWhenAttributeKeyIsNull() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put(null, "value");

        assertThatThrownBy(() -> new MapResourceContributor(attributes))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("attributes keys cannot be null");
    }

    @Test
    void shouldFailWhenAttributeValueIsNull() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("key", null);

        assertThatThrownBy(() -> new MapResourceContributor(attributes))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("attributes values cannot be null");
    }

    @Test
    void shouldFailWhenAttributeHasMultipleNullValues() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("key1", null);
        attributes.put("key2", null);

        assertThatThrownBy(() -> new MapResourceContributor(attributes))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("attributes values cannot be null");
    }

    @Test
    void shouldFailWhenAttributeHasMultipleNullKeys() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put(null, "value1");
        attributes.put(null, "value2");

        assertThatThrownBy(() -> new MapResourceContributor(attributes))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("attributes keys cannot be null");
    }

}
