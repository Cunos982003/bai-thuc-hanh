# Job Board API — Design

**Date:** 2026-11-04
**Source of truth:** `DE_BAI.md`. If this document and that file disagree, the file wins.
**Status:** draft, pending review

## Intent

Implement `DE_BAI.md` as a submittable take-home. It is graded on four criteria —
requirements met, code structure & OOP, table design & queries, Git & README — so
"the endpoints work" is only one quarter of the goal. Structure, schema, and commit
history are first-class deliverables here, not by-products.

## Goals

1. `GET /jobs` — keyword search, location filter, status filter, pagination, sorting
2. Full CRUD for Company and Job
3. `POST /jobs/{id}/applications` — rejects a duplicate email within the same job;
   rejects an application to a CLOSED job
4. Input validation, with one consistent JSON error shape for 400, 404, and 409
5. At least 5 unit tests on the service layer
6. Public GitHub repo, incremental commits with clear messages
7. README — how to run, migration/schema script, example requests

## Non-goals

Not in `DE_BAI.md`, so not being built.

- Authentication, authorization, user accounts
- OpenAPI / Swagger UI
- Docker or docker-compose
- Caching
- File upload (the CV is a URL string, not an upload)
- Soft delete
- Any frontend or UI
- CI pipeline

## Anti-goals

Technically working, but a failure against the grading criteria.

- JPA entities returned directly from controllers
- Pagination or filtering performed in application memory
- A schema with no FK constraints, no unique constraint on `(job_id, email)`, no indexes
- Enum persisted as ordinal
- One "initial commit" containing the whole project
- Unhandled exceptions surfacing as 500 where 400 / 404 / 409 is correct
- Business logic living in controllers

## Constraints

| Constraint | Value | Source |
|---|---|---|
| Language | Java 17 | `DE_BAI.md:1`, `build.gradle:13` |
| Framework | Spring Boot 3.x | `DE_BAI.md:1` |
| Database | PostgreSQL | `DE_BAI.md:1` |
| Build tool | Gradle | existing `build.gradle` |
| Base package | `re.edu.thuchanh` | existing project |

## Decisions you made

- **Spring Boot 3, not 4** (2026-11-04). `build.gradle:3` currently pins 4.1.2 and uses
  Boot 4 starter names (`spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa-test`);
  it gets migrated to 3.x.
- **Follow `DE_BAI.md` literally.** No features beyond what the file asks for.

## Decisions I am making

My calls, not yours. Reject any of them.

**Build**

1. Add `spring-boot-starter-validation`. Requirement 4 is unimplementable without it and
   it is not pulled in transitively by the JPA or web starter.
2. Add Flyway (`flyway-core` + `flyway-database-postgresql`). The README must ship a
   table-creation script or a migration, and Flyway gives criterion 3 something concrete
   to grade.
3. `spring.jpa.hibernate.ddl-auto: validate` — the migration owns the schema; Hibernate
   only checks that entities match it.

**Structure**

4. Package by feature (`company/`, `job/`, `application/`), each holding its entity,
   repository, service, controller, and its own `dto/` package.
5. DTOs at every boundary. Entities are never serialized.
6. The application entity is named `JobApplication`, not `Application` — avoids constant
   confusion with `@SpringBootApplication` and `SpringApplication`.
7. Exception types map to status codes in one `@RestControllerAdvice`:
   `NotFoundException` → 404, `ConflictException` → 409, validation → 400.

**Schema**

8. `status` stored as `VARCHAR` via `@Enumerated(EnumType.STRING)`.
9. `UNIQUE (job_id, email)` on applications, plus a handler catching
   `DataIntegrityViolationException` → 409. The constraint is the real guarantee; the
   service-level pre-check only exists for a clean message, and on its own has a race.
10. `CHECK (salary_min <= salary_max)`; both columns nullable.
11. Indexes on `jobs(location)`, `jobs(status)`, `jobs(company_id)`, `applications(job_id)`.

**Behaviour where the file is silent**

12. The `GET /jobs` keyword matches `title` OR `description`, case-insensitive.
13. `sort` is whitelisted (`title`, `salaryMin`, `salaryMax`, `location`, `createdAt`);
    anything else is a 400.
14. `GET /jobs` returns an envelope (`content`, `page`, `size`, `totalElements`,
    `totalPages`) rather than Spring's raw `Page` serialization.
15. Applying to a CLOSED job → 409. The request is well-formed; the resource state conflicts.
16. Deleting a Company that still has Jobs → 409. Deleting a Job that still has
    Applications → 409. No cascading deletes.
17. `appliedAt` is set server-side; any client-supplied value is ignored.

## Open item for you

- **README language** — Vietnamese or English? The assignment is Vietnamese; I default to
  English unless you say otherwise.

## What is left to the builder

Everything not fixed above: the exact Spring Boot 3.x patch version, the migration file
contents, class and method bodies, the specific test cases, and commit granularity. The
implementation plan covers these.
