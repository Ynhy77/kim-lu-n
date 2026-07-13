# AGM MD5 FLOATING ANALYZER

Ứng dụng đầy đủ bao gồm **Android Native App (Kotlin, Jetpack Compose)** và **Node.js/TypeScript Express Backend Server** được thiết kế an toàn, hiệu quả, tuân thủ hoàn toàn các nguyên tắc bảo mật và hạn chế quyền can thiệp hệ thống của Google Play.

---

## 📱 1. Hướng Dẫn Android App

### Công nghệ sử dụng
* **Ngôn ngữ:** Kotlin
* **UI Framework:** Jetpack Compose, Material 3
* **Kiến trúc:** MVVM (Model-View-ViewModel) + Coroutines
* **Bộ nhớ:** Android DataStore (mã hóa thông tin cấu hình và cài đặt)
* **Mạng:** Retrofit Client kết nối API bảo mật
* **Cửa sổ nổi:** Background Foreground Service + WindowManager (`TYPE_APPLICATION_OVERLAY`)

### Hướng dẫn xây dựng (Build APK)
1. Mở thư mục gốc của dự án bằng **Android Studio**.
2. Đồng bộ dự án với Gradle (Gradle Sync).
3. Đảm bảo cấu hình đường dẫn API Backend của bạn trong màn hình **Cấu hình (Settings)** của ứng dụng hoặc trong tệp cấu hình mặc định.
4. Chọn **Build > Build Bundle(s) / APK(s) > Build APK(s)** để tạo APK gỡ lỗi (`app-debug.apk`).
   * Hoặc chạy lệnh gradle sau từ thư mục gốc:
     ```bash
     ./gradlew assembleDebug
     ```

### Cơ chế bong bóng nổi (Overlay Bubble)
* Sử dụng quyền `SYSTEM_ALERT_WINDOW` để hiển thị nút tròn nổi tiện ích.
* Khi người dùng nhấp vào nút nổi, giao diện phân tích MD5 sẽ mở rộng ra ngay trên màn hình game mà không làm gián đoạn trò chơi.
* **Cơ chế Focus bàn phím:** Khi nhấp vào ô nhập MD5 trong bong bóng nổi, ứng dụng sẽ tạm thời gỡ bỏ cờ `FLAG_NOT_FOCUSABLE` để bàn phím Android xuất hiện và cho phép dán hoặc nhập dữ liệu. Khi tắt ô nhập, cờ sẽ được trả lại giúp trải nghiệm chạm vuốt các ứng dụng bên dưới mượt mà.

---

## 💻 2. Hướng Dẫn Backend Server (Node.js & TypeScript)

Backend đóng vai trò kiểm soát thời gian hết hạn, quản lý các lượt yêu cầu tạo key, và tích hợp bộ rút gọn link kiếm tiền **Link4m** mà không để lộ khóa API trong mã nguồn Android.

### Cấu trúc thư mục
```
backend/
  ├── dist/                  # Tệp JS biên dịch
  ├── src/
  │    ├── database/         # Bộ lưu trữ SQLite/JSON bền vững
  │    ├── middleware/       # Bộ lọc Rate Limit, Auth JWT
  │    ├── routes/           # Định tuyến API (/key, /access)
  │    ├── services/         # Key logic, Link4m integration
  │    └── server.ts         # Điểm khởi chạy Express
  ├── package.json
  └── tsconfig.json
```

### Các biến môi trường cần cấu hình (`.env`)
Tạo tệp `.env` bên trong thư mục `backend/` từ mẫu `.env.example`:
```env
PORT=3000
BASE_URL=http://<IP_OR_DOMAIN_BACKEND>:3000
LINK4M_API_KEY=your_link4m_api_key_here
JWT_SECRET=your_long_secure_jwt_secret_phrase
KEY_HASH_SECRET=your_hmac_secret_for_license_keys
```

### Hướng dẫn cài đặt và chạy Backend
1. Đi tới thư mục `backend/`:
   ```bash
   cd backend
   ```
2. Cài đặt các thư viện:
   ```bash
   npm install
   ```
3. Chạy máy chủ ở chế độ phát triển (Tự động tải lại):
   ```bash
   npm run dev
   ```
4. Biên dịch và chạy chế độ sản xuất:
   ```bash
   npm run build
   ```

---

## ⚙️ 3. Quy Trình Xác Minh & Kích Hoạt Key (Bypass Link4m)

Để đảm bảo doanh thu quảng cáo và hạn chế lạm dụng, ứng dụng triển khai quy trình bypass an toàn 100%:

