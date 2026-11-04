# Job Board API

API quản lý tin tuyển dụng viết bằng Spring Boot. Hệ thống cho phép quản lý **công ty**
(Company) và **tin tuyển dụng** (Job), tìm kiếm tin tuyển dụng theo từ khoá / địa điểm /
trạng thái kèm phân trang và sắp xếp, và cho phép ứng viên **nộp đơn ứng tuyển**
(Application) vào một tin tuyển dụng.

## Công nghệ

| Thành phần | Phiên bản |
|---|---|
| Java | 17 |
| Spring Boot | 3.5.16 |
| Spring Data JPA (Hibernate) | theo Spring Boot |
| PostgreSQL | 14+ |
| Flyway | 11 (quản lý schema) |
| Gradle | 9.8.2 (Gradle Wrapper) |

## Yêu cầu

- **JDK 17** trở lên
- **PostgreSQL 14** trở lên đang chạy

## Tạo cơ sở dữ liệu

Chỉ cần tạo database rỗng:

```sql
CREATE DATABASE jobboard;
```

Toàn bộ bảng, khoá ngoại, ràng buộc và index được **Flyway tạo tự động** khi ứng dụng
khởi động lần đầu, từ file migration:

```
src/main/resources/db/migration/V1__init.sql
```

**Không cần chạy script tạo bảng thủ công.** Nếu muốn xem trước schema, nội dung file
migration ở mục [Thiết kế cơ sở dữ liệu](#thiết-kế-cơ-sở-dữ-liệu) bên dưới.

Hibernate được cấu hình `ddl-auto: validate` — nghĩa là Hibernate **không** tự tạo bảng,
chỉ kiểm tra entity có khớp với schema do Flyway tạo hay không.

## Cấu hình

Ứng dụng đọc cấu hình kết nối từ biến môi trường. Nếu không đặt, các giá trị mặc định
sau được dùng:

| Biến môi trường | Mặc định |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/jobboard` |
| `DB_USERNAME` | `postgres` |
| `DB_PASSWORD` | `postgres` |

Ví dụ đặt lại thông tin kết nối:

```bash
export DB_URL="jdbc:postgresql://localhost:5432/jobboard"
export DB_USERNAME="postgres"
export DB_PASSWORD="matkhau_cua_ban"
```

## Cách chạy

Chạy trực tiếp bằng Gradle Wrapper:

```bash
./gradlew bootRun
```

Ứng dụng lắng nghe tại `http://localhost:8080`.

Đóng gói thành file jar:

```bash
./gradlew build
java -jar build/libs/thuchanh-0.0.1-SNAPSHOT.jar
```

## Chạy test

```bash
./gradlew test
```

Bộ test gồm **unit test tầng service** (Mockito) và **slice test tầng web**
(`@WebMvcTest` / MockMvc). Toàn bộ test **không cần database** — không cần PostgreSQL,
không cần Docker. Có thể chạy test ngay sau khi clone mà không cần cấu hình gì thêm.

## Danh sách API

### Company

| Method | Đường dẫn | Mô tả | Thành công |
|---|---|---|---|
| `POST` | `/companies` | Tạo công ty | `201` |
| `GET` | `/companies` | Danh sách công ty | `200` |
| `GET` | `/companies/{id}` | Chi tiết công ty | `200` |
| `PUT` | `/companies/{id}` | Cập nhật công ty | `200` |
| `DELETE` | `/companies/{id}` | Xoá công ty | `204` |

### Job

| Method | Đường dẫn | Mô tả | Thành công |
|---|---|---|---|
| `POST` | `/jobs` | Tạo tin tuyển dụng | `201` |
| `GET` | `/jobs` | Tìm kiếm / lọc / phân trang / sắp xếp | `200` |
| `GET` | `/jobs/{id}` | Chi tiết tin tuyển dụng | `200` |
| `PUT` | `/jobs/{id}` | Cập nhật tin tuyển dụng | `200` |
| `DELETE` | `/jobs/{id}` | Xoá tin tuyển dụng | `204` |

`GET /jobs` nhận các tham số truy vấn:

| Tham số | Kiểu | Mặc định | Ghi chú |
|---|---|---|---|
| `keyword` | chuỗi | — | Tìm trong **tiêu đề HOẶC mô tả**, không phân biệt hoa thường |
| `location` | chuỗi | — | Lọc theo địa điểm, không phân biệt hoa thường |
| `status` | `OPEN` \| `CLOSED` | — | Lọc theo trạng thái |
| `page` | số nguyên ≥ 0 | `0` | Trang, bắt đầu từ 0 |
| `size` | số nguyên ≥ 1 | `20` | Số phần tử mỗi trang |
| `sort` | chuỗi | `title` | Xem danh sách hợp lệ bên dưới |

Giá trị `sort` hợp lệ: `title`, `salaryMin`, `salaryMax`, `location`, `createdAt`.
Thêm dấu `-` ở đầu để sắp xếp giảm dần, ví dụ `-createdAt`. Giá trị khác sẽ trả về `400`.

Kết quả trả về dạng envelope:

```json
{
  "content": [ /* danh sách job */ ],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3
}
```

### Application

| Method | Đường dẫn | Mô tả | Thành công |
|---|---|---|---|
| `POST` | `/jobs/{jobId}/applications` | Nộp đơn ứng tuyển vào một job | `201` |

`appliedAt` do server tự gán; giá trị thời gian do client gửi lên (nếu có) sẽ bị bỏ qua.

## Ví dụ request

### Tạo công ty

```bash
curl -X POST http://localhost:8080/companies \
  -H "Content-Type: application/json" \
  -d '{
        "name": "Acme Technology",
        "address": "Hà Nội",
        "website": "https://acme.example"
      }'
```

`201 Created`, header `Location: /companies/1`:

```json
{
  "id": 1,
  "name": "Acme Technology",
  "address": "Hà Nội",
  "website": "https://acme.example"
}
```

### Tạo tin tuyển dụng

```bash
curl -X POST http://localhost:8080/jobs \
  -H "Content-Type: application/json" \
  -d '{
        "title": "Backend Developer",
        "description": "Xây dựng REST API bằng Spring Boot",
        "salaryMin": 15000000,
        "salaryMax": 25000000,
        "location": "Hà Nội",
        "status": "OPEN",
        "companyId": 1
      }'
```

`201 Created`:

```json
{
  "id": 1,
  "title": "Backend Developer",
  "description": "Xây dựng REST API bằng Spring Boot",
  "salaryMin": 15000000,
  "salaryMax": 25000000,
  "location": "Hà Nội",
  "status": "OPEN",
  "companyId": 1,
  "companyName": "Acme Technology",
  "createdAt": "2026-11-04T10:15:30.123Z"
}
```

### Tìm kiếm tin tuyển dụng

```bash
# Tìm theo từ khoá "backend", ở Hà Nội, chỉ job đang mở, trang 0, 10 phần tử, mới nhất trước
curl -G http://localhost:8080/jobs \
  --data-urlencode "keyword=backend" \
  --data-urlencode "location=Hà Nội" \
  --data-urlencode "status=OPEN" \
  --data-urlencode "page=0" \
  --data-urlencode "size=10" \
  --data-urlencode "sort=-createdAt"
```

```json
{
  "content": [
    {
      "id": 1,
      "title": "Backend Developer",
      "description": "Xây dựng REST API bằng Spring Boot",
      "salaryMin": 15000000,
      "salaryMax": 25000000,
      "location": "Hà Nội",
      "status": "OPEN",
      "companyId": 1,
      "companyName": "Acme Technology",
      "createdAt": "2026-11-04T10:15:30.123Z"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1
}
```

### Nộp đơn ứng tuyển

```bash
curl -X POST http://localhost:8080/jobs/1/applications \
  -H "Content-Type: application/json" \
  -d '{
        "fullName": "Nguyễn Văn A",
        "email": "a@example.com",
        "cvUrl": "https://cv.example/nguyen-van-a.pdf"
      }'
```

`201 Created`, header `Location: /jobs/1/applications/1`:

```json
{
  "id": 1,
  "jobId": 1,
  "fullName": "Nguyễn Văn A",
  "email": "a@example.com",
  "cvUrl": "https://cv.example/nguyen-van-a.pdf",
  "appliedAt": "2026-11-04T10:20:00.456Z"
}
```

### Các dạng lỗi

Mọi lỗi `400`, `404`, `409` đều trả về **cùng một cấu trúc JSON**:

```json
{
  "timestamp": "2026-11-04T10:25:00.789Z",
  "status": 404,
  "error": "Not Found",
  "message": "Company 999 not found",
  "path": "/companies/999",
  "fieldErrors": []
}
```

`fieldErrors` chỉ có phần tử khi lỗi đến từ kiểm tra dữ liệu đầu vào:

```json
{
  "timestamp": "2026-11-04T10:26:00.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/companies",
  "fieldErrors": [
    { "field": "name", "message": "must not be blank" }
  ]
}
```

**`400 Bad Request`** — dữ liệu đầu vào không hợp lệ:

```bash
# Tên công ty để trống
curl -X POST http://localhost:8080/companies \
  -H "Content-Type: application/json" \
  -d '{"name": ""}'

# salaryMin lớn hơn salaryMax
curl -X POST http://localhost:8080/jobs \
  -H "Content-Type: application/json" \
  -d '{"title": "Dev", "salaryMin": 5000, "salaryMax": 1000,
       "status": "OPEN", "companyId": 1}'

# sort không nằm trong danh sách cho phép
curl "http://localhost:8080/jobs?sort=password"

# page âm
curl "http://localhost:8080/jobs?page=-1"
```

**`404 Not Found`** — không tìm thấy tài nguyên:

```bash
curl http://localhost:8080/companies/999
```

**`409 Conflict`** — xung đột với trạng thái hiện tại:

```bash
# Nộp trùng email cho cùng một job
curl -X POST http://localhost:8080/jobs/1/applications \
  -H "Content-Type: application/json" \
  -d '{"fullName": "Nguyễn Văn A", "email": "a@example.com",
       "cvUrl": "https://cv.example/a.pdf"}'

# Nộp vào job đã đóng
curl -X POST http://localhost:8080/jobs/2/applications \
  -H "Content-Type: application/json" \
  -d '{"fullName": "Trần Thị B", "email": "b@example.com",
       "cvUrl": "https://cv.example/b.pdf"}'

# Xoá công ty vẫn còn tin tuyển dụng
curl -X DELETE http://localhost:8080/companies/1

# Xoá job vẫn còn đơn ứng tuyển
curl -X DELETE http://localhost:8080/jobs/1
```

> Lưu ý: hệ thống **không** xoá dây chuyền (cascade). Muốn xoá công ty phải xoá hết
> job của công ty đó trước; muốn xoá job phải xoá hết đơn ứng tuyển trước.

## Cấu trúc thư mục

Code được tổ chức **theo tính năng (package by feature)**, mỗi package chứa đầy đủ
entity, repository, service, controller và DTO của riêng nó:

```
src/main/java/re/edu/thuchanh/
├── ThuchanhApplication.java
├── common/                  # dùng chung
│   ├── NotFoundException.java
│   ├── ConflictException.java
│   ├── BadRequestException.java
│   ├── ErrorResponse.java
│   ├── GlobalExceptionHandler.java
│   ├── PageResponse.java
│   └── LikePattern.java
├── company/
│   ├── Company.java
│   ├── CompanyRepository.java
│   ├── CompanyService.java
│   ├── CompanyController.java
│   └── dto/
│       ├── CompanyRequest.java
│       └── CompanyResponse.java
├── job/
│   ├── Job.java
│   ├── JobStatus.java
│   ├── JobRepository.java
│   ├── JobService.java
│   ├── JobController.java
│   └── dto/
│       ├── JobRequest.java
│       └── JobResponse.java
└── application/
    ├── JobApplication.java
    ├── JobApplicationRepository.java
    ├── JobApplicationService.java
    ├── JobApplicationController.java
    └── dto/
        ├── ApplicationRequest.java
        └── ApplicationResponse.java
```

Nguyên tắc:

- **Entity không bao giờ được trả trực tiếp ra API.** Mọi lớp biên đều dùng DTO
  (Java `record`), entity chỉ tồn tại trong tầng persistence.
- **Logic nghiệp vụ nằm ở tầng service**, controller chỉ nhận request và gọi service.
- **Lỗi được xử lý tập trung** tại một `@RestControllerAdvice` duy nhất.
- Entity `JobApplication` được đặt tên có tiền tố `Job` để tránh nhầm lẫn với
  `@SpringBootApplication` / `SpringApplication`.

## Thiết kế cơ sở dữ liệu

Schema do Flyway quản lý trong `src/main/resources/db/migration/V1__init.sql`:

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

Điểm đáng chú ý:

- **Khoá ngoại có ràng buộc thật**: `jobs.company_id → companies.id` và
  `applications.job_id → jobs.id`.
- **`UNIQUE (job_id, email)`** trên `applications` là bảo đảm thật cho yêu cầu
  "không nhận trùng email trong cùng một Job". Tầng service cũng kiểm tra trước để
  trả về thông báo lỗi rõ ràng, nhưng bản thân kiểm tra đó có khe hở chủng tộc
  (race condition) — ràng buộc ở database mới là chốt chặn cuối. Khi ràng buộc bị vi
  phạm, `DataIntegrityViolationException` được ánh xạ thành `409`.
- **`CHECK (salary_min <= salary_max)`** bảo đảm khoảng lương hợp lệ ở tầng database.
  Cả hai cột đều cho phép `NULL`.
- **`status` lưu dạng `VARCHAR`** (`OPEN` / `CLOSED`) thay vì số thứ tự, để dữ liệu
  đọc được trực tiếp và không phụ thuộc vào thứ tự khai báo enum.
- **Bốn index** phục vụ các truy vấn lọc và join thường dùng: theo địa điểm, theo
  trạng thái, theo công ty, và theo job khi lấy đơn ứng tuyển.
- **`salary_min` / `salary_max` dùng `BIGINT`** vì lương là số tiền nguyên (đơn vị
  tiền tệ), không cần phần thập phân.
