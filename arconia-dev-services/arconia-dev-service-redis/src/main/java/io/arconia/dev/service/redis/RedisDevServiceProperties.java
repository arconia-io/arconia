package io.arconia.dev.service.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for the Redis Dev Service.
 */
@ConfigurationProperties(prefix = RedisDevServiceProperties.CONFIG_PREFIX)
public class RedisDevServiceProperties {

    public static final String CONFIG_PREFIX = "arconia.dev.services.redis";

    /**
     * Whether the dev service is enabled.
     */
    private boolean enabled = true;

    /**
     * The edition of Redis to use.
     */
    private Edition edition = Edition.COMMUNITY;

    private final Community community = new Community();

    private final Stack stack = new Stack();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Edition getEdition() {
        return edition;
    }

    public void setEdition(Edition edition) {
        this.edition = edition;
    }

    public Community getCommunity() {
        return community;
    }

    public Stack getStack() {
        return stack;
    }

    /**
     * Configuration for the community edition of Redis.
     */
    public static class Community {

        /**
         * Full name of the container image used in the dev service.
         */
        private String imageName = "redis:7.4-alpine";

        /**
         * Whether the container used in the dev service is reusable across applications.
         */
        private boolean reusable = false;

        public String getImageName() {
            return imageName;
        }

        public void setImageName(String imageName) {
            this.imageName = imageName;
        }

        public boolean isReusable() {
            return reusable;
        }

        public void setReusable(boolean reusable) {
            this.reusable = reusable;
        }

    }

    /**
     * Configuration for the stack edition of Redis.
     */
    public static class Stack {

        /**
         * Full name of the container image used in the dev service.
         */
        private String imageName = "redis/redis-stack-server:7.4.0-v3";

        /**
         * Whether the container used in the dev service is reusable across applications.
         */
        private boolean reusable = false;

        public String getImageName() {
            return imageName;
        }

        public void setImageName(String imageName) {
            this.imageName = imageName;
        }

        public boolean isReusable() {
            return reusable;
        }

        public void setReusable(boolean reusable) {
            this.reusable = reusable;
        }

    }

    /**
     * The edition of Redis to use.
     */
    public enum Edition {
        COMMUNITY,
        STACK;
    }

}
