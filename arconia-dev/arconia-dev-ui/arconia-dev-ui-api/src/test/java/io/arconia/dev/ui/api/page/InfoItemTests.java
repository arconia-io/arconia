package io.arconia.dev.ui.api.page;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link InfoItem}.
 */
class InfoItemTests {

    @Test
    void itemCanHaveSeveralValues() {
        InfoItem item = InfoItem.of("Ports", List.of("3000 → 53000", "4318 → 54318"));

        assertThat(item.getLabel()).isEqualTo("Ports");
        assertThat(item.getValues()).containsExactly("3000 → 53000", "4318 → 54318");
    }

    @Test
    void itemHasNoIconAndValuesThatAreNotCodeByDefault() {
        assertThat(InfoItem.of("Image", "postgres")).isEqualTo(InfoItem.builder().label("Image").value("postgres").build());
        assertThat(InfoItem.of("Image", "postgres").getIcon()).isNull();
        assertThat(InfoItem.of("Image", "postgres").isCode()).isFalse();
    }

    @Test
    void itemCanBeBuiltWithAnIconAndValuesThatAreCode() {
        InfoItem item = InfoItem.builder().label("Ports").values(List.of("3000 → 53000", "4318 → 54318")).icon("network").code(true).build();

        assertThat(item.getLabel()).isEqualTo("Ports");
        assertThat(item.getValues()).containsExactly("3000 → 53000", "4318 → 54318");
        assertThat(item.getIcon()).isEqualTo("network");
        assertThat(item.isCode()).isTrue();
    }

    @Test
    void whenTheIconIsNotANameThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> InfoItem.builder().label("Image").value("postgres").icon("My Icon").build())
                .withMessage("icon 'My Icon' can only contain lowercase letters, digits, and single hyphens between them");
    }

    @Test
    void whenTheBuilderHasNoLabelThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> InfoItem.builder().value("postgres").build())
                .withMessage("label cannot be null or empty");
    }

    @Test
    void whenTheBuilderHasNoValuesThenTheItemHasNone() {
        assertThat(InfoItem.builder().label("Image").build().getValues()).isEmpty();
        assertThat(InfoItem.builder().label("Image").value(null).build().getValues()).isEmpty();
    }

    @Test
    void itemCanBeCreatedWithSingleValue() {
        InfoItem item = InfoItem.of("Image", "postgres");

        assertThat(item.getLabel()).isEqualTo("Image");
        assertThat(item.getValues()).containsExactly("postgres");
    }

    @Test
    void whenTheSingleValueIsNullThenTheItemHasNone() {
        assertThat(InfoItem.of("Image", (String) null).getValues()).isEmpty();
    }

    @Test
    void valuesAreCopied() {
        List<String> values = new ArrayList<>(List.of("postgres"));
        InfoItem item = InfoItem.of("Image", values);

        values.clear();

        assertThat(item.getValues()).containsExactly("postgres");
    }

    @Test
    void whenLabelIsEmptyThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> InfoItem.of("", "postgres"))
                .withMessage("label cannot be null or empty");
    }

    @Test
    void whenThereAreNoValuesThenTheItemHasNone() {
        assertThat(InfoItem.of("Image", List.of()).getValues()).isEmpty();
        assertThat(InfoItem.of("Image", (List<String>) null).getValues()).isEmpty();
    }

    @Test
    void valuesThatAreNullAreLeftOut() {
        assertThat(InfoItem.of("Ports", Arrays.asList("5432", null, "8080")).getValues()).containsExactly("5432", "8080");
    }

}
