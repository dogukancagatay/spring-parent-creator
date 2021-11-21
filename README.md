# spring-parent-creator

A custom parent project creator for projects using Spring Boot.

## Why do we need this?

When using microservices architecture with multiple Java projects (libraries, microservices), you may need some shared objects(plugin setup, dependency versions, etc.) on your project structure.

Having a shared parent project that resembles `spring-boot-starter-parent` is a good starting point.

## Build

Build an executable JAR.

```shell
./mvnw package
```

## Running

You can create parent project poms with specified Spring Boot version.

```shell
java -jar -Dspring_boot.version=2.5.2 target/parent-creator-1.0.0.jar
```

### Runtime Parameters

Following parameters can be set.

- `spring_boot.version`: Default: *2.3.3.RELEASE*
- `output.filename`: Default: *new_parent_pom.xml*
- `maven.repository.url`: Default: *https://repo1.maven.org/maven2*