1. **Yêu cầu Key:**
   * Trong màn hình khóa (Lock Screen), người dùng nhấn **LẤY KEY**.
   * App gửi `installationId` (mã định danh duy nhất của thiết bị) lên backend (`POST /api/key/create`).
   * Backend tạo một phiên giao dịch tạm thời (Session) thời hạn 2 giờ, lưu Hash của Key vào Database, sau đó gửi URL gốc (`/api/access/verify?sessionId=...`) qua API rút gọn Link4m để nhận về bypass link.
   * Backend trả về Key (`AGM-XXXXXX`) kèm theo link rút gọn Link4m cho App hiển thị.

2. **Vượt Link:**
   * Người dùng nhấn vào link rút gọn trên App để mở trình duyệt, thực hiện vượt link quảng cáo Link4m.
   * Sau khi hoàn tất bypass, trình duyệt sẽ tự động chuyển hướng về trang xác thực của bạn (`GET /api/access/verify?sessionId=...`).
   * Máy chủ nhận yêu cầu này và cập nhật trạng thái phiên thành `used = 1`. Người dùng thấy màn hình thông báo vượt link thành công và được khuyên quay lại app.

3. **Kích Hoạt Trong App:**
   * Người dùng quay lại App, nhập Key (`AGM-XXXXXX`) và nhấn **KÍCH HOẠT**.
   * App gửi yêu cầu xác minh (`POST /api/access/check`).
   * Máy chủ kiểm tra:
     * Nếu Key đúng + Phiên của thiết bị đã được đổi trạng thái thành `used = 1` + Chưa hết hạn 2 giờ: Máy chủ phản hồi thành công và cấp một mã JWT Token mã hóa thông tin kích hoạt.
     * Nếu Key đúng nhưng người dùng chưa vượt link rút gọn: Trả về lỗi yêu cầu vượt link trước. Đồng thời cộng 1 điểm phạt nhập sai/kích hoạt láo. Nhập sai quá 5 lần sẽ khóa Key vĩnh viễn.
   * Khi kích hoạt thành công, App lưu JWT Token vào bộ nhớ bảo mật DataStore để duy trì trạng thái hoạt động.

4. **Kiểm Tra Khi Bật Bong Bóng:**
   * Mỗi khi bật bong bóng nổi, MainActivity gọi API `POST /api/access/check-token` để kiểm tra độ tin cậy của Token lưu cục bộ. Nếu Token đã hết hạn hoặc không hợp lệ, bong bóng sẽ tự đóng và đưa người dùng về màn hình khóa để yêu cầu lấy key mới, đảm bảo tính chặt chẽ.

---

## 🎲 4. Thuật Toán Phân Tích MD5 (Deterministic MD5 Mapping)

Ứng dụng cam kết rõ ràng: **Mọi kết quả hiển thị đều dựa trên phép toán học cố định (Deterministic) từ chuỗi MD5 đầu vào**, hoàn toàn không chứa yếu tố may rủi, ngẫu nhiên (Random), hack hoặc can thiệp trò chơi thật.

### Công thức tính toán:
1. Nhận chuỗi MD5 dạng HEX gồm 32 ký tự.
2. Trích xuất **6 ký tự đầu tiên** của chuỗi MD5.
3. Chia thành **3 cặp ký tự** (mỗi cặp gồm 2 ký tự).
4. Đổi mỗi cặp ký tự từ dạng Hexadecimal (Hệ 16) sang số nguyên Decimal (Hệ 10).
5. Ánh xạ mỗi số nguyên thu được sang giá trị xúc xắc (từ 1 đến 6) bằng công thức:
   $$\text{DiceValue} = (\text{DecimalValue} \pmod 6) + 1$$
6. Cộng tổng của 3 viên xúc xắc lại:
   * Nếu tổng nằm trong khoảng từ **3 đến 10** $\rightarrow$ Kết quả là **XỈU**.
   * Nếu tổng nằm trong khoảng từ **11 đến 18** $\rightarrow$ Kết quả là **TÀI**.

**Ví dụ thực tế:**
* Chuỗi MD5: `71b3e8c9735d64cbb0f6d62a34b288e4`
* 6 ký tự đầu: `71b3e8` $\rightarrow$ chia thành: `71`, `b3`, `e8`.
* Chuyển đổi sang Decimal:
  * `71` (Hex) = `113` (Dec) $\rightarrow$ $(113 \pmod 6) + 1 = 5 + 1 = 6$
  * `b3` (Hex) = `179` (Dec) $\rightarrow$ $(179 \pmod 6) + 1 = 5 + 1 = 6$
  * `e8` (Hex) = `232` (Dec) $\rightarrow$ $(232 \pmod 6) + 1 = 4 + 1 = 5$
* Tổng 3 xúc xắc: $6 + 6 + 5 = 17$
* Kết quả phân tích: **TÀI** (Do $17 \ge 11$).
