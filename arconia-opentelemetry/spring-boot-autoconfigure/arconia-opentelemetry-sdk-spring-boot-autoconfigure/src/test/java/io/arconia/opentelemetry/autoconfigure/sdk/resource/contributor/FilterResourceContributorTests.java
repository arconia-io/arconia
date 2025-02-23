package io.arconia.opentelemetry.autoconfigure.sdk.resource.contributor;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.resources.ResourceBuilder;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link FilterResourceContributor}.
 */
class FilterResourceContributorTests {

    private final ResourceBuilder resourceBuilder = mock(ResourceBuilder.class);

    @Test
    @SuppressWarnings("unchecked")
    void shouldRemoveDisabledKeys() {
        FilterResourceContributor contributor = new FilterResourceContributor(List.of("test.key"));

        contributor.contribute(resourceBuilder);

        ArgumentCaptor<Predicate<AttributeKey<?>>> predicateCaptor = ArgumentCaptor.forClass(Predicate.class);
        verify(resourceBuilder).removeIf(predicateCaptor.capture());

        Predicate<AttributeKey<?>> predicate = predicateCaptor.getValue();
        assertThat(predicate.test(AttributeKey.stringKey("test.key"))).isTrue();
        assertThat(predicate.test(AttributeKey.stringKey("other.key"))).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldNotRemoveKeysWhenNotDisabled() {
        FilterResourceContributor contributor = new FilterResourceContributor(List.of("test.key"));

        contributor.contribute(resourceBuilder);

        ArgumentCaptor<Predicate<AttributeKey<?>>> predicateCaptor = ArgumentCaptor.forClass(Predicate.class);
        verify(resourceBuilder).removeIf(predicateCaptor.capture());

        Predicate<AttributeKey<?>> predicate = predicateCaptor.getValue();
        assertThat(predicate.test(AttributeKey.stringKey("other.key"))).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldNotRemoveKeysWhenDisabledKeysIsEmpty() {
        FilterResourceContributor contributor = new FilterResourceContributor(Collections.emptyList());

        contributor.contribute(resourceBuilder);

        ArgumentCaptor<Predicate<AttributeKey<?>>> predicateCaptor = ArgumentCaptor.forClass(Predicate.class);
        verify(resourceBuilder).removeIf(predicateCaptor.capture());

        Predicate<AttributeKey<?>> predicate = predicateCaptor.getValue();
        assertThat(predicate.test(AttributeKey.stringKey("test.key"))).isFalse();
    }

    @Test
    void shouldFailWhenNullDisabledKeys() {
        assertThatThrownBy(() -> new FilterResourceContributor(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("disabledKeys cannot be null");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldRemoveMultipleDisabledKeys() {
        FilterResourceContributor contributor = new FilterResourceContributor(List.of("test.key1", "test.key2"));

        contributor.contribute(resourceBuilder);

        ArgumentCaptor<Predicate<AttributeKey<?>>> predicateCaptor = ArgumentCaptor.forClass(Predicate.class);
        verify(resourceBuilder).removeIf(predicateCaptor.capture());

        Predicate<AttributeKey<?>> predicate = predicateCaptor.getValue();
        assertThat(predicate.test(AttributeKey.stringKey("test.key1"))).isTrue();
        assertThat(predicate.test(AttributeKey.stringKey("test.key2"))).isTrue();
        assertThat(predicate.test(AttributeKey.stringKey("other.key"))).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldHandleDifferentAttributeKeyTypes() {
        FilterResourceContributor contributor = new FilterResourceContributor(List.of("test.key"));

        contributor.contribute(resourceBuilder);

        ArgumentCaptor<Predicate<AttributeKey<?>>> predicateCaptor = ArgumentCaptor.forClass(Predicate.class);
        verify(resourceBuilder).removeIf(predicateCaptor.capture());

        Predicate<AttributeKey<?>> predicate = predicateCaptor.getValue();
        assertThat(predicate.test(AttributeKey.stringKey("test.key"))).isTrue();
        assertThat(predicate.test(AttributeKey.longKey("test.key"))).isTrue();
        assertThat(predicate.test(AttributeKey.booleanKey("test.key"))).isTrue();
        assertThat(predicate.test(AttributeKey.doubleKey("test.key"))).isTrue();
    }

}
