# ReliaQuest's Entry-Level Java Challenge

Please keep the following in mind while working on this challenge:
* Code implementations will not be graded for **correctness** but rather on practicality
* Articulate clear and concise design methodologies, if necessary
* Use clean coding etiquette
  * E.g. avoid liberal use of new-lines, odd variable and method names, random indentation, etc...
* Test cases are not required

## Problem Statement

Your employer has recently purchased a license to top-tier SaaS platform, Employees-R-US, to off-load all employee management responsibilities.
Unfortunately, your company's product has an existing employee management solution that is tightly coupled to other services and therefore 
cannot be replaced whole-cloth. Product and Development leads in your department have decided it would be best to interface
the existing employee management solution with the commercial offering from Employees-R-US for the time being until all employees can be
migrated to the new SaaS platform.

Your ask is to expose employee information as a protected, secure REST API for consumption by Employees-R-US web hooks.
The initial REST API will consist of 3 endpoints, listed in the following section. If for any reason the implementation 
of an endpoint is problematic, the team lead will accept **pseudo-code** and a pertinent description (e.g. java-doc) of intent.

Good luck!

## Endpoints to implement (API module)

_See `com.challenge.api.controller.EmployeeController` for details._

getAllEmployees()

    output - list of employees
    description - this should return all employees, unfiltered

getEmployeeByUuid(...)

    path variable - employee UUID
    output - employee
    description - this should return a single employee based on the provided employee UUID

createEmployee(...)

    request body - attributes necessary to create an employee
    output - employee
    description - this should return a single employee, if created, otherwise error

## Code Formatting

This project utilizes Gradle plugin [Diffplug Spotless](https://github.com/diffplug/spotless/tree/main/plugin-gradle) to enforce format
and style guidelines with every build.

To format code according to style guidelines, you can run **spotlessApply** task.
`./gradlew spotlessApply`

The spotless plugin will also execute check-and-validation tasks as part of the gradle **build** task.
`./gradlew build`

---

## Running & Testing

### Build and run tests

```bash
./gradlew :api:build
```

Windows:

```powershell
.\gradlew.bat :api:build
```

### Start the server

```bash
./gradlew :api:bootRun
```

Windows:

```powershell
.\gradlew.bat :api:bootRun
```

The server starts on `http://localhost:8080`.

### Credentials

Default dev credentials:

| Variable | Default |
|---|---|
| `API_USERNAME` | `erus` |
| `API_PASSWORD` | `changeme` |

Override at runtime:

```bash
API_USERNAME=myuser API_PASSWORD=mypassword ./gradlew :api:bootRun
```

Windows PowerShell:

```powershell
$env:API_USERNAME="myuser"
$env:API_PASSWORD="mypassword"
.\gradlew.bat :api:bootRun
```

### Smoke-test curl examples

**Unauthenticated request - expect 401 with `WWW-Authenticate` header:**

```bash
curl -i http://localhost:8080/api/v1/employee
```

**List all employees - expect 200 with JSON array:**

```bash
curl -i -u erus:changeme http://localhost:8080/api/v1/employee
```

**Create an employee - expect 201 with `Location: /api/v1/employee/{uuid}`:**

```bash
curl -i -u erus:changeme -H "Content-Type: application/json" \
  -d '{"firstName":"Ada","lastName":"Lovelace","salary":150000,"age":36,"jobTitle":"Engineer","email":"ada@example.com","contractHireDate":"2025-01-01T00:00:00Z"}' \
  http://localhost:8080/api/v1/employee
```

**Get employee by UUID - expect 200:**

```bash
curl -i -u erus:changeme http://localhost:8080/api/v1/employee/{uuid}
```

**Invalid UUID - expect 400 with `ErrorResponse` body:**

```bash
curl -i -u erus:changeme http://localhost:8080/api/v1/employee/not-a-uuid
```

**Unknown UUID - expect 404 with `ErrorResponse` body:**

```bash
curl -i -u erus:changeme http://localhost:8080/api/v1/employee/00000000-0000-0000-0000-000000000000
```

**Validation failure (empty body) - expect 400 with `fieldErrors`:**

```bash
curl -i -u erus:changeme -H "Content-Type: application/json" -d '{}' http://localhost:8080/api/v1/employee
```

---

## Design Notes

- **Controller/service split** - `EmployeeController` handles HTTP concerns such as request mapping, response status, and the `Location` header; `EmployeeService` owns employee creation and lookup logic.
- **API dependencies** - the api module declares the Spring Boot starters it directly relies on: web, validation, and security. This makes the module runtime requirements explicit instead of relying only on shared convention-plugin dependencies.
- **Records introduced alongside a setter-driven interface** - request and response contracts use immutable DTOs at the API boundary, while `EmployeeImpl` defines the concrete behavior behind the provided interface.
- **Derived `fullName`** - the interface exposes `fullName` as a JavaBean-style property; this implementation computes it from `firstName + " " + lastName` instead of storing a separate value.
- **In-memory store** - employee data lives in a `ConcurrentHashMap` for the scope of this challenge; no database is involved.
- **Defensive copies** - the service returns copies of stored `Employee` objects so callers cannot mutate internal state.
- **`ErrorResponse` envelope** - typed application exceptions thrown by the controller, validation failures, and malformed UUIDs return a consistent JSON envelope with `status`, `message`, and optional `fieldErrors`.
- **Stateless HTTP Basic auth** - all `/api/**` routes require a valid `Authorization: Basic ...` header on every request; no session is created.
- **401 handling** - unauthenticated requests receive Spring Security's standard `WWW-Authenticate` challenge; the body is not an `ErrorResponse`.
- **Dev credentials** - `{noop}` plaintext passwords are intentional for this mock setup. Production deployments should source credentials from a secrets manager and use a proper password encoder.
