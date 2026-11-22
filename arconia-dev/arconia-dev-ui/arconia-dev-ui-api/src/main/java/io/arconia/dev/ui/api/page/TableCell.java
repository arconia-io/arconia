package io.arconia.dev.ui.api.page;

import java.util.Objects;

import org.springframework.util.Assert;

import io.arconia.core.annotations.Incubating;

/**
 * A cell of a {@link TableRow}: a text, and what the table needs to know to show it.
 */
@Incubating
public final class TableCell {

    private final String text;

    private final boolean secret;

    private TableCell(String text, boolean secret) {
        Assert.notNull(text, "text cannot be null");
        this.text = text;
        this.secret = secret;
    }

    /**
     * Creates a cell that shows a text, which can be empty.
     */
    public static TableCell text(String text) {
        return new TableCell(text, false);
    }

    /**
     * Creates a cell that holds a text not meant for everyone looking at the screen, such as
     * a password. The text is hidden until the developer asks to see it, and it is left out
     * when the table is filtered or sorted.
     */
    public static TableCell secret(String text) {
        return new TableCell(text, true);
    }

    /**
     * The text of the cell.
     */
    public String getText() {
        return text;
    }

    /**
     * Whether the text is hidden until the developer asks to see it.
     */
    public boolean isSecret() {
        return secret;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TableCell cell && this.secret == cell.secret && this.text.equals(cell.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, secret);
    }

    /**
     * The text of a secret cell is left out, so that it doesn't end up in a log.
     */
    @Override
    public String toString() {
        return "TableCell[text=%s, secret=%s]".formatted(secret ? "****" : text, secret);
    }

}
