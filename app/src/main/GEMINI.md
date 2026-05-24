# 🔒 Rule: No Hardcoded Strings

## Mục tiêu (Objective)

Tất cả các chuỗi (strings) xuất hiện trong mã nguồn **KHÔNG ĐƯỢC hardcode trực tiếp**.  
Mọi giá trị chuỗi phải được quản lý tập trung thông qua file cấu hình, hằng số, hoặc hệ thống i18n.

---

## ⚠️ Định nghĩa: Hardcoded String là gì?

Một string bị coi là **hardcoded** khi nó được viết trực tiếp vào logic mã nguồn:

```python
# ❌ HARDCODED - Vi phạm rule
message = "Xin chào, người dùng!"
url = "https://api.example.com/v1/users"
error = "Lỗi kết nối cơ sở dữ liệu"
button_label = "Đăng nhập"
role = "admin"
```

---

## ✅ Quy tắc bắt buộc

### 1. Tách strings vào file cấu hình tập trung

**Python / Django:**
```python
# constants.py hoặc strings.py
class Messages:
    GREETING = "Xin chào, người dùng!"
    LOGIN_BUTTON = "Đăng nhập"
    DB_ERROR = "Lỗi kết nối cơ sở dữ liệu"

class Config:
    API_BASE_URL = os.getenv("API_BASE_URL", "https://api.example.com/v1")
    USER_ROLE_ADMIN = "admin"

# Sử dụng:
print(Messages.GREETING)
```

**JavaScript / TypeScript:**
```typescript
// constants/strings.ts
export const MESSAGES = {
  GREETING: "Xin chào, người dùng!",
  LOGIN_BUTTON: "Đăng nhập",
  DB_ERROR: "Lỗi kết nối cơ sở dữ liệu",
} as const;

export const CONFIG = {
  API_BASE_URL: process.env.NEXT_PUBLIC_API_URL ?? "https://api.example.com/v1",
  USER_ROLE_ADMIN: "admin",
} as const;
```

**Java / Kotlin:**
```java
// strings.properties hoặc Constants.java
public final class AppStrings {
    public static final String GREETING = "Xin chào, người dùng!";
    public static final String LOGIN_BUTTON = "Đăng nhập";
    public static final String DB_ERROR = "Lỗi kết nối cơ sở dữ liệu";
}
```

---

### 2. Biến môi trường cho secrets và URLs

```bash
# .env
API_BASE_URL=https://api.example.com/v1
DATABASE_URL=postgresql://localhost:5432/mydb
JWT_SECRET=your-secret-key
```

```python
# ✅ ĐÚNG - Đọc từ environment
import os
API_URL = os.getenv("API_BASE_URL")
DB_URL = os.getenv("DATABASE_URL")
```

```typescript
// ✅ ĐÚNG
const apiUrl = process.env.API_BASE_URL;
const dbUrl = process.env.DATABASE_URL;
```

---

### 3. I18n / Localization cho UI strings

**React (i18next):**
```typescript
// locales/vi.json
{
  "common": {
    "login": "Đăng nhập",
    "logout": "Đăng xuất",
    "error.connection": "Lỗi kết nối"
  }
}

// Component
const { t } = useTranslation();
return <button>{t("common.login")}</button>;
```

**Android:**
```xml
<!-- res/values/strings.xml -->
<string name="btn_login">Đăng nhập</string>
<string name="error_connection">Lỗi kết nối</string>
```

**iOS / Swift:**
```swift
// Localizable.strings
"btn_login" = "Đăng nhập";
"error_connection" = "Lỗi kết nối";

// Sử dụng
NSLocalizedString("btn_login", comment: "")
```

---

## 🔍 Quy trình kiểm tra (Audit Checklist)

Trước khi commit hoặc review, Gemini **phải** thực hiện các bước sau:

### Bước 1: Scan tìm hardcoded strings

Tìm các pattern vi phạm trong toàn bộ mã nguồn:

```bash
# Tìm string literals trong Python
grep -rn '"[A-Za-zÀ-ỹ][A-Za-zÀ-ỹ ]{3,}"' --include="*.py" .
grep -rn "'[A-Za-zÀ-ỹ][A-Za-zÀ-ỹ ]{3,}'" --include="*.py" .

# Tìm URL hardcoded
grep -rn '"https\?://[^"]*"' --include="*.py" --include="*.ts" --include="*.js" .

# Tìm trong TypeScript/JavaScript
grep -rn '"[A-Za-z][A-Za-z ]{4,}"' --include="*.ts" --include="*.tsx" --include="*.js" .
```

