# Spring Security

`SecurityConfig` enables HTTP Basic, in-memory users and BCrypt password encoding.

## Classroom accounts

| Username | Password | Role |
| --- | --- | --- |
| student | student123 | USER |
| admin | admin123 | ADMIN |

These are public exercise credentials. BCrypt stores salted, slow, one-way hashes in memory; it does not hide the published sample passwords.

## Access rules

Rules use the first match. Declare `/api/public/**` and `/api/admin/**` before `/api/**` so the broader rule cannot override them. Other routes require authentication.

| GET route | Anonymous | USER | ADMIN |
| --- | --- | --- | --- |
| `/api/public/ping` | 200 | 200 | 200 |
| `/api/hello` | 401 | 200 | 200 |
| `/api/admin/report` | 401 | 403 | 200 |

- Authentication checks identity; authorization checks permissions.
- **401:** missing or invalid credentials.
- **403:** authenticated but missing the required role in this exercise. CSRF can also cause 403.
- HTTP Basic uses Base64, not encryption. Use HTTPS outside local testing.
- CSRF stays enabled. These routes use GET; review token handling before adding write operations.

## Persistent users

In-memory accounts reset on restart and lack account-management workflows. To use MySQL, add an account/role model, repository and `UserDetailsService`; store BCrypt hashes and account status. `Client` and `Employe` are not automatically login accounts.
