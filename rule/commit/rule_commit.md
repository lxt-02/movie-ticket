# Commit Rules

Dự án sử dụng Conventional Commits.

## Format

```text
<type>(<scope>): <description>
```

### Ví dụ:
- `feat(course-service): add course publishing workflow`
- `fix(user-service): prevent duplicate registration`
- `refactor(learning-service): simplify progress tracking logic`

---

## Type

- `feat`: thêm chức năng mới
- `fix`: sửa bug
- `refactor`: tổ chức lại code, không đổi nghiệp vụ chính
- `perf`: tối ưu hiệu năng
- `test`: thêm/sửa test
- `docs`: sửa tài liệu
- `build`: dependency, Maven, Gradle, Docker build
- `ci`: CI/CD
- `chore`: config, maintenance
- `style`: format/import/whitespace, không đổi logic

---

## Scope

Ưu tiên dùng tên service/module, ví dụ:
- `course-service`
- `learning-service`
- `user-service`
- `communication-service`
- `enrollment-service`
- `payment-service`
- `promotion-service`
- `search-service`
- `gateway`
- `database`
- `docker`

*Không dùng đường dẫn file/folder dài làm scope hoặc commit message.*

---

## Description

- Viết bằng tiếng Anh.
- Ngắn gọn, mô tả ý nghĩa thay đổi.
- Dùng động từ như: `add`, `fix`, `update`, `remove`, `improve`, `simplify`, `support`, `optimize`.
- Không viết chung chung như `update code`, `fix code`, `change files`.
- Không có dấu `.` ở cuối.

---

## Rule cho AI

Khi được cung cấp `git status`, `git diff` hoặc danh sách file thay đổi:
1. Phân tích mục đích chính của thay đổi.
2. Chọn `type` phù hợp.
3. Chọn `scope` theo service/module chính.
4. Viết commit theo format:
   ```text
   <type>(<scope>): <description>
   ```
5. Nếu nhiều thay đổi không liên quan nhau, đề xuất tách thành nhiều commit.
6. Không tự bịa feature nếu dữ liệu chưa đủ; cần dựa vào git diff.
7. Commit message phải mô tả what changed, không chỉ liệt kê file đã sửa.

---

## Output mặc định

Nếu người dùng yêu cầu lệnh commit, chỉ trả về:
```bash
git commit -m "<type>(<scope>): <description>"
```
