package io.arconia.dev.services.api.registration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;

/**
 * Labels applied to every container started by Arconia Dev Services,
 * making dev service containers identifiable across applications
 * (e.g. {@code docker ps --filter label=io.arconia.dev-service.name}).
 * <p>
 * Shared dev services rely on these labels to discover running containers
 * started by other applications.
 */
@Incubating
public final class DevServiceLabels {

    /**
     * The logical name of the dev service the container belongs to (e.g. {@code postgresql}).
     */
    public static final String NAME = "io.arconia.dev-service.name";

    /**
     * Whether the container is shared among applications and can be discovered by them (e.g. {@code true}).
     */
    public static final String SHARED = "io.arconia.dev-service.shared";

    /**
     * Identifier of the application instance that started the container,
     * used to prevent an application from discovering its own containers.
     */
    public static final String OWNER = "io.arconia.dev-service.owner";

    /**
     * Comma-separated list of network aliases the container is reachable by on the
     * dev services network (e.g. {@code lgtm}). Set only when the container joins a
     * network, so other applications can discover the hostname to wire against.
     */
    public static final String NETWORK_ALIASES = "io.arconia.dev-service.network-aliases";

    /**
     * Prefix of the labels describing the links the dev service exposes. Each link
     * contributes one label per field, named after the link id and the field
     * (e.g. {@code io.arconia.dev-service.link.grafana.port}), so that a reader can pick
     * out a single value and a later version can add fields without breaking older readers.
     * Ports are recorded as the container sees them, since no port is mapped when the
     * labels are applied.
     */
    public static final String LINK_PREFIX = "io.arconia.dev-service.link.";

    private static final String LINK_LABEL_SUFFIX = ".label";

    private static final String LINK_SCHEME_SUFFIX = ".scheme";

    private static final String LINK_PORT_SUFFIX = ".port";

    private static final String LINK_PATH_SUFFIX = ".path";

    private static final String OWNER_ID = UUID.randomUUID().toString();

    private DevServiceLabels() {}

    /**
     * The identifier of this application instance, used as the value of the {@link #OWNER} label.
     * The identifier is unique per JVM and stable across Spring Boot DevTools restarts.
     */
    public static String ownerId() {
        return OWNER_ID;
    }

    /**
     * The labels describing the given link definition, keyed by label name.
     */
    public static Map<String, String> linkLabels(DevServiceLinkDefinition link) {
        Assert.notNull(link, "link cannot be null");
        return Map.of(
                LINK_PREFIX + link.id() + LINK_LABEL_SUFFIX, link.label(),
                LINK_PREFIX + link.id() + LINK_SCHEME_SUFFIX, link.scheme(),
                LINK_PREFIX + link.id() + LINK_PORT_SUFFIX, String.valueOf(link.port()),
                LINK_PREFIX + link.id() + LINK_PATH_SUFFIX, link.path());
    }

    /**
     * The link definitions described by the given container labels, ordered by link id.
     * <p>
     * A container is adopted as it runs, so its labels are not necessarily written by the
     * version of Arconia reading them. Labels that don't describe a link are ignored, fields
     * a newer version may have added are ignored, and a link missing a field or holding a
     * value this version cannot make sense of is left out rather than failing the lookup.
     * Ordering by id keeps an adopted dev service reporting its links consistently, since
     * container labels carry no order of their own.
     */
    public static List<DevServiceLinkDefinition> linksFrom(Map<String, String> labels) {
        Assert.notNull(labels, "labels cannot be null");
        Set<String> ids = labels.keySet().stream()
                .filter(name -> name.startsWith(LINK_PREFIX) && name.endsWith(LINK_PORT_SUFFIX))
                .map(name -> name.substring(LINK_PREFIX.length(), name.length() - LINK_PORT_SUFFIX.length()))
                .collect(Collectors.toCollection(TreeSet::new));

        List<DevServiceLinkDefinition> links = new ArrayList<>();
        for (String id : ids) {
            try {
                String scheme = labels.get(LINK_PREFIX + id + LINK_SCHEME_SUFFIX);
                String path = labels.get(LINK_PREFIX + id + LINK_PATH_SUFFIX);
                links.add(DevServiceLinkDefinition.builder()
                        .id(id)
                        .label(labels.get(LINK_PREFIX + id + LINK_LABEL_SUFFIX))
                        .scheme(scheme != null ? scheme : DevServiceLinkDefinition.DEFAULT_SCHEME)
                        .port(Integer.parseInt(labels.get(LINK_PREFIX + id + LINK_PORT_SUFFIX)))
                        .path(path != null ? path : "")
                        .build());
            } catch (RuntimeException ex) {
                // Ignore a malformed link rather than failing the whole lookup.
            }
        }
        return List.copyOf(links);
    }

}
