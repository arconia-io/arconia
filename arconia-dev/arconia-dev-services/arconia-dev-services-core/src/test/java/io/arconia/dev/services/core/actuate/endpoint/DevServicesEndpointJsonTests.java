package io.arconia.dev.services.core.actuate.endpoint;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.boot.autoconfigure.ssl.PemSslBundleProperties;
import org.springframework.boot.autoconfigure.ssl.PropertiesSslBundle;
import org.springframework.boot.info.SslInfo;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import tools.jackson.databind.json.JsonMapper;

import io.arconia.dev.services.api.registration.ContainerInfo;
import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.core.TestSslDevServicesProperties;
import io.arconia.dev.services.core.actuate.endpoint.DevServicesEndpoint.ServiceInfo;

/**
 * JSON serialization tests for {@link DevServicesEndpoint}.
 */
class DevServicesEndpointJsonTests {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void serviceInfoSerializesAsDocumented() throws Exception {
        ContainerInfo containerInfo = ContainerInfo.builder()
                .id("cbf5a15cf542")
                .imageName("postgres:18")
                .names(List.of("unwritten_mithrandir"))
                .ports(List.of(new ContainerInfo.ContainerPort("0.0.0.0", 5432, 36803, "tcp")))
                .labels(Map.of("io.arconia.dev-service.name", "postgresql"))
                .state("running")
                .status("Up About 67 minutes")
                .build();
        ServiceInfo serviceInfo = new ServiceInfo("postgresql", "PostgreSQL Dev Service", DevServiceRegistration.Origin.MANAGED,
                List.of(new DevServiceLink("Console", "http://localhost:36803/console")), containerInfo, null);

        JSONAssert.assertEquals("""
                {
                  "name": "postgresql",
                  "description": "PostgreSQL Dev Service",
                  "origin": "MANAGED",
                  "links": [
                    {
                      "label": "Console",
                      "url": "http://localhost:36803/console"
                    }
                  ],
                  "containerInfo": {
                    "id": "cbf5a15cf542",
                    "imageName": "postgres:18",
                    "names": [
                      "unwritten_mithrandir"
                    ],
                    "ports": [
                      {
                        "ip": "0.0.0.0",
                        "privatePort": 5432,
                        "publicPort": 36803,
                        "type": "tcp"
                      }
                    ],
                    "labels": {
                      "io.arconia.dev-service.name": "postgresql"
                    },
                    "state": "running",
                    "status": "Up About 67 minutes"
                  },
                  "ssl": null
                }
                """, jsonMapper.writeValueAsString(serviceInfo), JSONCompareMode.STRICT);
    }

    @Test
    void containerInfoIsNullWhenTheContainerCannotBeInspected() throws Exception {
        ServiceInfo serviceInfo = new ServiceInfo("postgresql", "PostgreSQL Dev Service", DevServiceRegistration.Origin.DISCOVERED, List.of(), null, null);

        JSONAssert.assertEquals("""
                {
                  "name": "postgresql",
                  "description": "PostgreSQL Dev Service",
                  "origin": "DISCOVERED",
                  "links": [],
                  "containerInfo": null,
                  "ssl": null
                }
                """, jsonMapper.writeValueAsString(serviceInfo), JSONCompareMode.STRICT);
    }

    @Test
    void sslSerializesAsSpringBootReportsBundles() throws Exception {
        // The bundle registered for a dev service trusts the certificate it presents, and holds no key.
        PemSslBundleProperties properties = new PemSslBundleProperties();
        properties.getTruststore().setCertificate(TestSslDevServicesProperties.CERTIFICATE);
        var sslBundles = new DefaultSslBundleRegistry();
        sslBundles.registerBundle("dev-services-redis", PropertiesSslBundle.get(properties));
        ServiceInfo serviceInfo = new ServiceInfo("redis", "Redis Dev Service", DevServiceRegistration.Origin.MANAGED, List.of(), null,
                new SslInfo(sslBundles).getBundle("dev-services-redis"));

        // The shape is the one of the SSL information in the info endpoint; the certificate details vary.
        JSONAssert.assertEquals("""
                {
                  "name": "redis",
                  "ssl": {
                    "name": "dev-services-redis",
                    "certificateChains": [],
                    "trustStoreCertificateChains": [
                      {
                        "alias": "ssl",
                        "certificates": [
                          {
                            "subject": "CN=localhost,O=Arconia",
                            "issuer": "CN=Arconia Dev Services Test CA,O=Arconia",
                            "validity": {
                              "status": "VALID"
                            }
                          }
                        ]
                      }
                    ]
                  }
                }
                """, jsonMapper.writeValueAsString(serviceInfo), JSONCompareMode.LENIENT);
    }

}
