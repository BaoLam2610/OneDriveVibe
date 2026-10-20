# Android Base Kit

Bộ khung tài liệu và prompt để dựng codebase Android **thuần Jetpack Compose** (không KMP), Clean Architecture + MVI, đa module. Rút từ kinh nghiệm làm MVP1 của OneDriveVibe (10/2026): những gì chạy tốt thì giữ thành quy ước, những gì phải sửa lại thì biến thành luật ngay từ đầu.

## Có gì trong đây

Kit có hai phần: **đồ nghề** (ở lại trong kit, dùng một lần) và **`template/`** (phần duy nhất được chép vào repo dự án).

```
android-base-kit/
├── README.md                       ĐỒ NGHỀ  file này
├── bai-hoc-tu-onedrivevibe.md      ĐỒ NGHỀ  kinh nghiệm rút ra; đọc để hiểu vì sao có các luật
├── prompts/                        ĐỒ NGHỀ  prompt dùng một lần
│   ├── 00-bootstrap-codebase.md             dựng codebase từ con số 0, theo từng phase
│   └── 01-port-debug-tools.md               dựng công cụ debug trong app
└── template/                       CHÉP NỘI DUNG BÊN TRONG vào gốc repo dự án
    ├── CLAUDE.md                            luật cho AI agent, có chỗ điền
    ├── CONTRIBUTING.md                      nhánh, commit, PR, Definition of Done
    └── docs/
        ├── README.md                        mục lục, thứ tự đọc cho thành viên mới
        ├── architecture.md                  tầng, module, luật phụ thuộc, luồng dữ liệu
        ├── tech-stack.md                    thư viện, lý do chọn, thứ không dùng
        ├── debug-tools.md                   thiết kế module :tools:debug
        ├── conventions/                     13 file quy ước code
        ├── process/                         quy trình, checklist review và kiểm tay
        ├── prompts/                         prompt dùng lâu dài: add-feature, review, write-adr
        ├── templates/                       mẫu trống: đặc tả, thiết kế UI, kế hoạch, tiến độ
        └── adr/                             mục lục, mẫu ADR, 11 ADR khởi đầu
```

Bài học đã được chuyển thành luật trong `template/` (mỗi ADR có mục "Bối cảnh" giải thích lý do bằng "dự án trước", không nêu tên), nên repo dự án **không cần** file bài học. Muốn team đọc thì gửi riêng, không commit vào repo công ty.

**Giữ kit ở đâu:** trên nhánh `docs/android-compose-base-kit` của repo này, hoặc chép thư mục `android-base-kit/` ra một repo cá nhân/nội bộ riêng để dùng cho nhiều dự án.

## Cách dùng cho dự án mới

1. **Đọc** [bai-hoc-tu-onedrivevibe.md](bai-hoc-tu-onedrivevibe.md) (khoảng 20 phút), nếu chưa đọc.
2. **Tạo repo dự án** và chép **nội dung bên trong** `template/` vào gốc repo đó. Kết quả ở repo dự án chỉ có `CLAUDE.md`, `CONTRIBUTING.md`, `docs/`; không có thư mục `template/` hay `android-base-kit/`.

   PowerShell (Windows):

   ```powershell
   $kit  = "D:\Project\OneDriveVibe\OneDriveVibe\android-base-kit"
   $repo = "D:\Project\ShopMate"            # repo dự án mới
   New-Item -ItemType Directory -Force $repo | Out-Null
   Copy-Item -Path "$kit\template\*" -Destination $repo -Recurse
   ```

   Bash:

   ```bash
   KIT=~/kits/android-base-kit; REPO=~/work/shopmate
   mkdir -p "$REPO" && cp -R "$KIT/template/." "$REPO/"
   ```

