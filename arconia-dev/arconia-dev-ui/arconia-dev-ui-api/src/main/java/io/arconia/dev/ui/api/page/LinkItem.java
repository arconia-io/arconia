package io.arconia.dev.ui.api.page;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

import org.springframework.util.Assert;

import io.arconia.core.annotations.Incubating;

/**
 * An item of a {@link Card} that links to a page outside the Dev UI, such as the console of
 * a service or an endpoint of the application.
 */
@Incubating
public final class LinkItem implements CardItem {

    private final String label;

    private final String url;

    private LinkItem(String label, String url) {
        Assert.hasText(label, "label cannot be null or empty");
        Assert.hasText(url, () -> "url of the link '%s' cannot be null or empty".formatted(label));
        Assert.isTrue(isHttpUrl(url) || isApplicationPath(url),
                () -> "url '%s' of the link '%s' must be an HTTP or HTTPS URL, or a path that starts with a slash".formatted(url, label));
        this.label = label;
        this.url = url;
    }

    /**
     * Creates a link.
     *
     * @param label the label of the link (e.g. {@code Grafana})
     * @param url where the link points to: an HTTP or HTTPS URL, or a path of the application
     * itself that starts with a slash (e.g. {@code /swagger-ui.html}), which is taken from its
     * context path
     */
    public static LinkItem of(String label, String url) {
        return new LinkItem(label, url);
    }

    /**
     * The label of the link (e.g. {@code Grafana}).
     */
    @Override
    public String getLabel() {
        return label;
    }

    /**
     * Where the link points to: an HTTP or HTTPS URL, or a path of the application itself that
     * starts with a slash.
     */
    public String getUrl() {
        return url;
    }

    private static boolean isApplicationPath(String url) {
        return url.startsWith("/") && !url.startsWith("//") && !url.contains("\\");
    }

    static boolean isHttpUrl(String url) {
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            return uri.getHost() != null && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme));
        } catch (URISyntaxException ex) {
            return false;
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof LinkItem item && this.label.equals(item.label) && this.url.equals(item.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, url);
    }

    @Override
    public String toString() {
        return "LinkItem[label=%s, url=%s]".formatted(label, url);
    }

}
