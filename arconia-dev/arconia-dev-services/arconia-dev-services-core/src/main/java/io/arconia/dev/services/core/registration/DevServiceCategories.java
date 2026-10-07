package io.arconia.dev.services.core.registration;

import io.arconia.core.support.Incubating;

/**
 * The categories of mutually exclusive dev services: only one dev service per category can be
 * active in an application.
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
