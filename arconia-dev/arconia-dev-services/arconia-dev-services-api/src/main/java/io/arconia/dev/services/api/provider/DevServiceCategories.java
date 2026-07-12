package io.arconia.dev.services.api.provider;

import io.arconia.core.support.Incubating;

/**
 * Category constants for groups of mutually exclusive dev services.
 */
@Incubating
public final class DevServiceCategories {

    public static final String AWS = "aws";

    public static final String JDBC = "jdbc";

    public static final String MONGODB = "mongodb";

    public static final String OPENTELEMETRY = "opentelemetry";

    public static final String REDIS = "redis";

    private DevServiceCategories() {}

}
