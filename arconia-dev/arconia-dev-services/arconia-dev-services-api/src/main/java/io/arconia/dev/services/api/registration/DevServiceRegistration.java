package io.arconia.dev.services.api.registration;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import io.arconia.core.annotations.Incubating;

/**
 * Describes a registered dev service.
 */
@Incubating
public final class DevServiceRegistration {

    private static final Pattern NAME_PATTERN = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*+");

    private final String name;

    private final String title;

    @Nullable
    private final String description;

    @Nullable
    private final String documentation;

    @Nullable
    private final String propertiesPrefix;

    @Nullable
    private final String icon;

    @Nullable
    private final String category;

    private final Origin origin;

    private final Supplier<ContainerInfo> containerInfo;

    private final List<DevServiceLink> links;

    @Nullable
    private final String sslBundleName;

    private DevServiceRegistration(Builder builder) {
        Assert.hasText(builder.name, "name cannot be null or empty");
        Assert.isTrue(NAME_PATTERN.matcher(builder.name).matches(),
                "name must contain only lowercase letters, digits, and single dashes between them: " + builder.name);
        Assert.hasText(builder.title, "title cannot be null or empty");
        Assert.isTrue(builder.description == null || !builder.description.isBlank(), "description cannot be empty");
        Assert.isTrue(builder.documentation == null || isHttpUrl(builder.documentation), "documentation must be an HTTP or HTTPS URL");
        Assert.isTrue(builder.propertiesPrefix == null || !builder.propertiesPrefix.isBlank(), "propertiesPrefix cannot be empty");
        Assert.isTrue(builder.icon == null || NAME_PATTERN.matcher(builder.icon).matches(),
                () -> "icon must contain only lowercase letters, digits, and single dashes between them: " + builder.icon);
        Assert.isTrue(builder.category == null || !builder.category.isBlank(), "category cannot be empty");
        Assert.notNull(builder.origin, "origin cannot be null");
        Assert.notNull(builder.containerInfo, "containerInfo cannot be null");
        Assert.notNull(builder.links, "links cannot be null");
        this.name = builder.name;
        this.title = builder.title;
        this.description = builder.description;
        this.documentation = builder.documentation;
        this.propertiesPrefix = builder.propertiesPrefix;
        this.icon = builder.icon;
        this.category = builder.category;
        this.origin = builder.origin;
        this.containerInfo = builder.containerInfo;
        this.links = List.copyOf(builder.links);
        this.sslBundleName = builder.sslBundleName;
    }

    /**
     * The logical name of the dev service.
     */
    public String getName() {
        return name;
    }

    /**
     * The title of the dev service, which is the name of the product it runs, such as
     * {@code PostgreSQL}.
     */
    public String getTitle() {
        return title;
    }

    /**
     * What the dev service provides, in a sentence, if there is more to say than its title.
     */
    @Nullable
    public String getDescription() {
        return description;
    }

    /**
     * The URL of the documentation of the dev service, if any.
     */
    @Nullable
    public String getDocumentation() {
        return documentation;
    }

    /**
     * What the names of the configuration properties of the dev service start with, such as
     * {@code arconia.dev.services.postgresql}, if known.
     */
    @Nullable
    public String getPropertiesPrefix() {
        return propertiesPrefix;
    }

    /**
     * The name of the icon that developer tooling shows for the dev service, from the
     * <a href="https://lucide.dev/icons">Lucide</a> icon set, if any.
     */
    @Nullable
    public String getIcon() {
        return icon;
    }

    private static boolean isHttpUrl(String url) {
        try {
            URI uri = new URI(url);
            return uri.getHost() != null && ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()));
        }
        catch (URISyntaxException ex) {
            return false;
        }
    }

    /**
     * The category of mutually exclusive dev services the dev service belongs to, if any.
     */
    @Nullable
    public String getCategory() {
        return category;
    }

    /**
     * How the dev service container was made available to the application.
     */
    public Origin getOrigin() {
        return origin;
    }

    /**
     * Supplies the information about the container, inspected on each call.
     */
    public Supplier<ContainerInfo> getContainerInfo() {
        return containerInfo;
    }

    /**
     * The links the dev service exposes.
     */
    public List<DevServiceLink> getLinks() {
        return links;
    }

    /**
     * The name of the SSL bundle the application connects to the dev service with, registered
     * in the {@code SslBundles} of the application, when SSL is enabled with a configured
     * certificate.
     */
    @Nullable
    public String getSslBundleName() {
        return sslBundleName;
    }

    /**
     * How the dev service container was made available to the application.
     */
    public enum Origin {

        /**
         * The container is managed by this application through Testcontainers. The application
         * started it or attached to it when it is a container reused by Testcontainers.
         */
        MANAGED,

        /**
         * The container was started by another application and discovered
         * by this one, which only connects to it.
         */
        DISCOVERED

    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DevServiceRegistration}, with no description, no documentation, no properties
     * prefix, no icon,
     * and no links by default.
     */
    public static final class Builder {

        @Nullable
        private String name;
        @Nullable
        private String title;
        @Nullable
        private String description;
        @Nullable
        private String documentation;
        @Nullable
        private String propertiesPrefix;
        @Nullable
        private String icon;
        @Nullable
        private String category;
        @Nullable
        private Origin origin;
        @Nullable
        private Supplier<ContainerInfo> containerInfo;
        private List<DevServiceLink> links = List.of();
        @Nullable
        private String sslBundleName;

        private Builder() {}

        /**
         * The logical name of the dev service. It must contain only lowercase letters, digits,
         * and single dashes between them.
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * The title of the dev service, which is the name of the product it runs, such as
         * {@code PostgreSQL}.
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * What the dev service provides, in a sentence, if there is more to say than its
         * title.
         */
        public Builder description(@Nullable String description) {
            this.description = description;
            return this;
        }

        /**
         * The URL of the documentation of the dev service, if any.
         */
        public Builder documentation(@Nullable String documentation) {
            this.documentation = documentation;
            return this;
        }

        /**
         * What the names of the configuration properties of the dev service start with, if
         * known.
         */
        public Builder propertiesPrefix(@Nullable String propertiesPrefix) {
            this.propertiesPrefix = propertiesPrefix;
            return this;
        }

        /**
         * The name of the icon that developer tooling shows for the dev service, from the
         * <a href="https://lucide.dev/icons">Lucide</a> icon set, if any.
         */
        public Builder icon(@Nullable String icon) {
            this.icon = icon;
            return this;
        }

        /**
         * The category of mutually exclusive dev services the dev service belongs to, if any.
         */
        public Builder category(@Nullable String category) {
            this.category = category;
            return this;
        }

        /**
         * How the dev service container was made available to the application.
         */
        public Builder origin(Origin origin) {
            this.origin = origin;
            return this;
        }

        /**
         * The supplier of the information about the container, inspected on each call.
         */
        public Builder containerInfo(Supplier<ContainerInfo> containerInfo) {
            this.containerInfo = containerInfo;
            return this;
        }

        /**
         * The links the dev service exposes.
         */
        public Builder links(List<DevServiceLink> links) {
            this.links = links;
            return this;
        }

        /**
         * The name of the SSL bundle the application connects to the dev service with, if any.
         */
        public Builder sslBundleName(@Nullable String sslBundleName) {
            this.sslBundleName = sslBundleName;
            return this;
        }

        /**
         * Builds the {@link DevServiceRegistration} instance.
         */
        public DevServiceRegistration build() {
            return new DevServiceRegistration(this);
        }

    }

}
