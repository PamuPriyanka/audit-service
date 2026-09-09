# Technology Guidelines

* Use **Java 17**.
* Use **Spring Boot 3.x**.
* Use **Maven**.
* Build REST APIs using **Spring Web**.
* Follow a layered architecture:
    * Controller
    * Service
    * Repository
* Persist audit events using **file-based storage**.
* Store audit files under the project's **`logs/`** directory.
* The primary audit file must be **`logs/audit.log`**.
* Support rolling audit files such as **`audit-1.log`**, **`audit-2.log`**, etc.
* Audit APIs must read from and write to these audit log files as required.
* Do not use a database for audit-event persistence.
* Use **SLF4J / Logback** where appropriate for application logging, but keep audit-event persistence separate and explicitly directed to the audit log files.
* Use **Bean Validation** for request validation.
* Use `@RestControllerAdvice` for global exception handling.
* Use constructor-based dependency injection.
* Use **JUnit 5** and **Mockito** for testing.
* Follow SOLID principles and clean code practices.
* Prefer standard Java 17 APIs and avoid unnecessary dependencies.
* Keep the implementation simple, maintainable, and production-oriented.
