package io.arconia.docling.client.convert.response;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ErrorItem}.
 */
class ErrorItemTests {

    @Test
    void createErrorItemWithAllFields() {
        String componentType = "parser";
        String errorMessage = "Failed to parse document structure";
        String moduleName = "docling.core.parser";

        ErrorItem errorItem = new ErrorItem(
                componentType,
                errorMessage,
                moduleName
        );

        assertThat(errorItem.componentType()).isEqualTo(componentType);
        assertThat(errorItem.errorMessage()).isEqualTo(errorMessage);
        assertThat(errorItem.moduleName()).isEqualTo(moduleName);
    }

    @Test
    void createErrorItemWithNullFields() {
        ErrorItem errorItem = new ErrorItem(
                null,
                null,
                null
        );

        assertThat(errorItem.componentType()).isNull();
        assertThat(errorItem.errorMessage()).isNull();
        assertThat(errorItem.moduleName()).isNull();
    }

    @Test
    void createErrorItemWithEmptyFields() {
        String componentType = "";
        String errorMessage = "";
        String moduleName = "";

        ErrorItem errorItem = new ErrorItem(
                componentType,
                errorMessage,
                moduleName
        );

        assertThat(errorItem.componentType()).isEmpty();
        assertThat(errorItem.errorMessage()).isEmpty();
        assertThat(errorItem.moduleName()).isEmpty();
    }

}
