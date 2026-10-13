package io.arconia.dev.services.keycloak;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.arconia.dev.services.api.config.BaseDevServicesProperties;
import io.arconia.dev.services.api.config.ResourceMapping;
import io.arconia.dev.services.api.config.ReuseStrategy;
import io.arconia.dev.services.api.config.VolumeMapping;

/**
 * Properties for the Keycloak Dev Services.
 */
@ConfigurationProperties(prefix = KeycloakDevServicesProperties.CONFIG_PREFIX)
public class KeycloakDevServicesProperties implements BaseDevServicesProperties {

    public static final String CONFIG_PREFIX = "arconia.dev.services.keycloak";

    static final String DEFAULT_REALM = "arconia";

    static final String DEFAULT_ADMIN_USERNAME = "arconia";
    static final String DEFAULT_ADMIN_PASSWORD = "arconia";

    /**
     * Whether the dev service is enabled.
     */
    private boolean enabled = true;

    /**
     * Full name of the container image used in the dev service.
     */
    private String imageName = "quay.io/keycloak/keycloak:26.8";

    /**
     * Environment variables to set in the service.
     */
    private Map<String,String> environment = new HashMap<>();

    /**
     * Additional hostnames the dev service container is reachable by on the shared network,
     * next to the name of the dev service. Only applicable when the shared network is enabled.
     */
    private List<String> networkAliases = new ArrayList<>();

    /**
     * Fixed port for exposing the Keycloak HTTP port to the host.
     * When it's 0 (default), a random available port is assigned dynamically.
     */
    private int port = 0;

    /**
     * Resources from the classpath or host filesystem to copy into the container.
     * They can be files or directories that will be copied to the specified
     * destination path inside the container at startup and are immutable (read-only).
     */
    private List<ResourceMapping> resources = new ArrayList<>();

    /**
     * Strategy for reusing a running container for the dev service across applications or restarts.
     * Only applicable in dev mode.
     */
    private ReuseStrategy reuseStrategy = ReuseStrategy.FRAMEWORK;

    /**
     * Maximum waiting time for the service to start, counted from the container start.
     */
    private Duration startupTimeout = Duration.ofMinutes(2);

    /**
     * Files or directories to mount from the host filesystem into the container.
     * They are mounted at the specified destination path inside the container
     * at startup and are mutable (read-write). Changes in either the host
     * or the container will be immediately reflected in the other.
     */
    private List<VolumeMapping> volumes = new ArrayList<>();

    /**
     * Username of the Keycloak administrator, used to access the Admin Console.
     */
    private String adminUsername = DEFAULT_ADMIN_USERNAME;

    /**
     * Password of the Keycloak administrator, used to access the Admin Console.
     */
    private String adminPassword = DEFAULT_ADMIN_PASSWORD;

    /**
     * The realm the application connects to.
     */
    private Realm realm = new Realm();

    /**
     * The OAuth2 client the application authenticates as.
     */
    private Client client = new Client();

    /**
     * The OAuth2 resource server wiring.
     */
    private ResourceServer resourceServer = new ResourceServer();

    /**
     * Ports on the host that Keycloak can reach from inside its container, such as for
     * OpenID Connect Back-Channel Logout.
     */
    private List<Integer> hostAccessiblePorts = new ArrayList<>();

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    @Override
    public Map<String, String> getEnvironment() {
        return environment;
    }

    public void setEnvironment(Map<String, String> environment) {
        this.environment = environment;
    }

    @Override
    public List<String> getNetworkAliases() {
        return networkAliases;
    }

    public void setNetworkAliases(List<String> networkAliases) {
        this.networkAliases = networkAliases;
    }

    @Override
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public List<ResourceMapping> getResources() {
        return resources;
    }

    public void setResources(List<ResourceMapping> resources) {
        this.resources = resources;
    }

    @Override
    public ReuseStrategy getReuseStrategy() {
        return reuseStrategy;
    }

    public void setReuseStrategy(ReuseStrategy reuseStrategy) {
        this.reuseStrategy = reuseStrategy;
    }

    @Override
    public Duration getStartupTimeout() {
        return startupTimeout;
    }

    public void setStartupTimeout(Duration startupTimeout) {
        this.startupTimeout = startupTimeout;
    }

    @Override
    public List<VolumeMapping> getVolumes() {
        return volumes;
    }

    public void setVolumes(List<VolumeMapping> volumes) {
        this.volumes = volumes;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public Realm getRealm() {
        return realm;
    }

    public void setRealm(Realm realm) {
        this.realm = realm;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public ResourceServer getResourceServer() {
        return resourceServer;
    }

    public void setResourceServer(ResourceServer resourceServer) {
        this.resourceServer = resourceServer;
    }

    public List<Integer> getHostAccessiblePorts() {
        return hostAccessiblePorts;
    }

    public void setHostAccessiblePorts(List<Integer> hostAccessiblePorts) {
        this.hostAccessiblePorts = hostAccessiblePorts;
    }

    /**
     * The realm the application connects to: either generated from this configuration or
     * imported from a realm file.
     */
    public static class Realm {

        /**
         * Name of the generated realm. Defaults to `arconia`.
         */
        private String name = "";

        /**
         * Path to a Keycloak realm file to import instead of generating a realm, resolved from
         * the classpath or the host filesystem. It can be prefixed with `classpath:` or `file:`.
         */
        private String importPath = "";

        /**
         * Users of the generated realm, by username. Defaults to `isabella` (roles `admin` and
         * `user`) and `bjorn` (role `user`), each with their username as the password.
         */
        private Map<String,User> users = new LinkedHashMap<>();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getImportPath() {
            return importPath;
        }

        public void setImportPath(String importPath) {
            this.importPath = importPath;
        }

        public Map<String,User> getUsers() {
            return users;
        }

        public void setUsers(Map<String,User> users) {
            this.users = users;
        }

        /**
         * A user of the generated realm.
         */
        public static class User {

            /**
             * Password of the user.
             */
            private String password = "";

            /**
             * Realm roles held by the user.
             */
            private List<String> roles = new ArrayList<>();

            public String getPassword() {
                return password;
            }

            public void setPassword(String password) {
                this.password = password;
            }

            public List<String> getRoles() {
                return roles;
            }

            public void setRoles(List<String> roles) {
                this.roles = roles;
            }

        }

    }

    /**
     * The OAuth2 client declared in the generated realm and, when Spring Security's OAuth2
     * Client is on the classpath, registered with Spring Security.
     */
    public static class Client {

        static final String DEFAULT_CLIENT_ID = "arconia-app";

        static final String DEFAULT_CLIENT_SECRET = "arconia-secret";

        /**
         * Whether the dev service declares the client in the generated realm and registers it
         * with Spring Security.
         */
        private boolean enabled = true;

        /**
         * Identifier of the client. With an imported realm, the realm file must declare it.
         */
        private String id = DEFAULT_CLIENT_ID;

        /**
         * Secret of the client.
         */
        private String secret = DEFAULT_CLIENT_SECRET;

        /**
         * Identifier of the Spring Security client registration, as used in the login URL
         * `/oauth2/authorization/{registration-id}`.
         */
        private String registrationId = "keycloak";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public String getRegistrationId() {
            return registrationId;
        }

        public void setRegistrationId(String registrationId) {
            this.registrationId = registrationId;
        }

    }

    /**
     * The OAuth2 resource server wiring, applied when Spring Security's OAuth2 Resource Server
     * is on the classpath.
     */
    public static class ResourceServer {

        /**
         * Whether the resource server is configured to validate the tokens issued by the
         * dev service.
         */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

    }

}
