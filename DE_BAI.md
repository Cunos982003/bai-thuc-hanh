Công nghệ: Java 17, Spring Boot 3, PostgreSQL.

Dữ liệu
- Company: tên, địa chỉ, website
- Job: tiêu đề, mô tả, lương từ–đến, địa điểm, trạng thái OPEN/CLOSED, thuộc Company
- Application: Job, họ tên, email, link CV, thời điểm nộp

API
1. CRUD Company và Job
2. GET /jobs: tìm theo từ khoá, lọc địa điểm và trạng thái, phân trang, sắp xếp
3. POST /jobs/{id}/applications: không nhận trùng email trong cùng một Job; Job CLOSED thì từ chối
4. Kiểm dữ liệu đầu vào, lỗi trả JSON thống nhất (400, 404, 409)
5. Ít nhất 5 unit test cho tầng service

Nộp
- Repo GitHub công khai, commit nhỏ, message rõ
- README: cách chạy, script tạo bảng hoặc migration, ví dụ request

Chấm theo 4 tiêu chí: đúng yêu cầu, cấu trúc code và OOP, thiết kế bảng và truy vấn, Git và README.