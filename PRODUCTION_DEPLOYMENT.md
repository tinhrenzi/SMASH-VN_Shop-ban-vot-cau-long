# Hướng dẫn Thiết lập Biến Môi trường Production - SMASH VN Shop

Dự án SMASH VN Shop đã được cấu hình bảo mật loại bỏ hoàn toàn các credentials nhạy cảm khỏi mã nguồn. Khi triển khai lên môi trường Production (hoặc Docker), bạn bắt buộc phải cấu hình các biến môi trường dưới đây.

## 1. Danh sách các Biến Môi trường Bắt buộc

| Tên Biến Môi Trường | Mô Tả | Ví dụ Giá Trị |
|---|---|---|
| `DB_USERNAME` | Tên người dùng kết nối SQL Server | `smash_prod_user` |
| `DB_PASSWORD` | Mật khẩu kết nối SQL Server | `SuperStrongPassword123!` |
| `GMAIL_USERNAME`| Email gửi thông báo hệ thống | `smash.support@gmail.com` |
| `GMAIL_PASSWORD`| Mật khẩu ứng dụng của Gmail (App Password) | `abcd efgh ijkl mnop` |
| `GOOGLE_CLIENT_ID`| Client ID của Google OAuth2 App | `1083390992152-...apps.googleusercontent.com` |
| `GOOGLE_CLIENT_SECRET`| Client Secret của Google OAuth2 App | `GOCSPX-fAx...` |
| `APP_ADMIN_EMAILS`| Danh sách email admin nhận tin duyệt khóa (ngăn cách bằng dấu phẩy) | `admin1@gmail.com,admin2@gmail.com` |
| `APP_UPLOAD_PATH` | **Đường dẫn tuyệt đối** đến thư mục lưu trữ ảnh sản phẩm tải lên | `/var/www/smashvn/uploads/product/` (Linux) hoặc `D:/smashvn/uploads/product/` (Windows) |

---

## 2. Hướng dẫn thiết lập trên các Môi trường khác nhau

### A. Chạy thủ công trên Server (Command Line)
Thiết lập các biến trước khi chạy file JAR:

**Trên Linux/macOS:**
```bash
export DB_USERNAME="smash_prod_user"
export DB_PASSWORD="SuperStrongPassword123!"
export GMAIL_USERNAME="smash.support@gmail.com"
export GMAIL_PASSWORD="abcd efgh ijkl mnop"
export GOOGLE_CLIENT_ID="your_google_client_id"
export GOOGLE_CLIENT_SECRET="your_google_client_secret"
export APP_ADMIN_EMAILS="admin1@gmail.com,admin2@gmail.com"
export APP_UPLOAD_PATH="/var/www/smashvn/uploads/product/"

# Chạy ứng dụng
java -jar target/SMASH-VN-0.0.1-SNAPSHOT.jar
```

**Trên Windows (PowerShell):**
```powershell
$env:DB_USERNAME="smash_prod_user"
$env:DB_PASSWORD="SuperStrongPassword123!"
$env:GMAIL_USERNAME="smash.support@gmail.com"
$env:GMAIL_PASSWORD="abcd efgh ijkl mnop"
$env:GOOGLE_CLIENT_ID="your_google_client_id"
$env:GOOGLE_CLIENT_SECRET="your_google_client_secret"
$env:APP_ADMIN_EMAILS="admin1@gmail.com,admin2@gmail.com"
$env:APP_UPLOAD_PATH="D:/smashvn/uploads/product/"

# Chạy ứng dụng
java -jar target/SMASH-VN-0.0.1-SNAPSHOT.jar
```

---

### B. Triển khai bằng Docker Compose (`docker-compose.yml`)
Khai báo các biến môi trường trong khối `environment`:

```yaml
version: '3.8'
services:
  app:
    image: smash-vn-shop:latest
    ports:
      - "8080:8080"
    environment:
      - DB_USERNAME=smash_prod_user
      - DB_PASSWORD=SuperStrongPassword123!
      - GMAIL_USERNAME=smash.support@gmail.com
      - GMAIL_PASSWORD=abcd efgh ijkl mnop
      - GOOGLE_CLIENT_ID=your_google_client_id
      - GOOGLE_CLIENT_SECRET=your_google_client_secret
      - APP_ADMIN_EMAILS=admin1@gmail.com,admin2@gmail.com
      - APP_UPLOAD_PATH=/app/uploads/product/
      - SPRING_JPA_SHOW_SQL=false
      - SPRING_JPA_DDL_AUTO=validate
    volumes:
      - /var/www/smashvn/uploads:/app/uploads
```

---

## 3. Khắc phục sự cố khi khởi động (Fail-Fast Startup Checks)

Hệ thống được trang bị tính năng kiểm tra an toàn lúc khởi động:
1. Nếu thiếu bất kỳ biến cấu hình nào cho DB hoặc OAuth2, Spring Boot sẽ báo lỗi và dừng chạy ngay lập tức.
2. Nếu thư mục lưu ảnh `APP_UPLOAD_PATH` không tồn tại, hệ thống sẽ cố gắng tạo nó. Nếu không thể tạo (do sai đường dẫn) hoặc không có quyền ghi (permission write), ứng dụng sẽ ném ra lỗi `IllegalStateException` và đóng tiến trình.
   - *Cách xử lý*: Hãy kiểm tra xem tài khoản chạy ứng dụng Java/Docker có quyền đọc/ghi trên đường dẫn `APP_UPLOAD_PATH` hay chưa.
