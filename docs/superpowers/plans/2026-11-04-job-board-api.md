# Job Board API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the `DE_BAI.md` job-board API as a submittable take-home: Company/Job CRUD, filtered and paged `GET /jobs`, and job applications with duplicate and closed-job rejection.

**Architecture:** Spring Boot 3.5.16, package-by-feature (`company/`, `job/`, `application/`, plus a flat `common/` for cross-cutting error handling). JPA entities are never serialized — every boundary crossing uses a DTO, and DTOs are Java records. Flyway owns the schema; Hibernate only validates against it. All exceptions map to status codes in a single `@RestControllerAdvice`.

**Tech Stack:** Java 17 (toolchain), Spring Boot 3.5.16, Spring Data JPA, Jakarta Bean Validation, Flyway 11, PostgreSQL, Gradle 9.8.2 (wrapper unchanged), JUnit 5 + Mockito + MockMvc.

**Spec:** `docs/superpowers/specs/2026-11-04-job-board-api-design.md`

## Global Constraints

Copied verbatim from the spec; every task's requirements implicitly include this section.

- Language **Java 17**; framework **Spring Boot 3.x**; database **PostgreSQL**; build tool **Gradle**; base package `re.edu.thuchanh`.
- **Follow `DE_BAI.md` literally.** No features beyond what that file asks for.
- Non-goals (do not build): authentication/authorization, OpenAPI/Swagger, Docker/docker-compose, caching, file upload, soft delete, any frontend, CI pipeline.
- Anti-goals (a change that introduces one of these is wrong even if it works): JPA entities returned from controllers; pagination or filtering in application memory; a schema with no FK constraints, no unique constraint on `(job_id, email)`, or no indexes; enum persisted as ordinal; one "initial commit" holding the whole project; unhandled exceptions surfacing as 500 where 400/404/409 is correct; business logic in controllers.
- Entities are never serialized. Business logic lives in services, not controllers.
- `status` is `VARCHAR` via `@Enumerated(EnumType.STRING)`.
- `UNIQUE (job_id, email)` on applications; `CHECK (salary_min <= salary_max)`; both salary columns nullable.
- Indexes on `jobs(location)`, `jobs(status)`, `jobs(company_id)`, `applications(job_id)`.
- README is written in **Vietnamese** (user decision, 2026-11-04).
- **`./gradlew test` must pass with no database, no Docker, and no network.** Every test is a Mockito unit test or a MockMvc slice test.

### Verified environment facts (established 2026-11-04, do not re-litigate)

- Spring Boot **3.5.16** applies cleanly under Gradle **9.8.2** — verified by a real build. The Gradle wrapper stays at 9.8.2; the docs page lists only Gradle 7.6.4+/8.4+ but the combination works.
- Only JDK **21** (Temurin 21.0.11) is installed; `java` is not on PATH but `JAVA_HOME` is set. `toolchain { languageVersion = 17 }` still resolves and emits **Java 17 bytecode (major version 61)** — verified.
- **No PostgreSQL and no running Docker daemon.** The Flyway migration therefore cannot be executed locally. See "Known gaps".
- Git identity is configured (`Cunos982003 <cuongjap208@gmail.com>`).

### Known gaps

- **The Flyway migration is the highest-risk artifact in this plan.** It cannot be executed here, so a syntax or type error in it will not be caught by any test. Write `V1__init.sql` with unusual care, and keep it consistent with the entities so `ddl-auto: validate` passes for the grader.
- No test exercises the real database. Behaviour that lives only in SQL (the `CHECK` constraint, the unique constraint's exact semantics, index usage) is reasoned about, not proven.

## Review Focus

Input classes and failure modes the spec implies but whose tests no task would otherwise write. Each is pinned by a test in the task named beside it.

1. **Malformed or type-mismatched JSON body** (e.g. `{"name":` or `"salaryMin": "abc"`) — a reasonable client expects `400` in the standard error shape; Spring's default is an unhandled `500`. Test in Task 2.
2. **Negative or zero `page` / `size` on `GET /jobs`** — a reasonable client expects `400`; Spring Data's default is an unhandled `500`. Test in Task 5.
3. **`keyword` containing `%` or `_`** — a reasonable person searching for `100%` expects a literal substring match, not wildcard expansion. Test in Task 5.
4. **A string longer than its column** (e.g. a 300-character company name) — a reasonable client expects `400`, not a `500` from the database. Test in Task 3.
5. **A `DataIntegrityViolationException` from the `(job_id, email)` unique constraint** — the race spec decision 9 explicitly names. A reasonable client expects `409`, not `500`. Test in Task 2.

---

## Task 1: Build, configuration, and schema foundation

**Files:**
- Modify: `build.gradle` (whole file)
- Create: `src/main/resources/application.yaml` (replace existing)
- Create: `src/main/resources/db/migration/V1__init.sql`
- Delete: `src/test/java/re/edu/thuchanh/ThuchanhApplicationTests.java`
- Modify: `.gitignore` (append `.superpowers/`)

**Interfaces:**
- Consumes: nothing.
- Produces: a compiling Spring Boot 3.5.16 project; the `companies`, `jobs`, `applications` tables that every later task's entities must match exactly.

- [ ] **Step 1: Initialise the repository**

The project is not under version control yet. The assignment grades commit history, so this happens first.

```bash
cd "C:/Users/admin/IdeaProjects/thuchanh"
git init
git add .gitignore settings.gradle build.gradle gradlew gradlew.bat gradle/ src/ docs/
git status
```

Expected: `git status` lists the scaffold as staged, and does **not** list `build/`, `.gradle/`, or `.idea/`.

- [ ] **Step 2: Add the execution ledger to `.gitignore`**

Append `.superpowers/` to `.gitignore` so the skill's scratch workspace is never committed.

- [ ] **Step 3: Replace `build.gradle`**

Boot 4 starter names (`spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa-test`, `spring-boot-starter-webmvc-test`) do not exist in 3.x. Keep Lombok — it is already configured and working.

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.5.16'
    id 'io.spring.dependency-management' version '1.1.7'
}

group = 're.edu'
version = '0.0.1-SNAPSHOT'
description = 'thuchanh'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.flywaydb:flyway-core'
    implementation 'org.flywaydb:flyway-database-postgresql'
    compileOnly 'org.projectlombok:lombok'
    runtimeOnly 'org.postgresql:postgresql'
    annotationProcessor 'org.projectlombok:lombok'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testCompileOnly 'org.projectlombok:lombok'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
    testAnnotationProcessor 'org.projectlombok:lombok'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

