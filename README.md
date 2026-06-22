# EFMS Backend

This repository contains the server side of the Experimental Farming Management System (EFMS). The application is built with Spring Boot. It stores the farm data, performs the spatial logic on the parcels and exposes everything through a REST API used by the frontend.

## Just want to run EFMS?

If you only want to deploy EFMS (the database, the backend and the website running together), you do not need this repository. The deployment repository provides a straightforward installation:

**[EFMS-deploy repository](https://github.com/W-EFMS/EFMS-deploy)**

## Technology stack

- **Java 17 and Spring Boot 3.5.7:** built and managed with Maven.
- **PostgreSQL and PostGIS:** used in production for spatial data. Parcel boundaries are stored as geometric columns and queried with PostGIS functions through Hibernate Spatial and JTS.
- **Security:** stateless JWT authentication (HS256) on top of Spring Security.
- **GeoTools:** reads shapefiles so that farmers can import their official PAC declarations.
- **JavaMail:** handles the transactional emails, such as account verification and farm invitations.
- **SFTP sync (optional):** retrieves the latest Belgian phytosanitary product catalogue.

### Project structure

The application code lives under `src/main/java/yt/wer/efms/` and follows standard Spring Boot conventions:

- `controller/`: the REST API endpoints.
- `service/`: the business logic.
- `repository/`: the database access layer.
- `model/`: the JPA entities.
- `security/`: the JWT and Spring Security configuration.
- `dto/`: the data transfer objects for the API payloads.

## Running locally

You need JDK 17 installed. The Maven wrapper (`./mvnw`) is included, so a separate Maven installation is not required.

### PostGIS database and backend setup

To run against the PostGIS database, mirroring the production environment, start the database with Docker and point the application at it:

```sh
# 1. Start only the postgres/postgis service from the compose file
docker compose up -d postgres

# 2. Edit the environment variables as needed
nano .env

# 3. Run the backend with the prod profile
./launch.sh
```

`launch.sh` loads the variables from `.env` and runs `mvn spring-boot:run`. The database schema is created automatically on the first startup through Hibernate, so there are no manual migration scripts to run.

## Configuration

EFMS is configured entirely through environment variables. They are read from `.env` in development and injected by Docker in production. The essential ones are listed below.

| Variable | Purpose | Default |
| --- | --- | --- |
| `DB_URL` | JDBC URL of the PostGIS database | `jdbc:postgresql://localhost:5432/efms_db` |
| `DB_USER` and `DB_PASS` | database credentials | `efms` / *(set your own)* |
| `DB_NAME` | database name | `efms_db` |
| `JWT_SECRET` | secret key for signing authentication tokens (at least 32 characters) | *(none, required)* |
| `SPRING_PROFILES_ACTIVE` | active profile (`dev` or `prod`) | `prod` |

The email and phytosanitary-sync features have sensible defaults in `application.properties` but require additional variables, such as `MAIL_HOST` and `PHYTO_SFTP_*`, to function. These are optional for local development.

## Building a container image

A `Dockerfile` compiles the `.jar` and packages it into a slim JRE base image:

```sh
docker build -t efms-backend:latest .
```

The included `docker-compose.yml` can start the backend and a PostGIS database together for testing. For production deployments, use the [deploy repository](https://github.com/W-EFMS/EFMS-deploy).

## API overview

All endpoints are served from the root and grouped by area:

| Prefix | Responsibility |
| --- | --- |
| `/auth` | login, registration and email verification |
| `/users` | user profiles and preferences |
| `/farm` | farms, members, roles and farm-scoped assets |
| `/parcels` | parcels, sub-parcels, operations, periods and sharing |
| `/imports` and `/imported-parcels` | the PAC shapefile import workflow |
| `/units` | reference units |
| `/admin` | system-wide user and farm administration |

The backend health is exposed through Spring Actuator at `/actuator/health`.

## License

See the [LICENSE](LICENSE) file for details.
