# Parts 13 and 14: Spring Security classroom exercise

## Configuration and demo accounts

`spring-boot-starter-security` supplies the security infrastructure.
`SecurityConfig` declares a `SecurityFilterChain`, an in-memory
`UserDetailsService`, and a BCrypt `PasswordEncoder`. Its package is
`fr.ece.autolocapi.config` so the application discovers it automatically.

The assignment supplies these public classroom credentials:

| Username | Exercise password | Role |
| --- | --- | --- |
| student | student123 | USER |
| admin | admin123 | ADMIN |

These are demonstration values, not real credentials. At startup the configuration
BCrypt-encodes each password before passing it to the in-memory user store. Each
hash uses a salt; password verification uses the encoder's `matches` operation.
BCrypt is a deliberately costly, one-way password hash, not reversible encryption.
Its purpose is to make recovering passwords from a stolen user store more expensive.
The example password literals remain visible in this classroom configuration;
encoding does not make published demo passwords secret.

## Route rules and HTTP Basic

Rules are evaluated in order, with the first matching rule applying:

| Pattern | Access |
| --- | --- |
| `/api/public/**` | Public |
| `/api/admin/**` | ADMIN only |
| `/api/**` | USER or ADMIN |
| Any other request | Authenticated user |

Both the public and admin rules precede `/api/**`. Otherwise the broader rule
would match first: public requests would require login, and a USER could reach
the admin route. `roles("ADMIN")` creates the authority `ROLE_ADMIN`; `hasRole`
adds that prefix automatically. The admin is explicitly allowed by the general
API rule; no role hierarchy is assumed.

Authentication verifies who a caller is. Authorization checks whether that
authenticated caller has permission to access a route. HTTP Basic passes the
username and password in a Base64-encoded Authorization header on requests.
Base64 is not encryption; use HTTPS outside local classroom testing.

- **401 Unauthorized:** credentials are missing or invalid; authentication is required.
- **403 Forbidden:** the caller is authenticated but lacks the required role in this
  GET exercise. Other security checks, including CSRF, can also produce a 403.

CSRF protection remains enabled. These demo routes use GET. Adding POST, PUT or
DELETE routes requires reviewing the authentication model and CSRF-token handling;
do not disable CSRF merely to make a failing request pass.

## Run and verify using real HTTP requests

Reload Maven in IntelliJ after adding the dependency, then restart
`AutolocApiApplication` with its existing `DB_PASSWORD` environment variable.
The three routes are in `SecurityDemoController`.

| GET request | Anonymous | student | admin |
| --- | --- | --- | --- |
| `/api/public/ping` | 200 | 200 | 200 |
| `/api/hello` | 401 | 200 | 200 |
| `/api/admin/report` | 401 | 403 | 200 |

In Postman choose **No Auth** for anonymous requests. For the other columns choose
**Authorization > Basic Auth** and enter the corresponding classroom credentials.
Capture the actual 200, 401 and 403 responses for the screenshots requested in
part 14. This document does not substitute for those screenshots.

PowerShell examples use `curl.exe` explicitly:

```powershell
curl.exe -i -H "Accept: text/plain" http://localhost:8080/api/public/ping
curl.exe -i -H "Accept: text/plain" http://localhost:8080/api/hello
curl.exe -i -H "Accept: text/plain" -u student:student123 http://localhost:8080/api/hello
curl.exe -i -H "Accept: text/plain" -u student:student123 http://localhost:8080/api/admin/report
curl.exe -i -H "Accept: text/plain" -u admin:admin123 http://localhost:8080/api/admin/report
```

These examples intentionally use only the published exercise passwords.
No mock security tests are needed for this exercise: check the running server.

For a repeatable check of all nine combinations, start the application and run
this from the project directory in PowerShell:

```powershell
.\scripts\verify-security.ps1
```

The script sends real HTTP requests, isolates callers without cookies, checks
each expected status and the successful response bodies, and fails if any result
differs. Use `-BaseUrl http://localhost:8081` if running on another port. This
script is separate from the JUnit persistence tests and does not use mocks.

### Validation results

All production and test sources compiled with the installed JDK, Lombok and a
Java 17 target. A separate Spring Boot server was started on an automatically
allocated local port, using the existing MySQL configuration. The verification
script passed all nine real HTTP request combinations, including response-body
checks for successful requests. All nine existing JUnit tests also passed with
Spring Security enabled; their sample database rows were rolled back.

The checks used the installed libraries directly through the JDK and JUnit
Platform launcher. Postman screenshots remain a manual part 14 deliverable.

## Why in-memory users are limited

The user records and their encoded passwords exist only in application memory.
They are reconstructed on restart and provide no persistent registration,
password management or administrative account workflow. The JPA `Client` and
`Employe` entities are not automatically login accounts.

To authenticate from MySQL, add a persistent account model with unique usernames,
encoded password hashes, enabled/disabled state, and role authorities. Implement
a `UserDetailsService` that loads accounts and roles through repositories, and
use `PasswordEncoder` when setting or changing passwords. Production deployment
also needs secure account-management flows, HTTPS and appropriate secret handling.