- [ ] **Step 4: Write `application.yaml`**

`ddl-auto: validate` means the migration owns the schema and Hibernate only checks the entities against it. `open-in-view: false` avoids the lazy-loading-in-view antipattern.

```yaml
spring:
  application:
    name: thuchanh
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/jobboard}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true
```

- [ ] **Step 5: Write `V1__init.sql`**

Column names and types here must match the entities in Tasks 3, 4 and 6 exactly, or `ddl-auto: validate` will fail at startup for the grader.

```sql
CREATE TABLE companies (
    id      BIGSERIAL PRIMARY KEY,
    name    VARCHAR(255) NOT NULL,
    address VARCHAR(255),
    website VARCHAR(255)
);

CREATE TABLE jobs (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    salary_min  BIGINT,
    salary_max  BIGINT,
    location    VARCHAR(255),
    status      VARCHAR(16) NOT NULL,
    company_id  BIGINT NOT NULL REFERENCES companies (id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_jobs_status CHECK (status IN ('OPEN', 'CLOSED')),
    CONSTRAINT chk_jobs_salary CHECK (
        salary_min IS NULL OR salary_max IS NULL OR salary_min <= salary_max
    )
);

CREATE TABLE applications (
    id         BIGSERIAL PRIMARY KEY,
    job_id     BIGINT NOT NULL REFERENCES jobs (id),
    full_name  VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    cv_url     VARCHAR(1000) NOT NULL,
    applied_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_applications_job_email UNIQUE (job_id, email)
);

CREATE INDEX idx_jobs_location ON jobs (location);
CREATE INDEX idx_jobs_status ON jobs (status);
CREATE INDEX idx_jobs_company_id ON jobs (company_id);
CREATE INDEX idx_applications_job_id ON applications (job_id);
```

- [ ] **Step 6: Delete the generated context test**

`ThuchanhApplicationTests` is `@SpringBootTest` and needs a live PostgreSQL, which is not available here and must not be a precondition for `./gradlew test`. Delete the file.

```bash
rm src/test/java/re/edu/thuchanh/ThuchanhApplicationTests.java
```

- [ ] **Step 7: Verify the build**

Run: `./gradlew build`
Expected: `BUILD SUCCESSFUL`. Warnings about deprecated Gradle features are expected and harmless — they concern Gradle 10, not 9.8.2.

- [ ] **Step 8: Commit**

```bash
git add build.gradle .gitignore src/main/resources/application.yaml src/main/resources/db/migration/V1__init.sql
git rm --cached src/test/java/re/edu/thuchanh/ThuchanhApplicationTests.java 2>/dev/null || true
git add -A src/
git commit -m "chore: migrate to Spring Boot 3.5.16 and add Flyway schema"
```

---

## Task 2: Error handling core

