# SMASH-VN — Hệ thống bán vợt cầu lông

SMASH-VN là website demo hỗ trợ kinh doanh các sản phẩm cầu lông. Hệ thống cung cấp các chức năng cơ bản cho khách hàng như xem và tìm kiếm sản phẩm, quản lý giỏ hàng, đặt hàng, áp dụng khuyến mãi và theo dõi đơn hàng; đồng thời hỗ trợ phía quản trị quản lý sản phẩm, tồn kho, đơn hàng, khách hàng, khuyến mãi, bán hàng tại quầy (POS) và báo cáo doanh thu.

## Trạng thái dự án

> [!IMPORTANT]
> Đây là dự án mang tính chất **demo/phục vụ học tập** và **chưa được áp dụng vào thực tế**. Không sử dụng hệ thống trong môi trường vận hành hoặc cho giao dịch thực tế khi chưa được kiểm thử, hoàn thiện bảo mật và triển khai phù hợp.

## Tác giả

Dự án được xây dựng và phát triển bởi:

- **LuongHiep334**
- **Tinhrenzi**

## 🌐 Socials
[![Discord](https://img.shields.io/badge/Discord-%237289DA.svg?logo=discord&logoColor=white)](htttps://discord.gg/tinhxuannn) [![Facebook](https://img.shields.io/badge/Facebook-%231877F2.svg?logo=Facebook&logoColor=white)](https://facebook.com/TinhXuannn) [![Instagram](https://img.shields.io/badge/Instagram-%23E4405F.svg?logo=Instagram&logoColor=white)](https://instagram.com/TinhXuannn) 

# 💻Tech Stack
![C](https://img.shields.io/badge/c-%2300599C.svg?style=for-the-badge&logo=c&logoColor=white) ![HTML5](https://img.shields.io/badge/html5-%23E34F26.svg?style=for-the-badge&logo=html5&logoColor=white) ![CSS3](https://img.shields.io/badge/css3-%231572B6.svg?style=for-the-badge&logo=css3&logoColor=white) ![Java 21](https://img.shields.io/badge/java-21-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white) ![JavaScript](https://img.shields.io/badge/javascript-%23323330.svg?style=for-the-badge&logo=javascript&logoColor=%23F7DF1E) ![Python](https://img.shields.io/badge/python-3670A0?style=for-the-badge&logo=python&logoColor=ffdd54) ![Bootstrap](https://img.shields.io/badge/bootstrap-%23563D7C.svg?style=for-the-badge&logo=bootstrap&logoColor=white) ![Vue.js](https://img.shields.io/badge/vuejs-%2335495e.svg?style=for-the-badge&logo=vuedotjs&logoColor=%234FC08D) ![Spring](https://img.shields.io/badge/spring_boot-4.0.6-%236DB33F.svg?style=for-the-badge&logo=springboot&logoColor=white) ![NodeJS](https://img.shields.io/badge/node.js-6DA55F?style=for-the-badge&logo=node.js&logoColor=white) ![Apache](https://img.shields.io/badge/apache-%23D42029.svg?style=for-the-badge&logo=apache&logoColor=white) ![Microsoft SQL Server](https://img.shields.io/badge/sql_server-%23CC2927.svg?style=for-the-badge&logo=microsoftsqlserver&logoColor=white) ![Adobe Photoshop](https://img.shields.io/badge/adobephotoshop-%2331A8FF.svg?style=for-the-badge&logo=adobephotoshop&logoColor=white) ![Canva](https://img.shields.io/badge/Canva-%2300C4CC.svg?style=for-the-badge&logo=Canva&logoColor=white) 	![Figma](https://img.shields.io/badge/figma-%23F24E1E.svg?style=for-the-badge&logo=figma&logoColor=white)
# 📊GitHub Stats :
![](https://github-readme-stats.vercel.app/api?username=tinhrenzi&theme=tokyonight&hide_border=false&include_all_commits=false&count_private=false)<br/>
![](https://github-readme-streak-stats.herokuapp.com/?user=tinhrenzi&theme=tokyonight&hide_border=false)<br/>
![](https://github-readme-stats.vercel.app/api/top-langs/?username=tinhrenzi&theme=tokyonight&hide_border=false&include_all_commits=false&count_private=false&layout=compact)

---
[![](https://visitcount.itsvg.in/api?id=tinhrenzi&icon=0&color=0)](https://visitcount.itsvg.in)

## 🛠️ Dữ liệu mẫu môi trường dev (Professional Data Seeder)

Hệ thống hỗ trợ cơ chế tạo dữ liệu mẫu cho môi trường phát triển (profile `dev`). Seeder mặc định **TẮT** và chạy ở chế độ **DRY-RUN** (chỉ đọc và kiểm tra, không ghi database).

> [!WARNING]
> Cơ chế seed chỉ được sử dụng cho profile `dev`. Cấm kích hoạt trên môi trường Production!

### 1. Cách chạy Dry-Run (An toàn, không ghi Database)
Mở PowerShell tại thư mục gốc của dự án và chạy:

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
$env:APP_SEED_ENABLED="true"
$env:APP_SEED_DRY_RUN="true"
$env:APP_SEED_INCLUDE_COMMERCE="false"
$env:APP_SEED_PRODUCT_IMAGE_ROOT="uploads/product"
$env:APP_PASSWORD_MIGRATION_ENABLED="false"
$env:SPRING_FLYWAY_ENABLED="false"
$env:SPRING_JPA_HIBERNATE_DDL_AUTO="validate"
.\mvnw.cmd spring-boot:run
```

### 2. Cách chạy Commit trong tương lai (Khi được người dùng xác nhận)

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
$env:APP_SEED_ENABLED="true"
$env:APP_SEED_DRY_RUN="false"
$env:APP_SEED_INCLUDE_COMMERCE="true"
$env:APP_SEED_PRODUCT_IMAGE_ROOT="uploads/product"
$env:APP_PASSWORD_MIGRATION_ENABLED="false"
$env:SPRING_FLYWAY_ENABLED="false"
$env:SPRING_JPA_HIBERNATE_DDL_AUTO="validate"
.\mvnw.cmd spring-boot:run
```

### 3. Dọn dẹp biến môi trường sau khi chạy

```powershell
Remove-Item Env:APP_SEED_ENABLED -ErrorAction SilentlyContinue
Remove-Item Env:APP_SEED_DRY_RUN -ErrorAction SilentlyContinue
Remove-Item Env:APP_SEED_INCLUDE_COMMERCE -ErrorAction SilentlyContinue
Remove-Item Env:APP_SEED_PRODUCT_IMAGE_ROOT -ErrorAction SilentlyContinue
Remove-Item Env:APP_PASSWORD_MIGRATION_ENABLED -ErrorAction SilentlyContinue
```

### 4. Tài khoản thử nghiệm (Test Accounts)
- **Quản trị:** `admin` (Role: `QL`) - Mật khẩu: `123456`
- **Nhân viên:** `nhanvien01` (Role: `NV`) - Mật khẩu: `123456`
- **Khách hàng:** `khachhang01` đến `khachhang12` (Role: `KH`) - Mật khẩu: `123456`

### 5. Ghi chú quan trọng
- CÓ 33 ảnh UUID dạng mồ côi nằm trực tiếp tại gốc `uploads/product/` bị bỏ qua (không seed, không xóa và không tự suy đoán).
- Đường dẫn ảnh lưu trong Database là đường dẫn tương đối so với `uploads/product/`.
