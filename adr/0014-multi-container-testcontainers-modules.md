# 14. Multi-container Testcontainers modules

Date: 2026-05-09

## Status

Accepted

## Context

Some of the services Arconia provides a Testcontainers module for (see [13. Testcontainers modules](0013-testcontainers-modules.md)) need other services to run. OpenLIT stores the telemetry in ClickHouse. Langfuse runs as a web application and a worker, and needs PostgreSQL, ClickHouse, Redis, and S3-compatible storage.

Testcontainers can run such a setup from a Docker Compose file, but a Compose setup is not a container the application can connect to: Dev Services and Spring Boot service connections expect one container per service. Asking users to define each supporting container and wire them together would repeat the same setup in every project, and every change in the requirements of the service would break it.

## Decision

A Testcontainers module for such a service provides one container that represents the service and owns the containers it needs. Starting it starts the supporting containers first, independent ones in parallel, and then the service. A part that depends on the service itself starts after it, as the Langfuse worker does once Langfuse has run its database migrations. The container is ready only when every part of the service is ready. Stopping it stops all of them, and so does a failed start, so that no supporting container outlives the service.

The supporting containers join the network set on the container, such as the one shared by the Dev Services, and the container never removes a network it didn't create. Without one, it creates a private network and removes it when it stops. When the container is reused across runs through Testcontainers, the supporting containers are reused too, on a named network that stays after they stop, so that they find each other again on the next run. The service reaches its supporting containers through fixed network aliases, so one instance runs per network.

The container is the only part of the setup users configure. Its environment variables configure every part of the service, such as both the Langfuse web application and its worker, and take precedence over the defaults. The defaults that depend on the supporting containers, such as a mapped port, are computed again on every start. Users choose the image of each supporting container, while the supporting containers themselves are not part of the API.

## Consequences

A service that needs several containers works as a single container in plain Testcontainers code, in Spring Boot service connections, and in Dev Services. The supporting containers can change, for example to replace the storage of Langfuse, without breaking the projects using the module.

Users can't configure the supporting containers beyond their images. Running the service takes the resources of all its containers, and starting it takes as long as its slowest path. A reused setup leaves its named network behind, which users remove when they no longer need the containers.
