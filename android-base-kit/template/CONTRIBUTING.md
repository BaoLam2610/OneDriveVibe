# Đóng góp vào {{TEN_DU_AN}}

## Nhánh

| Nhánh | Dùng cho |
|---|---|
| `main` | Luôn build được, luôn qua CI. Không commit thẳng |
| `feat/<id>-<mo-ta-ngan>` | Tính năng, ví dụ `feat/AUTH-login-screen` |
| `fix/<id>-<mo-ta-ngan>` | Sửa lỗi |
| `refactor/<mo-ta>` | Refactor thuần, không đổi hành vi |
| `docs/<mo-ta>` | Chỉ sửa tài liệu |

Một nhánh, một việc. Refactor không trộn với sửa lỗi hay tính năng.

## Commit

Theo [Conventional Commits](https://www.conventionalcommits.org): `<type>(<scope>): <mô tả ngắn, tiếng Anh, thì hiện tại>`.

- `type`: `feat`, `fix`, `refactor`, `perf`, `test`, `docs`, `build`, `ci`, `chore`.
- `scope`: tên module ngắn (`auth`, `designsystem`, `data`, `app`).
- Ví dụ: `feat(auth): add login screen`, `fix(data): bump room version after adding column`.

Commit nhỏ, mỗi commit build được.

## Pull request

Mô tả PR gồm:

1. **Làm gì:** một đến ba câu, kèm mã quy tắc nghiệp vụ liên quan (`AUTH-03`, `CART-07`).
2. **Vì sao:** link ticket, đặc tả hoặc ADR.
3. **Kiểm thế nào:** test đã thêm, mục kiểm tay đã làm trên thiết bị (xem `docs/process/manual-test-checklist.md`).
4. **Rủi ro và chỗ cần reviewer để ý:** API mới dùng lần đầu, thay đổi schema, thay đổi giá trị đã lưu trên máy.
5. **Chuỗi mới:** liệt kê chuỗi chưa có trong thiết kế (đánh dấu `[mới]`).
6. **Ảnh chụp hoặc video** nếu đổi giao diện.

PR nên dưới khoảng 400 dòng thay đổi (không tính file sinh tự động). Lớn hơn thì chia.

## Definition of Done

Một việc chỉ được coi là xong khi:

- [ ] CI xanh: build, ktlint, detekt, Android Lint, unit test.
- [ ] ViewModel mới hoặc đổi hành vi có unit test (ADR-0009).
- [ ] Đổi schema Room: đã tăng `version`, đã ghi "Lịch sử version", schema JSON mới đã commit; nếu đã phát hành thì có Migration và test migration (ADR-0008).
- [ ] Thêm module Koin hoặc binding: `KoinVerifyTest` pass (ADR-0004).
- [ ] Không còn chuỗi cứng trong UI; chuỗi có cả bản `values` và `values-en` (ADR-0010).
- [ ] Kho dữ liệu mới đã đăng ký `DataResetter` (xóa khi đăng xuất).
- [ ] Đổi giá trị nghiệp vụ hoặc UI thì đặc tả và tài liệu thiết kế đã sửa theo, đã `grep` không còn chỗ mâu thuẫn.
- [ ] Review theo `docs/process/review-checklist.md`, có ít nhất một người duyệt.
- [ ] Đã kiểm tay trên thiết bị thật các mục áp dụng trong `docs/process/manual-test-checklist.md`.
- [ ] File tiến độ đã cập nhật (`[v]` chỉ khi đã kiểm trên máy).

## Quyết định kiến trúc

Đổi một quyết định đã có ADR thì viết ADR mới theo `docs/adr/template.md`, đánh dấu ADR cũ `superseded by ADR-NNNN`. Không sửa nội dung ADR đã `accepted`. Quy ước nhỏ (không ảnh hưởng kiến trúc) thì sửa thẳng trong `docs/conventions/`.

## Môi trường

- Android Studio bản stable mới nhất. JDK theo `gradle/gradle-daemon-jvm.properties` hoặc `.java-version`.
- Trước khi đẩy: `./gradlew ktlintCheck detekt testDebugUnitTest lintDebug` (hoặc task gộp `./gradlew check`).
- Không commit `local.properties`, keystore, file `.env` hay bất kỳ bí mật nào.
