# Evergen Project Guidelines

## Build Commands
- Build: `./mvnw clean install`
- Run application: `./mvnw spring-boot:run`
- Run all tests: `./mvnw test`
- Run single test: `./mvnw test -Dtest=EvergenApplicationTests`

## Code Style
- Java version: 17
- Use Spring Boot 3.x conventions
- Follow standard Java naming conventions (CamelCase for classes, methods)
- Use Lombok annotations to reduce boilerplate (@Data, @Builder, etc.)
- Organize imports: java.* first, then third-party, then project imports
- Constructor injection preferred for Spring components
- Exception handling: use Spring's exception handlers where appropriate
- Prefer immutability (final fields) when possible
- Include JavaDoc comments for public methods
- Each class should have a clear single responsibility

## Project Structure
- Follow standard Spring Boot package organization (config, model, service)
- Use DTOs for API/messaging layer
- CloudEvent standard for message format