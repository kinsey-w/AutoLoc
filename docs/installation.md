# Installation and setup

Run all commands from the project root.

## Requirements

- Git and JDK 17+ (`JAVA_HOME` set).
- MySQL 8+ running on `localhost:3306`.
- Docker Desktop running Linux containers.
- Internet access for Maven dependencies and Docker images.

## Run the project (Windows / PowerShell)

```powershell
git clone https://github.com/kinsey-w/AutoLoc.git
cd AutoLoc
$env:DB_PASSWORD = Read-Host 'Your local MySQL root password'
.\mvnw.cmd spring-boot:run
```

The password prompt uses visible input. MySQL must allow `root` to create databases; `autoloc_db` and its tables are created automatically. Maven is downloaded by the wrapper.

Alternatively, open `pom.xml` in IntelliJ, reload Maven, set `DB_PASSWORD` in the run configuration, and run `AutolocApiApplication`.

If using a different MySQL account, host or port, set `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_URL` in the same environment.

## Start monitoring

Keep the application running. In a second terminal in the project folder:

```powershell
docker compose up -d
.\scripts\verify-monitoring.ps1
```

| Service | Address |
| --- | --- |
| Public API | http://localhost:8080/api/public/ping |
| Health | http://localhost:8080/actuator/health |
| Prometheus targets | http://localhost:9090/targets |
| Grafana dashboard | http://localhost:3000/d/autoloc-monitoring |


## API accounts and tests

Classroom Basic Auth accounts: `student` / `student123` (USER), `admin` / `admin123` (ADMIN).

```powershell
$env:DB_PASSWORD = Read-Host 'Your local MySQL root password'
.\mvnw.cmd test
.\scripts\verify-security.ps1
```

MySQL must be running for JUnit tests. The security script also requires the running API.

Stop monitoring with `docker compose down`; stop the API with Ctrl+C. MySQL data, passwords and Docker volumes stay local and are not included in Git. Source, schema mappings and dashboard configuration are included.

More details: [monitoring](monitoring-notes.md), [security](security-notes.md), [Postman collection](postman/AutoLoc-Security.postman_collection.json).

On macOS/Linux use `./mvnw` (run `chmod +x mvnw` if needed) and set `DB_PASSWORD` in your shell or IDE. The setup assumes Docker Desktop; Linux Docker Engine also needs `host.docker.internal:host-gateway` under Prometheus `extra_hosts`.