### Bước 2: Phân loại vi phạm

| Loại | Ví dụ | Mức độ |
|------|-------|--------|
| UI Labels | `"Đăng nhập"`, `"Hủy bỏ"` | 🔴 Cao |
| Error messages | `"Lỗi không xác định"` | 🔴 Cao |
| API URLs | `"https://api.example.com"` | 🔴 Cao |
| Config values | `"admin"`, `"production"` | 🟡 Trung bình |
| Log messages | `"Starting server..."` | 🟢 Thấp |
| Test data | `"test@example.com"` | 🟢 Thấp (chấp nhận trong test files) |

### Bước 3: Refactor theo pattern chuẩn

```
Hardcoded string tìm thấy
         ↓
Xác định loại string
         ↓
┌────────────────────────────────────┐
│ UI text?       → i18n / locale     │
│ API URL?       → .env variable     │
│ Config value?  → constants file    │
│ Secret/key?    → .env + secret mgr │
└────────────────────────────────────┘
         ↓
Tạo/cập nhật file tập trung
         ↓
Thay thế tất cả occurrences
         ↓
Verify không còn hardcode
```

---

## 📁 Cấu trúc file tập trung (Recommended)

```
project/
├── constants/
│   ├── strings.ts        # UI strings (nếu không dùng i18n)
│   ├── config.ts         # Config values
│   └── routes.ts         # URL paths
├── locales/
│   ├── vi.json           # Tiếng Việt
│   └── en.json           # English
├── .env                  # Environment variables (không commit)
├── .env.example          # Template (commit)
└── src/
    └── (không có string nào hardcode ở đây)
```

---

## 🚫 Ngoại lệ được chấp nhận

Các trường hợp KHÔNG phải vi phạm:

```python
# ✅ Kỹ thuật / syntax - không phải UI string
content_type = "application/json"
method = "POST"
encoding = "utf-8"

# ✅ Regex patterns
pattern = r"^\d{4}-\d{2}-\d{2}$"

# ✅ Trong test files
def test_login():
    user = {"email": "test@example.com", "password": "Test@123"}

# ✅ Tên trường / field names trong model
class User(Model):
    username = CharField(max_length=150)
```

---

## ⚡ Hành động tự động của Gemini

Khi **thêm code mới**, Gemini phải:
1. ✅ Không viết string literal nào trực tiếp vào logic
2. ✅ Tạo constant/key trong file tập trung tương ứng
3. ✅ Import và sử dụng constant đó

Khi **review/edit code hiện có**, Gemini phải:
1. 🔍 Scan toàn bộ file đang chỉnh sửa
2. 🚨 Báo cáo mọi hardcoded string tìm thấy
3. 🔧 Refactor chúng TRƯỚC KHI thêm code mới
4. 📝 Giải thích từng thay đổi đã thực hiện

---

## 📊 Ví dụ Refactor hoàn chỉnh

**Trước (vi phạm):**
```typescript
async function loginUser(email: string, password: string) {
  const response = await fetch("https://api.myapp.com/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password }),
  });

  if (!response.ok) {
    throw new Error("Đăng nhập thất bại. Vui lòng thử lại.");
  }

  const user = await response.json();
  if (user.role !== "admin") {
    alert("Bạn không có quyền truy cập trang này.");
  }
}
```

**Sau (tuân thủ rule):**
```typescript
// constants/config.ts
export const API_CONFIG = {
  BASE_URL: process.env.NEXT_PUBLIC_API_URL ?? "",
  ENDPOINTS: {
    LOGIN: "/auth/login",
  },
} as const;

export const USER_ROLES = {
  ADMIN: "admin",
} as const;

// locales/vi.json
{
  "auth": {
    "login_failed": "Đăng nhập thất bại. Vui lòng thử lại.",
    "no_permission": "Bạn không có quyền truy cập trang này."
  }
}

// services/auth.ts
import { API_CONFIG, USER_ROLES } from "@/constants/config";
import { t } from "@/lib/i18n";

async function loginUser(email: string, password: string) {
  const response = await fetch(
    `${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.LOGIN}`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, password }),
    }
  );

  if (!response.ok) {
    throw new Error(t("auth.login_failed"));
  }

  const user = await response.json();
  if (user.role !== USER_ROLES.ADMIN) {
    alert(t("auth.no_permission"));
  }
}
```

---

*Rule version: 1.0 | Áp dụng cho tất cả ngôn ngữ lập trình*
