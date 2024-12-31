# Arconia

Arconia is a framework to build modern applications using Java and Spring Boot.
It provides support for multitenancy and AI.

<img src="arconia-logo.png" alt="The Arconia logo" height="250px" />

## 🚀&nbsp; Quick Start

### Pre-Requisites

* Java 21+
* [Spring CLI](https://docs.spring.io/spring-cli/reference/installation.html)

### Getting Started

Using the Spring CLI, you can easily bootstrap a new Spring Boot application using the Arconia framework.

First, add the Arconia Spring catalog providing the project templates.

```shell
spring project-catalog add arconia https://github.com/arconia-io/arconia
```

Then, create a new Spring Boot project for building a multitenant web application.

```shell
spring boot new myapp arconia-web
```

Finally, navigate to the `myapp` folder and run the Spring Boot application.

```shell
cd myapp
./mvnw spring-boot:run
```

You can now call the application as one of the valid tenants (`dukes` or `beans`).
This example uses [httpie](https://httpie.io) to send HTTP requests.

```shell
http :8080/ X-TenantId:dukes
```

## 🌟 Examples

Check these [examples](https://github.com/arconia-io/arconia-samples) to see Arconia and Spring Boot in action.

## 🛡️&nbsp; Security

The security process for reporting vulnerabilities is described in [SECURITY.md](SECURITY.md).

## 🖊️&nbsp; License

This project is licensed under the **Apache License 2.0**. See [LICENSE](LICENSE) for more information.
