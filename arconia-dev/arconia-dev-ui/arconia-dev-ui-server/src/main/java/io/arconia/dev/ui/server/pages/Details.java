package io.arconia.dev.ui.server.pages;

import java.util.List;

import io.arconia.dev.ui.api.page.InfoItem;

/**
 * Creates the details of the rows of the pages of the Dev UI, each with its icon.
 */
final class Details {

    private Details() {}

    /**
     * A detail that reads as text, such as a description.
     */
    static InfoItem text(String label, String icon, String value) {
        return InfoItem.builder().label(label).value(value).icon(icon).build();
    }

    /**
     * A detail that is code, such as the name of a class or the value of a property.
     */
    static InfoItem code(String label, String icon, String value) {
        return code(label, icon, List.of(value));
    }

    static InfoItem code(String label, String icon, List<String> values) {
        return InfoItem.builder().label(label).values(values).icon(icon).code(true).build();
    }

}