3. **Điền chỗ trống** trong repo dự án (không sửa bản trong kit):

   | Chỗ trống | Ý nghĩa | Ví dụ |
   |---|---|---|
   | `{{TEN_DU_AN}}` | Tên app, tên tài liệu, `rootProject.name` | `ShopMate` |
   | `{{PACKAGE}}` | Package gốc | `com.company.shopmate` |
   | `Pfx` | Tiền tố code UI, 2 đến 4 chữ cái | `Sm` → `SmButton`, `SmTheme` |
   | `{{MIN_SDK}}` | minSdk | `26` |
   | `{{NGAY}}` | Ngày khởi tạo tài liệu | `2026-11-01` |
   | `{{CHON_MOT: "A" \| "B"}}` | Chọn một trong hai câu, xóa câu kia (AI có được chạy Gradle không) | `CLAUDE.md`, prompt 00 |

   Các chỗ `<...>` trong mẫu (`docs/templates/`) và trong prompt là chỗ điền khi dùng, không thay lúc khởi tạo.

   PowerShell (chạy sau bước 2, sửa giá trị cho đúng dự án):

   ```powershell
   $map = [ordered]@{
       '{{TEN_DU_AN}}' = 'ShopMate'
       '{{PACKAGE}}'   = 'com.company.shopmate'
       '{{MIN_SDK}}'   = '26'
       '{{NGAY}}'      = '2026-11-01'
   }
   $utf8 = New-Object System.Text.UTF8Encoding $false
   Get-ChildItem $repo -Recurse -Filter *.md | ForEach-Object {
       $t = [IO.File]::ReadAllText($_.FullName)
       foreach ($k in $map.Keys) { $t = $t.Replace($k, $map[$k]) }
       $t = [regex]::Replace($t, '\bPfx', 'Sm')   # PfxButton -> SmButton (phân biệt hoa thường)
       $t = [regex]::Replace($t, '\bpfx', 'sm')   # pfx.android.feature, pfx_debug, pfxLocale
       [IO.File]::WriteAllText($_.FullName, $t, $utf8)
   }
   ```

   Còn lại làm tay: chọn một câu ở mỗi chỗ `{{CHON_MOT: ...}}` trong `CLAUDE.md`; sửa danh sách ngôn ngữ (VI + EN) nếu dự án khác.

   Kiểm xong khi lệnh sau không ra gì (trừ các chỗ `<...>` trong `docs/templates/` và `docs/prompts/`, là chỗ điền khi dùng):

   ```powershell
   Get-ChildItem $repo -Recurse -Filter *.md | Select-String -Pattern '\{\{|OneDriveVibe|Pfx'
   ```
4. **Commit tài liệu** ở repo dự án trước khi có code: `docs: initialize project documentation`.
5. **Dựng codebase:** mở Claude Code ở gốc **repo dự án**, mở file [prompts/00-bootstrap-codebase.md](prompts/00-bootstrap-codebase.md) **trong kit**, chép khối "Prompt" (sửa các dòng `<...>`) và dán vào Claude Code. Prompt chạy từng phase, dừng sau mỗi phase để build, đọc diff và commit. Tới phase 6 (công cụ debug), dán thêm khối prompt trong [prompts/01-port-debug-tools.md](prompts/01-port-debug-tools.md) nếu AI cần chi tiết hơn.
6. **Làm tính năng:** viết đặc tả và thiết kế bằng mẫu trong `docs/templates/` (chép sang `docs/spec/`, `docs/design/`), rồi dùng `docs/prompts/add-feature.md`, `docs/prompts/review.md`, `docs/prompts/write-adr.md`. Ba prompt này đã nằm trong repo dự án.

## Mặc định đã chốt (đổi bằng ADR mới)

| Mục | Chọn | ADR khởi đầu |
|---|---|---|
| Kiến trúc | Clean Architecture + MVI (State, Intent, Effect) | 0001 |
| Module | `:app`, `:core:*`, `:feature:*`, `:tools:debug`, `build-logic` | 0002 |
| Điều hướng | Navigation 3, back stack do app sở hữu | 0003 |
| DI | Koin 4, có kiểm `verify()` trong unit test | 0004 |
| Mạng | Ktor Client (OkHttp engine) + kotlinx.serialization | 0005 |
| UseCase | Bắt buộc giữa feature và data | 0006 |
| Lỗi | `AppResult` / `AppError` có cấu trúc, UI đổi sang chuỗi | 0007 |
| CSDL | Room, tăng version mỗi lần đổi schema, export schema | 0008 |
| Test | Unit test cho ViewModel (và UseCase có logic), có module `:core:testing` | 0009 |
| Ngôn ngữ | Chuỗi trong `res/values*`, domain không chứa chuỗi hiển thị | 0010 |
| Debug | `:tools:debug` chỉ có trong bản debug | 0011 |

## Không phải là gì

- Không phải template code chạy sẵn. Code được AI dựng theo prompt, nên luôn khớp phiên bản thư viện mới nhất lúc khởi tạo.
- Không chứa gì riêng của OneDriveVibe (Microsoft Graph, PIN, video Short...). Những chuyện đó chỉ xuất hiện ở file bài học, làm ví dụ.
- Không pin phiên bản thư viện. `tech-stack.md` có cột "bản đã dùng ở OneDriveVibe" chỉ để tham khảo; prompt bootstrap yêu cầu tra bản stable lúc khởi tạo.
