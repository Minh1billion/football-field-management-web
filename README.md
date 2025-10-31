# ⚽ UTE Score - Hệ Thống Quản Lý Đặt Sân Bóng Đá

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

Một ứng dụng web toàn diện được xây dựng bằng **Spring Boot** và **Thymeleaf**, hỗ trợ quản lý đặt sân bóng đá trực tuyến với khả năng lập lịch hiệu quả, quản lý người dùng và theo dõi doanh thu.

## 📋 Mục Lục

- [Tổng Quan](#tổng-quan)
- [Tính Năng](#tính-năng)
- [Công Nghệ Sử Dụng](#công-nghệ-sử-dụng)
- [Cấu Trúc Dự Án](#cấu-trúc-dự-án)
- [Hướng Dẫn Cài Đặt](#hướng-dẫn-cài-đặt)
- [Vai Trò Người Dùng](#vai-trò-người-dùng)
- [Thành Viên Nhóm](#thành-viên-nhóm)

## 🎯 Tổng Quan

UTE Score là nền tảng quản lý sân bóng đá hiện đại được thiết kế để tối ưu hóa quy trình đặt sân cho các cơ sở thể thao. Hệ thống hỗ trợ ba vai trò người dùng khác nhau (User, Manager, Admin) và cung cấp các công cụ toàn diện cho quản lý sân, điều phối đặt chỗ và phân tích kinh doanh.

### Điểm Nổi Bật

- 🔐 Xác thực bảo mật với JWT tokens
- 💳 Tích hợp cổng thanh toán VNPay
- 📧 Thông báo email qua Gmail SMTP
- ☁️ Lưu trữ hình ảnh trên cloud với Cloudinary
- 📊 Theo dõi doanh thu và phân tích thời gian thực
- 💬 Hệ thống chat tích hợp giữa người dùng và quản lý

## ✨ Tính Năng

### 👤 Tính Năng Người Dùng

**Quản Lý Tài Khoản**
- Đăng ký và đăng nhập với xác thực JWT
- Quản lý và tùy chỉnh hồ sơ cá nhân
- Đặt lại mật khẩu qua email OTP

**Hệ Thống Đặt Sân**
- Duyệt các sân và khung giờ có sẵn
- Đặt sân theo thời gian thực và xác nhận ngay lập tức
- Xem lịch sử và quản lý đặt sân
- Hủy hoặc chỉnh sửa đặt chỗ

**Giao Tiếp**
- Nhận thông báo từ quản lý
- Nhắn tin trực tiếp với quản lý sân
- Hệ thống đánh giá và nhận xét

**Phần Thưởng & Khách Hàng Thân Thiết**
- Theo dõi lịch sử đặt sân
- Tích điểm thưởng
- Ưu đãi và khuyến mãi đặc biệt

### 🧑‍💼 Tính Năng Quản Lý

**Quản Lý Sân Bóng**
- Thêm, chỉnh sửa và xóa sân bóng
- Cấu hình khung giờ và giá thuê
- Thiết lập lịch trình khả dụng
- Tải lên hình ảnh sân

**Quản Trị Đặt Sân**
- Xem xét và phê duyệt yêu cầu đặt sân
- Quản lý hủy và chỉnh sửa đặt chỗ
- Gửi thông báo cho người dùng

**Phân Tích Kinh Doanh**
- Theo dõi doanh thu và báo cáo
- Thống kê và xu hướng đặt sân
- Thông tin chi tiết về khách hàng

**Giao Tiếp Khách Hàng**
- Trò chuyện với người dùng
- Gửi thông báo chung
- Xử lý các thắc mắc của khách hàng

### 🛠️ Tính Năng Quản Trị Viên

**Quản Lý Người Dùng**
- Quản lý tài khoản và vai trò người dùng
- Phê duyệt hoặc từ chối đơn đăng ký quản lý
- Chặn hoặc khôi phục tài khoản
- Xem nhật ký hoạt động người dùng

**Cấu Hình Hệ Thống**
- Quản lý cài đặt nền tảng
- Cấu hình phương thức thanh toán
- Tùy chỉnh nội dung công khai

**Giám Sát Nền Tảng**
- Theo dõi tất cả đặt chỗ và giao dịch
- Tạo báo cáo toàn hệ thống
- Quản lý giải quyết tranh chấp

## 🛠️ Công Nghệ Sử Dụng

### Backend
| Công Nghệ | Mô Tả |
|-----------|-------|
| **Spring Boot 3.x** | Framework backend chính |
| **Java 17+** | Ngôn ngữ lập trình |
| **Spring Security + JWT** | Xác thực và phân quyền |
| **Spring Data JPA** | ORM (Hibernate) |
| **Microsoft SQL Server** | Cơ sở dữ liệu |
| **Maven** | Công cụ build |

### Frontend
| Công Nghệ | Mô Tả |
|-----------|-------|
| **Thymeleaf** | Template engine |
| **HTML5, CSS3** | Giao diện người dùng |
| **JavaScript (ES6+)** | Tương tác động |

### Dịch Vụ Bên Thứ Ba
| Dịch Vụ | Mục Đích |
|---------|----------|
| **VNPay** | Cổng thanh toán trực tuyến |
| **Cloudinary** | Lưu trữ hình ảnh trên cloud |
| **Gmail SMTP** | Dịch vụ gửi email |
| **JWT** | Xác thực token |
| **GEMINI** | Ngăn chặn bình luận tiêu cực |

## 📁 Cấu Trúc Dự Án

```plaintext
final-project/
│
├── src/
│   └── main/
│       ├── java/utescore/
│       │   ├── config/              # Cấu hình ứng dụng (JWT, Security, CORS)
│       │   ├── controller/          # Xử lý HTTP requests
│       │   │   ├── admin/           # Controller cho Admin
│       │   │   ├── manager/         # Controller cho Manager
│       │   │   ├── publicview/      # Controller trang công khai
│       │   │   └── user/            # Controller cho User
│       │   ├── dto/                 # Data Transfer Objects
│       │   ├── entity/              # JPA Entities
│       │   ├── repository/          # Spring Data JPA Repositories
│       │   ├── service/             # Business Logic
│       │   ├── util/                # Utility classes
│       │   └── FinalProjectApplication.java
│       │
│       └── resources/
│           ├── static/              # CSS, JS, Images
│           ├── templates/           # Thymeleaf Templates
│           │   ├── admin/           # Giao diện Admin
│           │   ├── auth/            # Giao diện đăng nhập/đăng ký
│           │   ├── home/            # Trang chủ
│           │   ├── layout/          # Header/Footer layout
│           │   ├── manager/         # Giao diện Manager
│           │   └── user/            # Giao diện User
│           └── application.properties
│
└── pom.xml                          # Maven dependencies
```

## 🚀 Hướng Dẫn Cài Đặt

### Yêu Cầu Hệ Thống

- **Java:** 17 trở lên
- **Maven:** 3.6+
- **SQL Server:** 2019 trở lên
- **IDE:** IntelliJ IDEA / Eclipse / VS Code (khuyến nghị)

### 1️⃣ Clone Repository

```bash
git clone https://github.com/Minh1billion/football-field-management-web.git
cd football-field-management-web
```

### 2️⃣ Cấu Hình Database

Tạo database trong SQL Server:

```sql
CREATE DATABASE [football-field-management];
```

Mở file `src/main/resources/application.properties` và cập nhật thông tin:

```properties
# Database Configuration
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=football-field-management;encrypt=True;trustServerCertificate=true;
spring.datasource.username=sa
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.SQLServerDialect
```

### 3️⃣ Cấu Hình Dịch Vụ Bên Thứ Ba

Cập nhật các thông tin sau trong `application.properties`:

```properties
# Email Configuration (Gmail SMTP)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your_email@gmail.com
spring.mail.password=your_app_password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# Cloudinary Configuration
cloudinary.cloud-name=your_cloud_name
cloudinary.api-key=your_api_key
cloudinary.api-secret=your_api_secret

# VNPay Configuration
vnpay.tmn-code=your_tmn_code
vnpay.hash-secret=your_hash_secret
vnpay.url=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
vnpay.return-url=http://localhost:8080/payment/vnpay-return

# JWT Configuration
jwt.secret=your_jwt_secret_key_min_256_bits
jwt.expiration=86400000

# Hỗ trợ AI giải quyết bình luận tiêu cực
gemini.api.key=AIzaSyBQOSOKdsIKsxzSRzQAXhdpZuvizNOyqzc
```

### 4️⃣ Build và Chạy Ứng Dụng

**Sử dụng Maven:**

```bash
mvn clean install
mvn spring-boot:run
```

**Sử dụng IDE:**
- Mở project trong IntelliJ IDEA hoặc Eclipse
- Tìm file `FinalProjectApplication.java`
- Click chuột phải và chọn "Run"

### 5️⃣ Truy Cập Ứng Dụng

Mở trình duyệt và truy cập:

```
http://localhost:8080
```

## 👥 Vai Trò Người Dùng

### Tài Khoản Mặc Định

| Vai Trò | Email | Mật Khẩu | Mô Tả |
|---------|-------|----------|-------|
| **Admin** | admin@utescore.com | User@123 | Quản trị viên hệ thống |
| **Manager** | manager@utescore.com | User@123 | Quản lý sân bóng |
| **User** | user@utescore.com | User@123 | Người dùng thông thường |

## 👨‍💻 Thành Viên Nhóm

| Họ và Tên | MSSV | Vai Trò | Email |
|-----------|------|---------|-------|
| **Trần Quang Minh** | 23110269 | Team Leader, Backend Developer | 23110269@student.ute.edu.vn |
| **Huỳnh Duy Nguyên** | 23110270 | Backend Developer, Database Designer | 23110270@student.ute.edu.vn |
| **Trần Trí Tình** | 23110341 | Frontend Developer, UI/UX Designer | 23110341.tt@student.ute.edu.vn |

## 📞 Liên Hệ

- **Website:** [http://ec2-13-212-162-49.ap-southeast-1.compute.amazonaws.com:8080/](http://ec2-13-212-162-49.ap-southeast-1.compute.amazonaws.com:8080/)     Deloy bằng AWS
- **Email:** nhomweb11@gmail.com
- **GitHub:** [Minh1billion](https://github.com/Minh1billion/football-field-management-web)

## 🙏 Lời Cảm Ơn

- Cảm ơn mọi người đã đọc lời viết này !!!

---

<div align="center">
  <p>Được phát triển với ❤️ bởi Nhóm UTE Score</p>
  <p>© 2025 UTE Score. All rights reserved.</p>
</div>