**Files:**
- Create: `src/main/java/re/edu/thuchanh/common/NotFoundException.java`
- Create: `src/main/java/re/edu/thuchanh/common/ConflictException.java`
- Create: `src/main/java/re/edu/thuchanh/common/BadRequestException.java`
- Create: `src/main/java/re/edu/thuchanh/common/ErrorResponse.java`
- Create: `src/main/java/re/edu/thuchanh/common/GlobalExceptionHandler.java`
- Test: `src/test/java/re/edu/thuchanh/common/GlobalExceptionHandlerTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces: `NotFoundException(String)`, `ConflictException(String)`, `BadRequestException(String)` — every later service throws these. `ErrorResponse(Instant timestamp, int status, String error, String message, String path, List<FieldError> fieldErrors)` with nested `ErrorResponse.FieldError(String field, String message)`. `GlobalExceptionHandler` is a `@RestControllerAdvice` bean that later `@WebMvcTest` slices pick up automatically.

- [ ] **Step 1: Write the failing test**

The test uses a standalone MockMvc with a probe controller, so it needs no Spring context and no database. It pins the whole error contract, including Review Focus items 1 and 5.

```java
package re.edu.thuchanh.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    @RestController
    static class ProbeController {
        @GetMapping("/probe/not-found")
        void notFound() { throw new NotFoundException("Company 7 not found"); }

        @GetMapping("/probe/conflict")
        void conflict() { throw new ConflictException("Job is closed"); }

        @GetMapping("/probe/bad-request")
        void badRequest() { throw new BadRequestException("Unknown sort field"); }

        @GetMapping("/probe/db")
        void db() { throw new DataIntegrityViolationException("uk_applications_job_email"); }

        @PostMapping("/probe/validated")
        void validated(@Valid @RequestBody ProbeBody body) { }

        record ProbeBody(@NotBlank String name) { }
    }

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void notFoundMapsTo404() throws Exception {
        mockMvc.perform(get("/probe/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Company 7 not found"))
                .andExpect(jsonPath("$.path").value("/probe/not-found"));
    }

    @Test
    void conflictMapsTo409() throws Exception {
        mockMvc.perform(get("/probe/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void badRequestMapsTo400() throws Exception {
        mockMvc.perform(get("/probe/bad-request"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void beanValidationMapsTo400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/probe/validated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    void malformedJsonMapsTo400() throws Exception {
        mockMvc.perform(post("/probe/validated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void wrongJsonTypeMapsTo400() throws Exception {
        mockMvc.perform(post("/probe/validated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":123}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void dataIntegrityViolationMapsTo409() throws Exception {
        mockMvc.perform(get("/probe/db"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew test --tests 're.edu.thuchanh.common.GlobalExceptionHandlerTest'`
Expected: FAIL — compilation error, the exception types do not exist yet.

- [ ] **Step 3: Write the three exception types**

Each is a `RuntimeException` with a single `String message` constructor.

```java
package re.edu.thuchanh.common;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
```

`ConflictException` and `BadRequestException` are identical in shape with their own names.

- [ ] **Step 4: Write `ErrorResponse`**

```java
package re.edu.thuchanh.common;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> fieldErrors) {

    public record FieldError(String field, String message) { }
}
```

- [ ] **Step 5: Write `GlobalExceptionHandler`**

One `@RestControllerAdvice`; each method builds an `ErrorResponse`. Statuses: `NotFoundException` → 404, `ConflictException` → 409, `BadRequestException` → 400, `MethodArgumentNotValidException` → 400 (collecting one `FieldError` per violated field from `getBindingResult().getFieldErrors()`), `ConstraintViolationException` → 400, `HttpMessageNotReadableException` → 400, `MethodArgumentTypeMismatchException` → 400, `DataIntegrityViolationException` → 409, and a final `Exception` → 500 fallback. Take `HttpServletRequest` as a parameter to fill `path`; leave `fieldErrors` empty (`List.of()`) except for validation failures.

- [ ] **Step 6: Run the test to verify it passes**

Run: `./gradlew test --tests 're.edu.thuchanh.common.GlobalExceptionHandlerTest'`
Expected: PASS — 7 tests, 0 failures.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/re/edu/thuchanh/common src/test/java/re/edu/thuchanh/common
git commit -m "feat: add consistent JSON error handling for 400, 404, 409"
```

---

## Task 3: Company feature

**Files:**
- Create: `src/main/java/re/edu/thuchanh/company/Company.java`
- Create: `src/main/java/re/edu/thuchanh/company/CompanyRepository.java`
- Create: `src/main/java/re/edu/thuchanh/company/dto/CompanyRequest.java`
- Create: `src/main/java/re/edu/thuchanh/company/dto/CompanyResponse.java`
- Create: `src/main/java/re/edu/thuchanh/company/CompanyService.java`
- Create: `src/main/java/re/edu/thuchanh/company/CompanyController.java`
- Test: `src/test/java/re/edu/thuchanh/company/CompanyServiceTest.java`
- Test: `src/test/java/re/edu/thuchanh/company/CompanyControllerTest.java`

**Interfaces:**
- Consumes: `NotFoundException`, `ConflictException`, `ErrorResponse`, `GlobalExceptionHandler` from Task 2.
- Produces: entity `Company` with `Long id`, `String name`, `String address`, `String website`, and a protected no-arg constructor plus a static `create(String name, String address, String website)` factory. `CompanyRepository extends JpaRepository<Company, Long>` with `boolean existsJobByCompanyId(Long id)`. `CompanyService` with `CompanyResponse create(CompanyRequest)`, `CompanyResponse get(Long)`, `List<CompanyResponse> list()`, `CompanyResponse update(Long, CompanyRequest)`, `void delete(Long)`. `CompanyRequest(String name, String address, String website)`, `CompanyResponse(Long id, String name, String address, String website)`.

- [ ] **Step 1: Write the failing service test**

Mockito unit test, no Spring context. The delete guard is tested here because the spec forbids cascading deletes (spec decision 16).

```java
package re.edu.thuchanh.company;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import re.edu.thuchanh.common.ConflictException;
import re.edu.thuchanh.common.NotFoundException;
import re.edu.thuchanh.company.dto.CompanyRequest;
import re.edu.thuchanh.company.dto.CompanyResponse;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock CompanyRepository companyRepository;
    @InjectMocks CompanyService companyService;

    @Test
    void createPersistsAndReturnsResponse() {
        when(companyRepository.save(any(Company.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CompanyResponse response = companyService.create(
                new CompanyRequest("Acme", "Hanoi", "https://acme.example"));

        assertThat(response.name()).isEqualTo("Acme");
        assertThat(response.address()).isEqualTo("Hanoi");
    }

    @Test
    void getThrowsNotFoundForUnknownId() {
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.get(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void listReturnsAllCompanies() {
        when(companyRepository.findAll()).thenReturn(List.of(
                Company.create("Acme", null, null),
                Company.create("Globex", null, null)));

        assertThat(companyService.list()).hasSize(2);
    }

    @Test
    void updateReplacesFields() {
        Company existing = Company.create("Old", "Old addr", null);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(companyRepository.save(existing)).thenReturn(existing);

        CompanyResponse response = companyService.update(1L,
                new CompanyRequest("New", "New addr", "https://new.example"));

        assertThat(response.name()).isEqualTo("New");
        assertThat(response.address()).isEqualTo("New addr");
    }

    @Test
    void updateThrowsNotFoundForUnknownId() {
        when(companyRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.update(42L, new CompanyRequest("x", null, null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteRemovesCompanyWithNoJobs() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(Company.create("Acme", null, null)));
        when(companyRepository.existsJobByCompanyId(1L)).thenReturn(false);

        companyService.delete(1L);

        verify(companyRepository).delete(any(Company.class));
    }

    @Test
    void deleteThrowsConflictWhenCompanyStillHasJobs() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(Company.create("Acme", null, null)));
        when(companyRepository.existsJobByCompanyId(1L)).thenReturn(true);

        assertThatThrownBy(() -> companyService.delete(1L))
                .isInstanceOf(ConflictException.class);

        verify(companyRepository, never()).delete(any(Company.class));
    }

    @Test
    void deleteThrowsNotFoundForUnknownId() {
        when(companyRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.delete(7L))
                .isInstanceOf(NotFoundException.class);
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew test --tests 're.edu.thuchanh.company.CompanyServiceTest'`
Expected: FAIL — compilation error, `Company`, `CompanyRepository`, `CompanyService` do not exist.

- [ ] **Step 3: Write the `Company` entity**

`@Entity @Table(name = "companies")`, Lombok `@Getter @Setter` (never `@Data` — it generates a broken `equals`/`hashCode` for JPA entities), a `protected Company()` no-arg constructor, and `static Company create(String name, String address, String website)`. Fields: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id`, `@Column(nullable = false) String name`, `String address`, `String website`. These must match `V1__init.sql`.

- [ ] **Step 4: Write `CompanyRepository`**

`interface CompanyRepository extends JpaRepository<Company, Long>` plus the delete guard. The `jobs` table exists from Task 1's migration, but the `Job` entity does not exist yet — so this is a native query:

```java
@Query(value = "SELECT count(*) > 0 FROM jobs WHERE company_id = :id", nativeQuery = true)
boolean existsJobByCompanyId(@Param("id") Long id);
```

- [ ] **Step 5: Write `CompanyRequest` and `CompanyResponse`**

```java
public record CompanyRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 255) String address,
        @Size(max = 255) String website) { }
```

`CompanyResponse(Long id, String name, String address, String website)` with `static CompanyResponse from(Company company)`. The `@Size(max = 255)` on `name` is what turns an over-length name into a `400` instead of a database `500` (Review Focus item 4).

- [ ] **Step 6: Write `CompanyService`**

`@Service @RequiredArgsConstructor`, `@Transactional(readOnly = true)` at class level, `@Transactional` on `create`, `update`, `delete`. `get`, `update` and `delete` throw `NotFoundException("Company %d not found".formatted(id))` when `findById` is empty. `delete` throws `ConflictException` when `existsJobByCompanyId` is true.

- [ ] **Step 7: Run the service test to verify it passes**

Run: `./gradlew test --tests 're.edu.thuchanh.company.CompanyServiceTest'`
Expected: PASS — 8 tests, 0 failures.

- [ ] **Step 8: Write the failing controller test**

`@WebMvcTest(CompanyController.class)` with `@MockitoBean CompanyService`. Do **not** use `@MockBean` — it is deprecated in Boot 3.4+. `@WebMvcTest` already picks up `GlobalExceptionHandler`. This pins Review Focus item 4 at the boundary.

```java
@WebMvcTest(CompanyController.class)
class CompanyControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean CompanyService companyService;

    @Test
    void createReturns201WithLocation() throws Exception {
        when(companyService.create(any())).thenReturn(new CompanyResponse(1L, "Acme", null, null));

        mockMvc.perform(post("/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Acme\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/companies/1"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createRejectsBlankNameWith400() throws Exception {
        mockMvc.perform(post("/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    void createRejectsOverlongNameWith400NotServerError() throws Exception {
        String longName = "a".repeat(300);

        mockMvc.perform(post("/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + longName + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getReturns404InStandardShapeWhenServiceThrows() throws Exception {
        when(companyService.get(9L)).thenThrow(new NotFoundException("Company 9 not found"));

        mockMvc.perform(get("/companies/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/companies/9"));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/companies/3"))
                .andExpect(status().isNoContent());
    }
}
```

- [ ] **Step 9: Run the controller test to verify it fails**

Run: `./gradlew test --tests 're.edu.thuchanh.company.CompanyControllerTest'`
Expected: FAIL — compilation error, `CompanyController` does not exist.

- [ ] **Step 10: Write `CompanyController`**

`@RestController @RequestMapping("/companies") @RequiredArgsConstructor`. `POST` returns `201` with `Location: /companies/{id}`; `GET /{id}`, `GET`, `PUT /{id}` return `200`; `DELETE /{id}` returns `204`. Every method is a one-line delegation to the service — no business logic.

- [ ] **Step 11: Run the controller test to verify it passes**

Run: `./gradlew test --tests 're.edu.thuchanh.company.CompanyControllerTest'`
Expected: PASS — 5 tests, 0 failures.

- [ ] **Step 12: Run the whole suite**

Run: `./gradlew test`
Expected: PASS — all tests from Tasks 2 and 3 green.

- [ ] **Step 13: Commit**

```bash
git add src/main/java/re/edu/thuchanh/company src/test/java/re/edu/thuchanh/company
git commit -m "feat: add Company CRUD with conflict-safe delete"
```

---

## Task 4: Job feature — entity, repository, CRUD

**Files:**
- Create: `src/main/java/re/edu/thuchanh/job/JobStatus.java`
- Create: `src/main/java/re/edu/thuchanh/job/Job.java`
- Create: `src/main/java/re/edu/thuchanh/job/JobRepository.java`
- Create: `src/main/java/re/edu/thuchanh/job/dto/JobRequest.java`
- Create: `src/main/java/re/edu/thuchanh/job/dto/JobResponse.java`
- Create: `src/main/java/re/edu/thuchanh/job/JobService.java`
- Create: `src/main/java/re/edu/thuchanh/job/JobController.java`
- Test: `src/test/java/re/edu/thuchanh/job/JobServiceTest.java`

**Interfaces:**
- Consumes: `CompanyRepository` (Task 3), the common exceptions (Task 2).
- Produces: `enum JobStatus { OPEN, CLOSED }`; entity `Job` with `Long id`, `String title`, `String description`, `Long salaryMin`, `Long salaryMax`, `String location`, `JobStatus status`, `Company company`, `Instant createdAt`; `JobRepository extends JpaRepository<Job, Long>`; `JobRequest(String title, String description, Long salaryMin, Long salaryMax, String location, JobStatus status, Long companyId)`; `JobResponse(Long id, String title, String description, Long salaryMin, Long salaryMax, String location, JobStatus status, Long companyId, String companyName, Instant createdAt)`; `JobService` with `create`, `get`, `update`, `delete` (search arrives in Task 5).

- [ ] **Step 1: Write the failing service test**

```java
@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock JobRepository jobRepository;
    @Mock CompanyRepository companyRepository;
    @InjectMocks JobService jobService;

    private Company acme() { return Company.create("Acme", "Hanoi", null); }

    private JobRequest openJobRequest(Long companyId) {
        return new JobRequest("Backend Dev", "Build APIs", 1000L, 2000L,
                "Hanoi", JobStatus.OPEN, companyId);
    }

    @Test
    void createAttachesCompanyAndStampsCreatedAt() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(acme()));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));

        JobResponse response = jobService.create(openJobRequest(1L));

        assertThat(response.companyName()).isEqualTo("Acme");
        assertThat(response.createdAt()).isNotNull();
    }

    @Test
    void createThrowsNotFoundWhenCompanyMissing() {
        when(companyRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.create(openJobRequest(5L)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("5");

        verify(jobRepository, never()).save(any());
    }

    @Test
    void getThrowsNotFoundForUnknownId() {
        when(jobRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.get(99L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateReplacesFields() {
        Job existing = Job.create("Old", null, null, null, null, JobStatus.OPEN, acme());
        when(jobRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(companyRepository.findById(1L)).thenReturn(Optional.of(acme()));
        when(jobRepository.save(existing)).thenReturn(existing);

        JobResponse response = jobService.update(1L,
                new JobRequest("New", "desc", 10L, 20L, "Da Nang", JobStatus.CLOSED, 1L));

        assertThat(response.title()).isEqualTo("New");
        assertThat(response.status()).isEqualTo(JobStatus.CLOSED);
    }

    @Test
    void deleteRemovesJobWithNoApplications() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(
                Job.create("t", null, null, null, null, JobStatus.OPEN, acme())));
        when(jobRepository.existsApplicationByJobId(1L)).thenReturn(false);

        jobService.delete(1L);

        verify(jobRepository).delete(any(Job.class));
    }

    @Test
    void deleteThrowsConflictWhenJobStillHasApplications() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(
                Job.create("t", null, null, null, null, JobStatus.OPEN, acme())));
        when(jobRepository.existsApplicationByJobId(1L)).thenReturn(true);

        assertThatThrownBy(() -> jobService.delete(1L))
                .isInstanceOf(ConflictException.class);

        verify(jobRepository, never()).delete(any(Job.class));
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew test --tests 're.edu.thuchanh.job.JobServiceTest'`
Expected: FAIL — compilation error, `Job`, `JobRepository`, `JobService` do not exist.

- [ ] **Step 3: Write `JobStatus` and `Job`**

`enum JobStatus { OPEN, CLOSED }`.

`Job` is `@Entity @Table(name = "jobs")` with Lombok `@Getter @Setter`, a `protected Job()` no-arg constructor, and `static Job create(String title, String description, Long salaryMin, Long salaryMax, String location, JobStatus status, Company company)` which sets `createdAt = Instant.now()`. Mapping, which must match `V1__init.sql`:

- `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id`
- `@Column(nullable = false) String title`
- `@Column(columnDefinition = "text") String description`
- `Long salaryMin`, `Long salaryMax` — plain `BIGINT`, deliberately not `BigDecimal`; job salaries are whole currency units and this keeps validation and comparison simple
- `String location`
- `@Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) JobStatus status`
- `@ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "company_id", nullable = false) Company company`
- `@Column(name = "created_at", nullable = false, updatable = false) Instant createdAt`

- [ ] **Step 4: Write `JobRepository`**

`interface JobRepository extends JpaRepository<Job, Long>` plus the delete guard, native for the same reason as Task 3:

```java
@Query(value = "SELECT count(*) > 0 FROM applications WHERE job_id = :id", nativeQuery = true)
boolean existsApplicationByJobId(@Param("id") Long id);
```

- [ ] **Step 5: Write `JobRequest` and `JobResponse`**

```java
public record JobRequest(
        @NotBlank @Size(max = 255) String title,
        String description,
        @PositiveOrZero Long salaryMin,
        @PositiveOrZero Long salaryMax,
        @Size(max = 255) String location,
        @NotNull JobStatus status,
        @NotNull Long companyId) {

    @AssertTrue(message = "salaryMin must be less than or equal to salaryMax")
    public boolean isSalaryRangeValid() {
        return salaryMin == null || salaryMax == null || salaryMin <= salaryMax;
    }
}
```

The `@AssertTrue` method is what makes `salaryMin > salaryMax` a `400` rather than a database `500` from the `CHECK` constraint.

`JobResponse(Long id, String title, String description, Long salaryMin, Long salaryMax, String location, JobStatus status, Long companyId, String companyName, Instant createdAt)` with `static JobResponse from(Job job)` reading `job.getCompany().getId()` and `getName()` — safe inside the service's transaction.

- [ ] **Step 6: Write `JobService` (CRUD only)**

`@Service @RequiredArgsConstructor`, class-level `@Transactional(readOnly = true)`. `create` and `update` load the `Company` by `request.companyId()` and throw `NotFoundException` when absent. `delete` throws `ConflictException` when `existsApplicationByJobId` is true. Leave a clear seam for Task 5's `search` method.

- [ ] **Step 7: Run the service test to verify it passes**

Run: `./gradlew test --tests 're.edu.thuchanh.job.JobServiceTest'`
Expected: PASS — 6 tests, 0 failures.

- [ ] **Step 8: Write `JobController`**

`@RestController @RequestMapping("/jobs") @RequiredArgsConstructor`. `POST` → `201` + `Location`; `GET /{id}`, `PUT /{id}` → `200`; `DELETE /{id}` → `204`. One-line delegations only. `GET /jobs` (search) is deliberately **not** added here — Task 5 owns it.

- [ ] **Step 9: Run the whole suite**

Run: `./gradlew test`
Expected: PASS — all tests from Tasks 2, 3 and 4 green.

- [ ] **Step 10: Commit**

```bash
git add src/main/java/re/edu/thuchanh/job src/test/java/re/edu/thuchanh/job
git commit -m "feat: add Job entity, repository and CRUD endpoints"
```

---

## Task 5: Job search — filtering, sorting, pagination

**Files:**
- Create: `src/main/java/re/edu/thuchanh/common/PageResponse.java`
- Create: `src/main/java/re/edu/thuchanh/common/LikePattern.java`
- Modify: `src/main/java/re/edu/thuchanh/job/JobRepository.java` (add `search`)
- Modify: `src/main/java/re/edu/thuchanh/job/JobService.java` (add `search`)
- Modify: `src/main/java/re/edu/thuchanh/job/JobController.java` (add `GET /jobs`)
- Test: `src/test/java/re/edu/thuchanh/common/LikePatternTest.java`
- Test: `src/test/java/re/edu/thuchanh/job/JobServiceSearchTest.java`
- Test: `src/test/java/re/edu/thuchanh/job/JobControllerTest.java`

**Interfaces:**
- Consumes: `JobRepository`, `JobService`, `JobController`, `JobResponse`, `JobStatus` from Task 4; `BadRequestException` from Task 2.
- Produces: `PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages)` with `static <T> PageResponse<T> of(org.springframework.data.domain.Page<T> page)`. `LikePattern.containing(String raw)` → `String` or `null`. `JobService.search(String keyword, String location, JobStatus status, int page, int size, String sort)` → `PageResponse<JobResponse>`.

- [ ] **Step 1: Write the failing `LikePattern` test**

Pins Review Focus item 3. This is a pure function, so it is fully testable without a database.

```java
class LikePatternTest {

    @Test
    void nullStaysNull() {
        assertThat(LikePattern.containing(null)).isNull();
    }

    @Test
    void plainTextIsWrappedAndLowercased() {
        assertThat(LikePattern.containing("Backend")).isEqualTo("%backend%");
    }

    @Test
    void percentIsEscaped() {
        assertThat(LikePattern.containing("100%")).isEqualTo("%100\\%%");
    }

    @Test
    void underscoreIsEscaped() {
        assertThat(LikePattern.containing("a_b")).isEqualTo("%a\\_b%");
    }

    @Test
    void backslashIsEscapedFirst() {
        assertThat(LikePattern.containing("a\\b")).isEqualTo("%a\\\\b%");
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew test --tests 're.edu.thuchanh.common.LikePatternTest'`
Expected: FAIL — compilation error, `LikePattern` does not exist.

- [ ] **Step 3: Write `LikePattern` and `PageResponse`**

`LikePattern.containing(String raw)`: return `null` for `null`; otherwise escape `\` → `\\` **first**, then `%` → `\%`, then `_` → `\_`, lowercase, and wrap in `%` on both sides. Order matters — escaping the backslash first prevents double-escaping.

`PageResponse.of(Page<T> page)` maps Spring's `Page` to the envelope the spec requires (spec decision 14) rather than serializing Spring's raw `Page`.

- [ ] **Step 4: Run it to verify it passes**

Run: `./gradlew test --tests 're.edu.thuchanh.common.LikePatternTest'`
Expected: PASS — 5 tests, 0 failures.

- [ ] **Step 5: Write the failing search service test**

```java
@ExtendWith(MockitoExtension.class)
class JobServiceSearchTest {

    @Mock JobRepository jobRepository;
    @Mock CompanyRepository companyRepository;
    @InjectMocks JobService jobService;

    @Test
    void searchPassesFiltersToRepositoryAndWrapsPage() {
        Job job = Job.create("Backend Dev", "Build APIs", 1000L, 2000L,
                "Hanoi", JobStatus.OPEN, Company.create("Acme", null, null));
        when(jobRepository.search(eq("%backend%"), eq("hanoi"), eq(JobStatus.OPEN), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(job)));

        PageResponse<JobResponse> result =
                jobService.search("Backend", "Hanoi", JobStatus.OPEN, 0, 20, "title");

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void searchTreatsBlankKeywordAndLocationAsNoFilter() {
        when(jobRepository.search(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        jobService.search("  ", "", null, 0, 20, "title");

        verify(jobRepository).search(isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void searchAcceptsEveryWhitelistedSortField() {
        when(jobRepository.search(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        for (String field : List.of("title", "salaryMin", "salaryMax", "location", "createdAt")) {
            jobService.search(null, null, null, 0, 20, field);
        }
    }

    @Test
    void searchRejectsUnknownSortFieldWithBadRequest() {
        assertThatThrownBy(() -> jobService.search(null, null, null, 0, 20, "password"))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(jobRepository);
    }

    @Test
    void searchAppliesDescendingPrefix() {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        when(jobRepository.search(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        jobService.search(null, null, null, 0, 20, "-createdAt");

        verify(jobRepository).search(any(), any(), any(), captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("createdAt").isDescending()).isTrue();
    }
}
```

- [ ] **Step 6: Run it to verify it fails**

Run: `./gradlew test --tests 're.edu.thuchanh.job.JobServiceSearchTest'`
Expected: FAIL — compilation error, `JobService.search` does not exist.

- [ ] **Step 7: Add `search` to `JobRepository`**

JPQL with a `JOIN FETCH` to avoid an N+1 on `company`, and an explicit `countQuery` because Spring Data cannot derive one from a fetch join. Keyword matches `title` OR `description`, case-insensitively, using the escaped pattern:

```java
@Query(value = """
        SELECT j FROM Job j JOIN FETCH j.company
        WHERE (:keyword IS NULL OR LOWER(j.title) LIKE :keyword ESCAPE '\\'
                                 OR LOWER(j.description) LIKE :keyword ESCAPE '\\')
          AND (:location IS NULL OR LOWER(j.location) = :location)
          AND (:status IS NULL OR j.status = :status)
        """,
        countQuery = """
        SELECT COUNT(j) FROM Job j
        WHERE (:keyword IS NULL OR LOWER(j.title) LIKE :keyword ESCAPE '\\'
                                 OR LOWER(j.description) LIKE :keyword ESCAPE '\\')
          AND (:location IS NULL OR LOWER(j.location) = :location)
          AND (:status IS NULL OR j.status = :status)
        """)
Page<Job> search(@Param("keyword") String keyword,
                 @Param("location") String location,
                 @Param("status") JobStatus status,
                 Pageable pageable);
```

The caller passes an already-lowercased, already-escaped `keyword` (from `LikePattern.containing`) and an already-lowercased `location` — the query compares against `LOWER(...)` and does no escaping of its own.

- [ ] **Step 8: Add `search` to `JobService`**

Normalise first: blank/whitespace keyword and location become `null`; otherwise `keyword = LikePattern.containing(keyword.trim())` and `location = location.trim().toLowerCase()`. Then resolve the sort through a whitelist of `title`, `salaryMin`, `salaryMax`, `location`, `createdAt`; anything else throws `BadRequestException`. Support a leading `-` for descending. Build a `PageRequest` and map through `PageResponse.of`. Sort resolution is a small private helper so the whitelist lives in exactly one place.

- [ ] **Step 9: Run it to verify it passes**

Run: `./gradlew test --tests 're.edu.thuchanh.job.JobServiceSearchTest'`
Expected: PASS — 5 tests, 0 failures.

- [ ] **Step 10: Write the failing controller test**

Pins Review Focus item 2 — Spring Data's default for a negative page is an unhandled `500`, so the controller must validate explicitly.

```java
@WebMvcTest(JobController.class)
class JobControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean JobService jobService;

    @Test
    void searchReturnsEnvelope() throws Exception {
        when(jobService.search(any(), any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0L, 0));

        mockMvc.perform(get("/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void searchRejectsNegativePageWith400() throws Exception {
        mockMvc.perform(get("/jobs").param("page", "-1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(jobService);
    }

    @Test
    void searchRejectsZeroSizeWith400() throws Exception {
        mockMvc.perform(get("/jobs").param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchRejectsUnknownSortWith400() throws Exception {
        when(jobService.search(any(), any(), any(), anyInt(), anyInt(), any()))
                .thenThrow(new BadRequestException("Unsupported sort field: password"));

        mockMvc.perform(get("/jobs").param("sort", "password"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void searchRejectsUnknownStatusWith400() throws Exception {
        mockMvc.perform(get("/jobs").param("status", "BANANA"))
                .andExpect(status().isBadRequest());
    }
}
```

- [ ] **Step 11: Run it to verify it fails**

Run: `./gradlew test --tests 're.edu.thuchanh.job.JobControllerTest'`
Expected: FAIL — `GET /jobs` is not mapped yet.

- [ ] **Step 12: Add `GET /jobs` to `JobController`**

Parameters: `keyword` (optional), `location` (optional), `status` (`JobStatus`, optional), `page` (`@Min(0)`, default `0`), `size` (`@Min(1)`, default `20`), `sort` (default `"title"`). Annotate the controller class `@Validated` so the `@Min` constraints are enforced — that is what turns a negative page into a `400` via `ConstraintViolationException`. An invalid `status` string becomes a `MethodArgumentTypeMismatchException`, already mapped to `400` in Task 2.

- [ ] **Step 13: Run it to verify it passes**

Run: `./gradlew test --tests 're.edu.thuchanh.job.JobControllerTest'`
Expected: PASS — 5 tests, 0 failures.

- [ ] **Step 14: Run the whole suite**

Run: `./gradlew test`
Expected: PASS — all tests from Tasks 2–5 green.

- [ ] **Step 15: Commit**

```bash
git add src/main/java/re/edu/thuchanh/common src/main/java/re/edu/thuchanh/job src/test/java/re/edu/thuchanh/common src/test/java/re/edu/thuchanh/job
git commit -m "feat: add job search with keyword, filters, sorting and pagination"
```

---

## Task 6: Application feature

**Files:**
- Create: `src/main/java/re/edu/thuchanh/application/JobApplication.java`
- Create: `src/main/java/re/edu/thuchanh/application/JobApplicationRepository.java`
- Create: `src/main/java/re/edu/thuchanh/application/dto/ApplicationRequest.java`
- Create: `src/main/java/re/edu/thuchanh/application/dto/ApplicationResponse.java`
- Create: `src/main/java/re/edu/thuchanh/application/JobApplicationService.java`
- Create: `src/main/java/re/edu/thuchanh/application/JobApplicationController.java`
- Test: `src/test/java/re/edu/thuchanh/application/JobApplicationServiceTest.java`

**Interfaces:**
- Consumes: `JobRepository`, `Job`, `JobStatus` (Task 4); the common exceptions (Task 2).
- Produces: entity `JobApplication` with `Long id`, `Job job`, `String fullName`, `String email`, `String cvUrl`, `Instant appliedAt`. `JobApplicationRepository extends JpaRepository<JobApplication, Long>` with `boolean existsByJobIdAndEmail(Long jobId, String email)`. `ApplicationRequest(String fullName, String email, String cvUrl)`, `ApplicationResponse(Long id, Long jobId, String fullName, String email, String cvUrl, Instant appliedAt)`. `JobApplicationService.apply(Long jobId, ApplicationRequest request)` → `ApplicationResponse`.

- [ ] **Step 1: Write the failing service test**

```java
@ExtendWith(MockitoExtension.class)
class JobApplicationServiceTest {

    @Mock JobApplicationRepository applicationRepository;
    @Mock JobRepository jobRepository;
    @InjectMocks JobApplicationService applicationService;

    private Job job(JobStatus status) {
        return Job.create("Backend Dev", null, null, null, null, status,
                Company.create("Acme", null, null));
    }

    private ApplicationRequest request(String email) {
        return new ApplicationRequest("Nguyen Van A", email, "https://cv.example/a.pdf");
    }

    @Test
    void applySavesApplicationAndStampsAppliedAt() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job(JobStatus.OPEN)));
        when(applicationRepository.existsByJobIdAndEmail(1L, "a@example.com")).thenReturn(false);
        when(applicationRepository.save(any(JobApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        ApplicationResponse response = applicationService.apply(1L, request("a@example.com"));

        assertThat(response.appliedAt()).isNotNull();
        assertThat(response.email()).isEqualTo("a@example.com");
    }

    @Test
    void applyThrowsNotFoundWhenJobMissing() {
        when(jobRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.apply(99L, request("a@example.com")))
                .isInstanceOf(NotFoundException.class);

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void applyThrowsConflictWhenJobIsClosed() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job(JobStatus.CLOSED)));

        assertThatThrownBy(() -> applicationService.apply(1L, request("a@example.com")))
                .isInstanceOf(ConflictException.class);

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void applyThrowsConflictWhenEmailAlreadyAppliedToSameJob() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job(JobStatus.OPEN)));
        when(applicationRepository.existsByJobIdAndEmail(1L, "a@example.com")).thenReturn(true);

        assertThatThrownBy(() -> applicationService.apply(1L, request("a@example.com")))
                .isInstanceOf(ConflictException.class);

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void applyNormalisesEmailBeforeCheckingAndSaving() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job(JobStatus.OPEN)));
        when(applicationRepository.existsByJobIdAndEmail(1L, "a@example.com")).thenReturn(false);
        ArgumentCaptor<JobApplication> captor = ArgumentCaptor.forClass(JobApplication.class);
        when(applicationRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        applicationService.apply(1L, request("  A@Example.COM  "));

        assertThat(captor.getValue().getEmail()).isEqualTo("a@example.com");
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew test --tests 're.edu.thuchanh.application.JobApplicationServiceTest'`
Expected: FAIL — compilation error, `JobApplication`, `JobApplicationService` do not exist.

- [ ] **Step 3: Write `JobApplication`**

`@Entity @Table(name = "applications")` with Lombok `@Getter @Setter`, a `protected JobApplication()` no-arg constructor, and `static JobApplication create(Job job, String fullName, String email, String cvUrl)` setting `appliedAt = Instant.now()`. Mapping, which must match `V1__init.sql`:

- `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id`
- `@ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "job_id", nullable = false) Job job`
- `@Column(name = "full_name", nullable = false) String fullName`
- `@Column(nullable = false) String email`
- `@Column(name = "cv_url", nullable = false, length = 1000) String cvUrl`
- `@Column(name = "applied_at", nullable = false, updatable = false) Instant appliedAt`

The entity is named `JobApplication`, not `Application`, to avoid confusion with `@SpringBootApplication` (spec decision 6).

- [ ] **Step 4: Write `JobApplicationRepository`**

`interface JobApplicationRepository extends JpaRepository<JobApplication, Long>` with the derived query `boolean existsByJobIdAndEmail(Long jobId, String email);`.

- [ ] **Step 5: Write the DTOs**

```java
public record ApplicationRequest(
        @NotBlank @Size(max = 255) String fullName,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 1000) String cvUrl) { }
```

`ApplicationResponse(Long id, Long jobId, String fullName, String email, String cvUrl, Instant appliedAt)` with `static ApplicationResponse from(JobApplication application)`.

`cvUrl` is length-checked but not format-checked: the spec calls it a link without requiring URL validation, and a strict pattern would reject legitimate values.

- [ ] **Step 6: Write `JobApplicationService`**

`@Service @RequiredArgsConstructor`. `apply` is `@Transactional` and, in order: load the job (empty → `NotFoundException`); reject a `CLOSED` job with `ConflictException`; normalise the email with `trim().toLowerCase()`; reject an existing `(jobId, email)` with `ConflictException`; save and return. `appliedAt` comes from the entity factory — any client-supplied timestamp is ignored (spec decision 17).

The pre-check is only for a clean message. The real guarantee is the unique constraint: a concurrent duplicate slips past the check and surfaces as a `DataIntegrityViolationException` at commit, which `GlobalExceptionHandler` maps to `409`. Do not add retry logic or locking.

- [ ] **Step 7: Run the test to verify it passes**

Run: `./gradlew test --tests 're.edu.thuchanh.application.JobApplicationServiceTest'`
Expected: PASS — 5 tests, 0 failures.

- [ ] **Step 8: Write `JobApplicationController`**

`@RestController @RequestMapping("/jobs/{jobId}/applications") @RequiredArgsConstructor`, one method: `POST` taking `@PathVariable Long jobId` and `@Valid @RequestBody ApplicationRequest`, returning `201` with `Location: /jobs/{jobId}/applications/{id}`.

No `GET` is added: requirement 1 gives CRUD to Company and Job only, and the spec says to follow `DE_BAI.md` literally.

- [ ] **Step 9: Run the whole suite**

Run: `./gradlew test`
Expected: PASS — all tests from Tasks 2–6 green.

- [ ] **Step 10: Commit**

```bash
git add src/main/java/re/edu/thuchanh/application src/test/java/re/edu/thuchanh/application
git commit -m "feat: add job applications with duplicate and closed-job rejection"
```

---

## Task 7: README and final verification

**Files:**
- Create: `README.md`

**Interfaces:**
- Consumes: every endpoint, migration and configuration from Tasks 1–6.
- Produces: the submission's front door.

- [ ] **Step 1: Write `README.md` in Vietnamese**

Required sections, per requirement 7 and the grading criteria:

- **Giới thiệu** — what the API does, one paragraph.
- **Công nghệ** — Java 17, Spring Boot 3.5.16, PostgreSQL, Flyway, Gradle.
- **Yêu cầu** — JDK 17+, PostgreSQL 14+.
- **Tạo cơ sở dữ liệu** — `CREATE DATABASE jobboard;`. State that Flyway creates all tables on first start from `src/main/resources/db/migration/V1__init.sql`, and that no manual schema step is needed.
- **Cấu hình** — the `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` environment variables and their defaults.
- **Cách chạy** — `./gradlew bootRun`, and `./gradlew build` to produce the jar.
- **Chạy test** — `./gradlew test`. State explicitly that the tests are unit and slice tests and need **no database**.
- **Danh sách API** — a table of every endpoint with method, path, and success status.
- **Ví dụ request** — at least one `curl` per endpoint group: create company, create job, `GET /jobs` with keyword/location/status/page/size/sort, apply to a job, and one example of each error shape (400, 404, 409).
- **Cấu trúc thư mục** — the package-by-feature layout.
- **Thiết kế cơ sở dữ liệu** — the three tables, the FK constraints, the unique constraint on `(job_id, email)`, the `CHECK` on the salary range, and the four indexes.

Use only the endpoints actually implemented. Do not document a `GET /jobs/{id}/applications` — it does not exist.

- [ ] **Step 2: Verify the build and full suite from clean**

Run: `./gradlew clean build`
Expected: `BUILD SUCCESSFUL`, every test green, jar produced in `build/libs/`.

- [ ] **Step 3: Verify the app fails fast without a database, as documented**

Run: `./gradlew bootRun`
Expected: FAILS to start with a Flyway or JDBC connection error naming `localhost:5432`. This is the correct behaviour — it confirms the app really does depend on PostgreSQL and that the README's setup instructions are the ones a grader must follow. Stop the process.

- [ ] **Step 4: Commit**

```bash
git add README.md
git commit -m "docs: add Vietnamese README with setup, migration and API examples"
```

- [ ] **Step 5: Report the migration gap**

State plainly in the final message that `V1__init.sql` was never executed, because no PostgreSQL or Docker daemon is available on this machine. If the human partner can start Docker Desktop, a Testcontainers `@SpringBootTest` would close this gap; otherwise it is the one thing a grader will be first to discover if it is wrong.

---

## Self-Review

**Spec coverage.** Every requirement maps to a task: CRUD Company and Job → Tasks 3, 4; `GET /jobs` search/filter/page/sort → Task 5; `POST /jobs/{id}/applications` with duplicate and closed rejection → Task 6; validation and one error shape for 400/404/409 → Task 2, exercised in Tasks 3 and 5; ≥5 service unit tests → 8 + 6 + 5 + 5 service tests across Tasks 3, 4, 5, 6; public repo with incremental commits → Task 1 initialises it, every task commits; README → Task 7. Spec decisions 1–17: 1–3 → Task 1; 4–7 → Tasks 2–6; 8–11 → Task 1's migration; 12–14 → Task 5; 15–17 → Task 6. Non-goals are absent from every task.

**Step scan.** Each step names one action with a checkable result. Code steps give signatures and the values the spec pins; bodies appear only where the signature and tests leave a choice (the repository JPQL, `LikePattern`'s escaping order, `apply`'s ordering).

**Type consistency.** `Company.create`, `Job.create` and `JobApplication.create` are used with identical argument order in the tasks that define them and the tasks that call them. `PageResponse.of` is defined in Task 5 Step 3 and used in Step 8. `LikePattern.containing` returns `String` and is compared against `null` in the repository query. `existsJobByCompanyId` (Task 3) and `existsApplicationByJobId` (Task 4) are each defined and stubbed with the same names. `JobService.search`'s six-parameter signature is identical in Task 5 Steps 5, 8 and 12.

**Review Focus.** All five items have a named owning task and a concrete test: 1 and 5 → Task 2 `GlobalExceptionHandlerTest`; 2 → Task 5 `JobControllerTest`; 3 → Task 5 `LikePatternTest`; 4 → Task 3 `CompanyControllerTest`.

**Proportion.** The plan is roughly twice the spec's length, which is expected for a spec that fixes decisions but no interfaces. Code blocks are confined to test assertions and the few bodies that are genuinely under-determined.
