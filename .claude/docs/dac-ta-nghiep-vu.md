# Đặc tả nghiệp vụ: Ứng dụng xem OneDrive (Phim, Ảnh, Truyện PDF)

- **Nền tảng**: Android (Kotlin)
- **Người dùng**: cá nhân chủ sở hữu tenant, không có nhiều tài khoản
- **Nguồn dữ liệu**: OneDrive for Business qua Microsoft Graph, xác thực Client Credentials (xem `.claude/docs/onedrive-graph-api.md`)
- **Phạm vi tài khoản (MVP1, đã chốt)**: **chỉ hỗ trợ OneDrive for Business** (tài khoản work/school). Người dùng chỉ cần nhập **4 trường: Tenant ID, Client ID, Client Secret, UPN** là kết nối được. **Không hỗ trợ** OneDrive cá nhân (tài khoản Microsoft cá nhân như `@outlook.com`, `@hotmail.com`, `@live.com`) và không có đăng nhập Microsoft tương tác trong MVP1.
- **Ngôn ngữ (MVP1, đã chốt)**: **Tiếng Việt + English**. Văn bản trong tài liệu này viết bằng tiếng Việt và là chuỗi mặc định; bản English được dịch tương ứng khi xây dựng. Có thể thêm ngôn ngữ khác sau này mà không đổi nghiệp vụ.
- **Quyền**: chỉ đọc (`Files.Read.All`). App **không** xóa, đổi tên hay upload tệp trên OneDrive. Mọi dữ liệu do app tạo ra (lịch sử xem, cài đặt, cache) chỉ lưu trên máy.

---

## 1. Thuật ngữ

| Thuật ngữ | Nghĩa |
|---|---|
| Config | Bộ thông tin kết nối gồm đúng 4 trường: Tenant ID, Client ID, Client Secret, UPN. Không có trường nào khác |
| UPN | User Principal Name: email tài khoản OneDrive for Business cần đọc (vd. `user@contoso.com`). Bắt buộc vì xác thực app-only không có `/me`, app gọi `/users/{UPN}/drive` |
| Mã PIN | Mã gồm **đúng 6 chữ số** do người dùng tự đặt để mở app khi bật bảo mật |
| Chế độ thiết bị | Bảo mật **tắt**: config chỉ được mã hóa bằng khóa phần cứng (Keystore), không cần PIN để mở app. Đây là chế độ mặc định ngay sau khi kết nối |
| Delta sync | Đồng bộ danh sách tệp từ OneDrive về CSDL trên máy (mục 6 của tài liệu API) |
| Loại tệp được bật | Các loại tệp (Ảnh / Video / PDF) mà người dùng chọn hiển thị trong Cài đặt |
| Tệp hỗ trợ | Tệp thuộc một trong các định dạng ở mục 5.1 |
| Danh sách phát | Các video mà nút Trước/Sau và chế độ phát đi qua (xem VD-10) |
| Màn chính | Màn có **thanh điều hướng đáy** với các tab Thư mục, Thư viện, Short, Cài đặt (mục 3.4). Thêm 2026-10-10 |
| Danh sách | Cách gọi chung tab **Thư mục** và tab **Thư viện** (mục 3.4.1 đến 3.4.3) |
| Short | Tab xem video ngắn theo kiểu vuốt dọc, thứ tự ngẫu nhiên (mục 3.4.4) |

---

## 2. Luồng tổng thể

```
Mở app
  ├─ Chưa có config ──► [Kết nối] ──(thành công: lưu config, chế độ thiết bị)──► Hộp thoại "Thiết lập mã PIN?" (KN-13)
  │                                                                               ├─ Thiết lập mã PIN ─► [Thiết lập bảo mật] ─► [Màn chính]
  │                                                                               │                         └─ Back ─► quay lại hộp thoại
  │                                                                               └─ Để sau ──────────────────────────► [Màn chính]
  ├─ Có config + bảo mật BẬT ─────► [Khóa] ─────────────────────────────► [Màn chính]
  └─ Có config + bảo mật TẮT ─────────────────────────────────────────────► [Màn chính]

[Màn chính] (thanh điều hướng đáy: Thư mục / Thư viện / Short / Cài đặt)
  ├─ Thư mục, Thư viện: chọn video ─► [Xem video]  (màn riêng, mặc định vào ở hướng dọc; chọn Ngang ở Cài đặt › Video thì vào ngang)
  │                     chọn ảnh ───► [Xem ảnh]
  │                     chọn PDF ───► [Xem PDF]
  ├─ Short: xem video ngắn vuốt dọc ngay trong tab (không mở màn riêng)
  └─ Cài đặt: các màn con (đổi PIN, cập nhật secret...) mở đè lên, Ngắt kết nối ─► [Kết nối]

App ở nền quá thời gian tự khóa (khi bảo mật BẬT) ─► [Khóa]
```

---

## 3. Mô tả màn hình

### 3.1 Màn Kết nối

**Mục đích**: nhập config và xác nhận kết nối được tới OneDrive.

**Phạm vi MVP1**: chỉ dành cho tài khoản **OneDrive for Business**. Màn này **chỉ có 4 trường** bên dưới, không có nút "Đăng nhập với Microsoft", không có lựa chọn loại tài khoản. Màn **không có thanh tiến trình bước** (không còn "Bước 1/2").

**Thành phần**

| Trường | Bắt buộc | Kiểu hiển thị | Kiểm tra định dạng |
|---|---|---|---|
| Tenant ID | Có | Che ký tự (như mật khẩu) | GUID hoặc tên miền (vd. `xxx.onmicrosoft.com`) |
| Client ID | Có | Che ký tự | GUID |
| Client Secret | Có | Che ký tự | Không rỗng |
| UPN (email tài khoản OneDrive) | Có | Hiển thị bình thường | Định dạng email |

Nút **Kết nối**, biểu tượng ẩn/hiện ký tự (hình con mắt) ở từng trường bị che.

**Quy tắc nghiệp vụ**

- **KN-01**: Các trường Tenant ID, Client ID, Client Secret che ký tự khi nhập, giống ô mật khẩu.
- **KN-02**: Các trường cho phép **dán** từ clipboard (thao tác thường dùng vì giá trị copy từ Entra admin center). Không cho **copy/cắt** nội dung ra khỏi trường bị che.
- **KN-03**: Biểu tượng con mắt cho phép hiện tạm giá trị để người dùng đối chiếu; rời màn hoặc app xuống nền thì tự che lại.
- **KN-04**: Bàn phím không được học hoặc gợi ý nội dung các trường (tắt gợi ý, tắt autofill, tắt học từ cá nhân).
- **KN-05**: Tự bỏ khoảng trắng và xuống dòng ở đầu/cuối giá trị dán vào.
- **KN-06**: Nút Kết nối chỉ bật khi cả 4 trường hợp lệ về định dạng; trường sai hiện lỗi ngay dưới trường.
- **KN-07**: Bấm Kết nối: hiện trạng thái đang kết nối, khóa các trường, lần lượt:
  1. Lấy access token (Client Credentials, `scope=https://graph.microsoft.com/.default`).
  2. Gọi `GET /users/{upn}/drive?$select=id,driveType`.
- **KN-08** (tiêu chí thành công): Kết nối **thành công khi cả hai bước ở KN-07 trả HTTP 2xx** (token hợp lệ, đã có quyền, drive tồn tại). Khi thành công, app **lưu config ngay** ở chế độ thiết bị (mã hóa bằng khóa Keystore, xem CH-01) rồi hiện **hộp thoại hỏi thiết lập mã PIN** (KN-13) ngay trên màn Kết nối. App **không** hiện thẻ "Đã kết nối", **không** hiện thông tin tài khoản hay drive, **không** hiện dung lượng.
- **KN-12** (không hiển thị dung lượng): App **không** đọc, không lưu và không hiển thị `quota` ở bất kỳ màn nào (kể cả `used`, `total`, `state`). Vì vậy `used` lớn hơn `total` (`state = exceeded`) không ảnh hưởng đến kết nối hay bất kỳ thao tác nào của app.
- **KN-13** (hộp thoại thiết lập mã PIN): hiện ngay sau KN-08, gồm tiêu đề, một đoạn giải thích, **cảnh báo** như BM-03 (không đặt PIN thì bất kỳ ai cầm máy đang mở khóa đều xem được OneDrive của bạn) và **hai nút**:
  - **Thiết lập mã PIN**: mở màn Thiết lập bảo mật (mục 3.2).
  - **Để sau**: giữ bảo mật **tắt**, vào Màn chính (tab Thư mục) và bắt đầu đồng bộ lần đầu (DB-01). Người dùng bật lại ở Cài đặt › Bảo mật (CD-02).

  Hộp thoại **bắt buộc chọn một trong hai nút**: chạm ra ngoài hoặc bấm Back của hệ thống không đóng được. Hộp thoại chỉ hiện **một lần cho mỗi lần kết nối mới** (kết nối lại sau khi Ngắt kết nối thì hiện lại). Nếu app bị đóng khi hộp thoại đang hiện, config đã được lưu (KN-08), lần mở sau vào thẳng Màn chính ở chế độ thiết bị và hộp thoại **không** hiện lại.
- **KN-09**: Thất bại: hiện thông báo theo bảng dưới, giữ nguyên giá trị đã nhập để sửa; **không lưu config**. Thông báo được chọn theo **mã lỗi** và hiển thị theo ngôn ngữ đang dùng (CD-10), không hiển thị nguyên văn thông báo lỗi của máy chủ.
- **KN-10**: Màn này chặn chụp màn hình và ẩn nội dung trong danh sách ứng dụng gần đây.
- **KN-11** (phạm vi tài khoản): UPN phải là tài khoản **work/school** thuộc đúng tenant đã nhập. App không hỗ trợ tài khoản Microsoft cá nhân; nếu nhập UPN cá nhân (vd. `@outlook.com`) thì kết nối sẽ thất bại theo bảng lỗi dưới (thường là Graph `404`).

| Lỗi (xem tài liệu API mục 1.4, 9) | Thông báo |
|---|---|
| AADSTS900023 | Tenant ID không đúng định dạng |
| AADSTS90002 | Không tìm thấy tenant |
| AADSTS700016 | Client ID không tồn tại trong tenant này |
| AADSTS7000215 | Client Secret không đúng. Kiểm tra đã copy cột **Value**, không phải **Secret ID** |
| AADSTS7000222 | Client Secret đã hết hạn, hãy tạo secret mới |
| AADSTS7000112 | Ứng dụng trên Entra đang bị vô hiệu hóa |
| Graph `403` | Ứng dụng chưa được cấp quyền `Files.Read.All` hoặc chưa Grant admin consent |
| Graph `404` | Không tìm thấy OneDrive của UPN này (sai UPN, chưa có license hoặc OneDrive chưa khởi tạo). Lưu ý: app chỉ hỗ trợ tài khoản OneDrive for Business, không hỗ trợ tài khoản Microsoft cá nhân |
| Lỗi mạng / timeout | Không kết nối được, kiểm tra mạng và thử lại |
| Lỗi khác | Thông báo chung kèm mã lỗi để tra cứu |

### 3.2 Màn Thiết lập bảo mật (mở từ hộp thoại KN-13)

Màn này chỉ mở khi người dùng chọn **Thiết lập mã PIN** ở hộp thoại KN-13. Màn **không có thanh tiến trình bước**.

- **BM-01**: Màn vào thẳng bước đặt mã PIN. **Không có công tắc "Bảo vệ ứng dụng"**, vì lựa chọn dùng hay không dùng PIN đã nằm ở hộp thoại KN-13. Bật/tắt bảo vệ về sau thực hiện ở Cài đặt (CD-02, CD-03).
- **BM-02**: Đặt **mã PIN 6 chữ số**, nhập 2 lần và hai lần phải khớp; tùy chọn bật mở khóa bằng sinh trắc học nếu máy hỗ trợ.
- **BM-03** (cảnh báo khi không dùng PIN): *"Bất kỳ ai cầm máy đang mở khóa đều xem được OneDrive của bạn"*. Hiện trong hộp thoại KN-13 và khi tắt bảo vệ ở Cài đặt (CD-03).
- **BM-04**: Bấm Hoàn tất: **mã hóa lại config** (đã được lưu ở chế độ thiết bị theo KN-08) bằng khóa dẫn xuất từ PIN kết hợp Keystore (mục 4.1), ghi ra tệp tạm rồi mới thay tệp cũ như CD-09; xong vào Màn chính (tab Thư mục) và bắt đầu đồng bộ lần đầu. Lỗi giữa chừng thì config giữ nguyên ở chế độ thiết bị.
- **BM-05**: Ô nhập PIN hiển thị 6 chấm tròn; nhập đủ 6 số thì tự chuyển bước, không cần bấm nút.
- **BM-06**: Chặn PIN dễ đoán và báo *"Mã PIN quá dễ đoán, hãy chọn mã khác"*. Gồm: 6 số giống nhau (`000000`, `111111`...), dãy tăng hoặc giảm liên tiếp (`123456`, `654321`...), và dạng lặp 2 số hoặc 3 số (`121212`, `123123`...).
- **BM-07**: Nhập bằng **bàn phím số tự vẽ trong app**, không dùng bàn phím hệ thống, để bàn phím bên thứ ba không ghi nhận được PIN.
- **BM-08** (nút Back): Back ở màn này **quay lại hộp thoại KN-13** (hiện lại trên màn Kết nối). PIN đã nhập bị bỏ, config vẫn ở chế độ thiết bị. Nếu app bị đóng giữa chừng khi đang đặt PIN thì lần mở sau vào thẳng Màn chính ở chế độ thiết bị.

### 3.3 Màn Khóa (khi bảo mật bật)

- **KH-01**: Mở khóa bằng mã PIN; nhập đủ 6 số thì tự kiểm tra. Nếu đã bật sinh trắc học thì tự hiện hộp thoại sinh trắc học, có nút chuyển sang nhập PIN.
- **KH-02**: Sai PIN 5 lần liên tiếp: khóa nhập 30 giây; mỗi lần sai tiếp theo thời gian chờ tăng gấp đôi. Thời gian chờ vẫn tính khi tắt/mở lại app. Nhập đúng thì bộ đếm sai được đặt lại.
- **KH-03**: Nút "Quên mã PIN": cảnh báo không thể khôi phục, xác nhận thì **ngắt kết nối** (xóa toàn bộ dữ liệu như CD-05) và quay về màn Kết nối.
- **KH-04**: Màn chặn chụp màn hình.
- **KH-05**: Dùng bàn phím số tự vẽ như BM-07. Vị trí các phím cố định, không xáo trộn.
- **KH-06**: Nếu bật tùy chọn "Xóa dữ liệu khi nhập sai quá nhiều" (CD-08): đạt 10 lần sai liên tiếp thì thực hiện như ngắt kết nối (CD-05). Từ lần sai thứ 8 trở đi, hiện cảnh báo số lần còn lại.
- **KH-07** (mở khóa xong): quay lại đúng tab và đúng trạng thái trước khi khóa (DH-02). Khóa không xóa trạng thái các tab, chỉ xóa token và config đã giải mã (CH-03).

### 3.4 Màn chính và thanh điều hướng đáy

Màn chính dùng **thanh điều hướng đáy** thay cho 2 tab Thư mục / Thư viện trước đây (đổi 2026-10-10, ADR-0023). Cài đặt cũng là một tab trên thanh này, không còn nút bánh răng trên AppBar.

- **DH-01** (các mục): thanh có tối đa 4 mục theo thứ tự **Thư mục, Thư viện, Short, Cài đặt**.
  - Mục **Short** chỉ hiện khi **cả hai** điều kiện đúng: loại **Video** đang bật (CD-01) và **đồng bộ lần đầu đã xong** (DB-01). Thiếu một trong hai thì mục Short ẩn và thanh còn 3 mục. Quét lại toàn bộ do `410` (DB-03) **không** làm ẩn lại Short, vì danh sách cũ vẫn dùng được trong lúc quét.
  - Tắt loại Video ở Cài đặt thì mục Short biến mất ngay và trạng thái của tab Short (DH-02) bị bỏ; bật lại thì Short xáo danh sách mới (SV-02).
- **DH-02** (mỗi tab giữ trạng thái riêng): chuyển tab rồi quay lại, hoặc mở một màn xem rồi Back về, thì tab hiện **đúng trạng thái và vị trí** trước đó:

  | Tab | Trạng thái được giữ |
    |---|---|
  | Thư mục | Thư mục đang mở (cả chuỗi thư mục cha để Back), vị trí cuộn, chế độ tìm kiếm và từ khóa đang nhập |
  | Thư viện | Bộ lọc chip, vị trí cuộn |
  | Short | Thứ tự đã xáo, video đang xem, vị trí phát |
  | Cài đặt | Vị trí cuộn |

  Trạng thái này cũng được giữ khi app bị khóa rồi mở khóa (KH-07) và khi hệ điều hành thu hồi tiến trình lúc app ở nền rồi mở lại. Chỉ bị xóa khi Ngắt kết nối (CD-05) hoặc khi tắt hẳn app (vuốt bỏ khỏi danh sách gần đây).
- **DH-03** (nút Back):
  1. Trong một tab, Back lùi trong chính tab đó trước: Thư mục lên một cấp (TM-01), thoát chế độ tìm kiếm; đóng bảng hoặc hộp thoại đang mở.
  2. Ở gốc của tab Thư viện, Short hoặc Cài đặt: Back chuyển về tab **Thư mục** (giữ nguyên trạng thái của Thư mục).
  3. Ở gốc tab Thư mục: Back thoát app.
- **DH-04** (chạm lại tab đang chọn):
  - Thư mục: cuộn lên đầu; nếu đã ở đầu thì về thư mục gốc.
  - Thư viện, Cài đặt: cuộn lên đầu.
  - Short: không làm gì (xáo lại bằng kéo xuống, SV-03).
- **DH-05** (khi nào thanh ẩn): thanh chỉ hiện ở Màn chính. Thanh **ẩn** ở các màn Xem video, Xem ảnh, Xem PDF, màn Khóa, Kết nối, Thiết lập bảo mật, các màn con mở từ Cài đặt (đổi PIN, nhập PIN để tắt bảo mật, cập nhật Client Secret...) và khi bàn phím đang mở để tìm kiếm.
- **DH-06** (mở app): vào tab **Thư mục hoặc Thư viện**, tùy tab nào dùng gần nhất trong hai tab này. App **không** mở thẳng vào Short (tránh video tự phát có tiếng ngay khi mở) hay Cài đặt. Khôi phục sau khi hệ điều hành thu hồi tiến trình thì theo DH-02 (quay lại đúng tab đang mở, kể cả Short, nhưng Short ở trạng thái **tạm dừng**).
- **DH-07** (chấm nhắc trên Cài đặt): khi banner nhắc hết hạn secret đang hiện (CD-06), mục Cài đặt trên thanh có một chấm nhỏ tông cảnh báo; đã hết hạn thì chấm tông lỗi.
- **DH-08** (chặn chụp màn hình): chính sách CH-05 và CD-12 áp theo **tab đang hiện**: chuyển sang tab Cài đặt hay rời tab đó thì app áp lại chế độ chặn tương ứng ngay.

#### 3.4.1 Tab Thư mục

- **TM-01**: Duyệt theo cây thư mục OneDrive, bắt đầu từ thư mục gốc; thanh breadcrumb cho phép quay lại cấp bất kỳ; nút Back quay lên một cấp.
- **TM-02**: Thư mục luôn hiển thị trước, sau đó đến tệp.
- **TM-03**: Chỉ hiển thị tệp hỗ trợ thuộc loại tệp được bật. Tệp không hỗ trợ ẩn đi (hoặc hiện mờ, không mở được, nếu bật tùy chọn trong Cài đặt).
- **TM-04**: ~~Tùy chọn "Ẩn thư mục không có tệp phù hợp" (xét cả thư mục con) chỉ áp dụng sau khi đồng bộ lần đầu hoàn tất.~~ **Không làm** (chốt 2026-10-07: tốn công, không cần thiết); thư mục luôn hiện đủ.
- **TM-05**: Sắp xếp theo Tên / Ngày sửa / Dung lượng, tăng hoặc giảm; nhớ lựa chọn.
- **TM-06**: Chuyển đổi hiển thị dạng lưới / danh sách.
- **TM-07**: Trong khi đồng bộ lần đầu chưa xong, tab Thư mục vẫn duyệt được bằng cách gọi trực tiếp API liệt kê thư mục.

#### 3.4.2 Tab Thư viện

- **TV-01**: Hiển thị toàn bộ **ảnh và video** trong drive (mọi thư mục), dạng lưới, nhóm theo ngày, mới nhất ở trên, có tiêu đề nhóm theo ngày/tháng (định dạng theo ngôn ngữ đang dùng, CD-10).
- **TV-02**: Ngày dùng để xếp: ngày chụp (`photo.takenDateTime`) nếu có; nếu không có thì ngày tạo tệp gốc (`fileSystemInfo.createdDateTime`); cuối cùng là ngày tải lên OneDrive (`createdDateTime`).
- **TV-03**: Chip lọc nhanh: Tất cả / Ảnh / Video. Hàng chip chỉ hiện khi **cả Ảnh và Video** đang bật trong Cài đặt (chỉ còn một loại thì không cần lọc). Bộ lọc đang chọn mà bị tắt ở Cài đặt thì tự về "Tất cả".
- **TV-04**: Thanh cuộn nhanh có nhãn tháng/năm để nhảy nhanh trong thư viện lớn.
- **TV-05**: PDF **không** xuất hiện trong Thư viện; PDF xem qua tab Thư mục và mục "Đọc tiếp".
- **TV-06**: Khi đồng bộ lần đầu chưa xong: hiện trạng thái "Đang lập chỉ mục, đã quét N mục" và nội dung đã quét được đến thời điểm đó, kèm lối tắt sang tab Thư mục.

#### 3.4.3 Thành phần chung của Danh sách (tab Thư mục và Thư viện)

- **DS-01**: Mỗi tệp hiển thị thumbnail, tên, dung lượng. Video có nhãn thời lượng; tệp đang xem/đọc dở có thanh tiến độ nhỏ.
- **DS-02**: Dải "Xem tiếp / Đọc tiếp" ở đầu màn: tối đa 10 video và PDF xem dở gần nhất. Bấm vào mở đúng vị trí đã dừng. Có thể xóa từng mục khỏi dải.
- **DS-03**: Tìm kiếm theo tên tệp/thư mục trên dữ liệu đã đồng bộ (không gọi API, dùng được khi offline), chỉ trả về loại tệp được bật.
- **DS-04**: Kéo xuống để đồng bộ thay đổi mới nhất. App cũng tự đồng bộ khi mở (sau khi mở khóa) nếu lần đồng bộ trước đã quá 15 phút. Quy tắc này cũng áp dụng khi **quay lại app từ nền** (kể cả ở chế độ thiết bị, không PIN, khi tiến trình app còn sống nhiều giờ ở nền).
- **DS-05**: Không có mạng: vẫn hiển thị dữ liệu đã đồng bộ, có thanh báo "Đang offline"; tệp đã có trong cache vẫn mở được, tệp chưa có thì báo cần kết nối mạng.
- **DS-06**: Tệp đã bị xóa trên OneDrive (phát hiện qua đồng bộ) bị gỡ khỏi danh sách, lịch sử xem và cache.

#### 3.4.4 Tab Short (thêm 2026-10-10)

Xem video ngắn theo kiểu vuốt dọc (giống TikTok, YouTube Shorts) **ngay trong tab**, không mở màn Xem video riêng. Chỉ hiển thị video: **không** có nút tương tác (thích, bình luận, chia sẻ...).

**Nguồn và thứ tự**

- **SV-01** (nguồn video): **toàn bộ video trong drive** (mọi thư mục) thuộc định dạng hỗ trợ (5.1) và có thời lượng **nhỏ hơn hoặc bằng** "Thời lượng tối đa của Short" (Cài đặt › Video, 3 đến 10 phút, mặc định 3 phút). Video không có thời lượng trong dữ liệu đồng bộ (`video.duration` trống) bị loại. Danh sách lấy từ CSDL trên máy (DB-05), không gọi API để liệt kê.
- **SV-02** (thứ tự ngẫu nhiên): danh sách được **xáo ngẫu nhiên một lần** khi mở tab Short lần đầu sau khi app khởi động. Thứ tự giữ nguyên khi chuyển tab, mở màn khác, khóa/mở khóa và khi hệ điều hành thu hồi tiến trình rồi khôi phục (DH-02). Tắt hẳn app rồi mở lại thì xáo mới.
- **SV-03** (kéo để xáo lại): **chỉ khi đang ở video đầu tiên**, kéo xuống thì xáo lại **toàn bộ** danh sách và phát từ đầu một video ngẫu nhiên mới; video đầu mới phải **khác** video đang xem (khi danh sách có từ 2 video). Ở video khác video đầu, vuốt xuống là về video trước (SV-04).
- **SV-04** (chuyển video): vuốt lên sang video sau, vuốt xuống về video trước. Ở video cuối danh sách, vuốt lên không chuyển (không quay vòng).

**Phát và hiển thị**

- **SV-05** (phát): video hiện trên màn thì tự phát **có tiếng**, chạy hết thì **tự lặp lại** từ đầu. Tốc độ luôn 1x. Giữ màn hình sáng khi đang phát (như VD-09). Âm lượng chỉnh bằng phím âm lượng của máy.
- **SV-06** (chạm): chạm một lần để **tạm dừng / phát tiếp**, hiện biểu tượng ngắn ở giữa màn. Không có tua, chạm đúp, chỉnh tốc độ, zoom, khóa thao tác, vuốt chỉnh độ sáng/âm lượng hay nút xoay màn hình.
- **SV-07** (thanh tiến độ): thanh mỏng ở mép trên thanh điều hướng đáy, chỉ để xem, không kéo để tua.
- **SV-08** (khung hình): xác định theo **tỉ lệ thật của hình sau khi giải mã** (đã tính hướng xoay ghi trong tệp), không chỉ dựa vào `video.width`/`height` của OneDrive:
  - Video **dọc** có tỉ lệ gần tỉ lệ vùng hiển thị (phần bị cắt không quá khoảng 20% mỗi chiều; ngưỡng chốt lại khi thử máy): **Cắt đầy**, lấp kín vùng phía trên thanh điều hướng đáy.
  - Video dọc lệch tỉ lệ nhiều (vd. 3:4): **Vừa khung**.
  - Video **ngang** hoặc vuông: **Vừa khung**, nằm giữa theo chiều dọc như hiển thị thông thường.

  Không theo cài đặt "Khung hình mặc định" của màn Xem video (VD-06). Tab Short luôn ở hướng dọc.
- **SV-09** (tên tệp): góc dưới bên trái luôn hiện **tên tệp** và **thư mục chứa tệp** bằng chữ mờ, một dòng mỗi thứ, cắt "…" khi dài. Không phải nút bấm.
- **SV-10** (nền): tab Short dùng nền đen của màn xem, thanh điều hướng đáy chuyển sang **nền tối** khi đang ở tab này, không phụ thuộc giao diện Sáng/Tối.

**Vòng đời và dữ liệu**

- **SV-11** (tạm dừng và phát tiếp): video **tạm dừng** khi chuyển sang tab khác, app xuống nền hoặc app bị khóa. Quay lại tab Short thì phát tiếp **đúng video và đúng vị trí** đã dừng (DH-02); riêng trường hợp khôi phục sau khi hệ điều hành thu hồi tiến trình thì ở trạng thái tạm dừng (DH-06).
- **SV-12** (không tính là xem): video xem ở Short **không** lưu vị trí xem (VD-12), không vào dải "Xem tiếp" và không đổi các lựa chọn của màn Xem video (chế độ phát, tốc độ, khung hình).
- **SV-13** (tải trước): app tải trước phần đầu của video kế tiếp (và video trước) để vuốt sang là phát gần như ngay (ADR-0024). Phần tải trước dùng chung cache video (BN-01, phần video). Không tải trước khi không có mạng.
- **SV-14** (lỗi):
  - Link stream hết hạn giữa chừng: như VD-14.
  - Định dạng/codec không phát được: hiện thẻ lỗi như VD-15 nhưng **không** đếm ngược và **không** tự chuyển; người dùng vuốt sang video khác.
  - Mất mạng: như VD-16 (phát hết phần đã tải trước, tạm dừng và báo lỗi mạng, có mạng lại thì cho phát tiếp). Video đã có trong cache vẫn phát được khi offline.
- **SV-15** (danh sách rỗng): không có video nào thỏa SV-01 thì hiện trạng thái trống *"Không có video dài tối đa N phút"* kèm gợi ý tăng "Thời lượng tối đa của Short" ở Cài đặt.
- **SV-16** (dữ liệu thay đổi):
  - Đổi "Thời lượng tối đa của Short": lần quay lại tab Short kế tiếp thì lọc lại theo giá trị mới và **xáo mới**.
  - Đồng bộ thêm video mới trong lúc dùng app: **không** chèn vào danh sách đang xáo; video mới xuất hiện từ lần xáo sau (SV-02, SV-03).
  - Video bị xóa trên OneDrive (DS-06): gỡ khỏi danh sách; nếu đang hiện thì chuyển sang video kế tiếp.

### 3.5 Màn Xem video

Chọn một video trong Danh sách (Thư mục, Thư viện hoặc dải "Xem tiếp") sẽ chuyển sang **màn Xem video riêng**. Video xem ở tab Short **không** dùng màn này (mục 3.4.4).

**Điều khiển** (lấy cảm hứng từ YouTube):

- **VD-01**: Chạm một lần để hiện/ẩn thanh điều khiển; tự ẩn sau 3 giây không thao tác.
- **VD-02**: Play/Pause ở giữa; thanh tua hiển thị thời gian hiện tại / tổng và phần đã tải trước (buffer).
- **VD-03**: Chạm đúp nửa trái/phải để tua lùi/tới (mặc định 10 giây, chỉnh trong Cài đặt); chạm đúp liên tiếp thì cộng dồn.
- **VD-04**: Vuốt dọc nửa trái để chỉnh độ sáng, nửa phải để chỉnh âm lượng. Tạm tắt khi video đang được zoom lớn hơn 1x (xem VD-18).
- **VD-05**: Tốc độ phát: 0.25x, 0.5x, 0.75x, 1x, 1.25x, 1.5x, 2x.
- **VD-06**: Nút khung hình xoay vòng 3 chế độ, hiện nhãn ngắn mỗi lần đổi:
  1. **Vừa khung**: giữ tỉ lệ, có viền đen nếu khác tỉ lệ màn hình.
  2. **Cắt đầy**: giữ tỉ lệ, phóng to cho đầy màn hình, phần thừa bị cắt.
  3. **Kéo giãn**: kéo cho đầy màn hình, hình có thể bị méo.

  Nhớ chế độ dùng gần nhất cho các lần xem sau (Cài đặt có mục "Khung hình mặc định" để đặt lại). Nếu Cài đặt đang chọn một khung hình cố định thì video mở theo khung đó và việc xoay vòng trong lúc xem **không** được ghi nhớ.
- **VD-07**: Nút **xoay màn hình** trên thanh điều khiển, bấm để chuyển qua lại **Dọc ↔ Ngang**. Hướng màn hình chỉ đổi bằng nút này, **không** theo cảm biến xoay của máy. **Không có nút toàn màn hình riêng** vì đây đã là màn riêng: ở hướng ngang, thanh trạng thái và thanh điều hướng của hệ thống tự ẩn (vuốt từ cạnh để hiện tạm thời).
- **VD-08**: Nút khóa thao tác: chặn mọi chạm cho tới khi mở khóa (tránh chạm nhầm).
- **VD-09**: Giữ màn hình luôn sáng khi đang phát.
- **VD-10**: Nút Video trước / Video sau. **Danh sách phát** là các video cùng thư mục theo thứ tự sắp xếp hiện tại; nếu mở từ Thư viện thì là các video theo thứ tự và bộ lọc hiện tại của Thư viện; nếu mở từ dải "Xem tiếp" thì là các video cùng thư mục với video đó. Ở video đầu danh sách, nút Trước bị mờ; ở video cuối, nút Sau bị mờ (riêng chế độ Lặp danh sách thì quay vòng, xem VD-13).
- **VD-20**: Nút **Chế độ phát** trên thanh điều khiển, mỗi lần bấm xoay vòng 4 chế độ và hiện nhãn ngắn (chi tiết ở VD-13). Chế độ được lưu và dùng cho các lần xem sau.

**Quy tắc nghiệp vụ**

- **VD-11**: Video phát dạng stream, không cần tải hết; phần đã xem được giữ trong cache video.
- **VD-12**: Lưu vị trí xem khi đã xem trên 5% và chưa tới 95% thời lượng. Mở lại video có vị trí đã lưu: hỏi *"Xem tiếp từ mm:ss?"* (Xem tiếp / Xem từ đầu). Xem quá 95%: coi là đã xem xong và xóa khỏi dải "Xem tiếp". Ở chế độ Lặp một video, các vòng lặp sau không lưu lại vị trí; video được tính là xem xong từ lần đầu chạy tới cuối.
- **VD-13** (khi video chạy hết, theo Chế độ phát):

  | Chế độ | Khi video chạy hết |
    |---|---|
  | **Không lặp** | Dừng ở cuối, hiện nút Phát lại (VD-21) |
  | **Tự phát tiếp** (mặc định) | Đếm ngược 5 giây rồi tự chuyển sang video sau, có nút Hủy. Hết video cuối danh sách thì dừng, hiện nút Phát lại |
  | **Lặp một video** | Phát lại từ đầu ngay, không đếm ngược |
  | **Lặp danh sách** | Như Tự phát tiếp; hết video cuối thì quay về video đầu danh sách |

  Chọn thủ công Video trước/sau (VD-10) luôn chuyển video, không phụ thuộc chế độ.
- **VD-14**: Link stream hết hạn giữa chừng (khoảng 1 giờ): app tự lấy link mới và phát tiếp ở đúng vị trí, người dùng không phải thao tác.
- **VD-15**: Định dạng/codec không phát được: báo *"Thiết bị không hỗ trợ phát định dạng này"*, không để màn hình đen. Ở chế độ Tự phát tiếp hoặc Lặp danh sách, video lỗi **không nhảy ngay**: hiện thẻ lỗi kèm đếm ngược 5 giây và nút Hủy, hết đếm ngược thì bỏ qua và chuyển sang video sau (nếu cả danh sách đều lỗi thì dừng, tránh lặp vô hạn). Lỗi chỉ gắn với đúng video bị lỗi, không làm video kế tiếp bị bỏ qua oan.
- **VD-16**: Mất mạng khi đang phát: phát hết phần đã buffer, sau đó tạm dừng và báo lỗi mạng; có mạng lại thì cho phép tiếp tục. Không tự chuyển video khi đang mất mạng.
- **VD-17** (thông tin tệp): nút Thông tin (ⓘ) trên thanh điều khiển mở bảng trượt từ dưới lên (ở hướng ngang hiện dạng bảng bên phải), video **tạm dừng** trong lúc xem bảng. Dữ liệu lấy từ CSDL trên máy, không gọi API, dùng được khi offline. Gồm:

  | Mục | Nguồn dữ liệu |
    |---|---|
  | Tên tệp | `name` |
  | Dung lượng | `size` |
  | Ngày tải lên OneDrive | `createdDateTime` |
  | Ngày sửa gần nhất | `lastModifiedDateTime` |
  | Ngày tạo gốc trên thiết bị | `fileSystemInfo.createdDateTime`; chỉ hiện khi khác ngày tải lên |
  | Thời lượng | `video.duration` |
  | Độ phân giải, tốc độ khung hình | `video.width`, `height`, `frameRate` |
  | Codec, bitrate | `video.fourCC`, `bitRate` |
  | Thư mục chứa tệp | dựng từ chuỗi thư mục cha trong CSDL |

  Trường nào không có dữ liệu thì ẩn dòng đó. Không dùng `photo.alternateTakenDateTime` làm ngày quay vì giá trị này có thể chỉ trùng ngày tải lên. OneDrive không có khái niệm "ngày đăng", nên app dùng ngày tải lên.
- **VD-18** (zoom tự do): dùng hai ngón tay để zoom từ 1x đến 4x; khi đang zoom lớn hơn 1x thì một ngón kéo để di chuyển video, và hiện nút "Đặt lại zoom". Chạm đúp vẫn là tua (VD-03), không dùng để zoom. Zoom được đặt lại về 1x khi chuyển sang video khác hoặc khi đổi hướng màn hình.
- **VD-19** (hướng màn hình):
  - Mở video từ Danh sách vào ở hướng theo Cài đặt › Video › "Hướng màn hình khi mở video" (mặc định **dọc**), bất kể tỉ lệ video hay hướng của lần xem trước (không nhớ hướng).
  - Chuyển video trước/sau hoặc tự phát video kế tiếp thì **giữ hướng hiện tại**.
  - Bấm Back để thoát màn Xem video (một lần bấm, kể cả khi đang ở hướng ngang), màn hình trở về hướng dọc.
  - Đổi hướng **không làm gián đoạn phát**: không tải lại video, không mất vị trí, giữ nguyên tốc độ, khung hình (VD-06) và trạng thái phát/tạm dừng.
- **VD-21** (nút Phát lại): khi video dừng ở cuối (chế độ Không lặp, hoặc hết video cuối danh sách ở chế độ Tự phát tiếp), nút ở giữa đổi thành biểu tượng **Phát lại** thay cho Play/Pause, và thanh điều khiển không tự ẩn. Bấm thì phát lại **từ đầu** video đó, không hỏi "Xem tiếp từ mm:ss?". Kéo thanh tua về một vị trí bất kỳ cũng thoát trạng thái này và phát bình thường.

### 3.6 Màn Xem ảnh

- **AN-01**: Hiển thị ngay thumbnail kích thước lớn, rồi thay bằng ảnh gốc khi tải xong (có chỉ báo đang tải).
- **AN-02**: Zoom bằng hai ngón tay; chạm đúp để zoom tới điểm chạm, chạm đúp lần nữa để về vừa khung; khi đang zoom thì kéo để di chuyển.
- **AN-03**: Vuốt ngang để chuyển ảnh trước/sau theo đúng ngữ cảnh mở (cùng thư mục, hoặc cùng bộ lọc trong Thư viện). Khi đang zoom, vuốt ngang chỉ di chuyển ảnh.
- **AN-04**: Chạm một lần để ẩn/hiện thanh công cụ (chế độ xem toàn màn hình).
- **AN-05**: Nút Thông tin: tên tệp, kích thước ảnh, dung lượng, ngày chụp, thiết bị chụp (nếu có), đường dẫn thư mục.
- **AN-06**: Hỗ trợ GIF động. Ảnh độ phân giải rất lớn được hiển thị theo vùng để không bị tràn bộ nhớ.
- **AN-07**: Ảnh lỗi định dạng: hiện biểu tượng lỗi kèm tên tệp, vẫn vuốt sang ảnh khác được.

### 3.7 Màn Xem PDF (truyện)

- **PD-01**: Mở PDF: tải toàn bộ tệp, hiện tiến trình (% và dung lượng); cho phép hủy. Tệp đã có trong cache thì mở ngay.
- **PD-02**: Chế độ đọc mặc định: cuộn dọc liên tục; có tùy chọn lật trang ngang.
- **PD-03**: Zoom bằng hai ngón tay và chạm đúp; mặc định vừa chiều rộng màn hình.
- **PD-04**: Hiện số trang hiện tại / tổng số trang; chạm vào để nhập số trang cần nhảy tới.
- **PD-05**: Lưu trang đang đọc theo từng tệp; mở lại thì vào thẳng trang đó (có thông báo nhỏ "Đang ở trang X"). Đọc đến trang cuối thì coi là đã đọc xong và xóa khỏi dải "Đọc tiếp".
- **PD-06**: Chạm một lần để ẩn/hiện thanh công cụ; giữ màn hình luôn sáng khi đang đọc.
- **PD-07**: PDF có mật khẩu hoặc bị hỏng: báo *"Không mở được tệp PDF này"*.
- **PD-08** (thông tin tệp): nút Thông tin (ⓘ) mở bảng trượt từ dưới lên gồm: tên tệp, dung lượng, số trang (chỉ có sau khi tệp đã mở được), ngày tải lên, ngày sửa gần nhất, thư mục chứa tệp. Các trường từ OneDrive lấy từ CSDL trên máy như VD-17.

### 3.8 Tab Cài đặt

Cài đặt là **một tab trên thanh điều hướng đáy** (DH-01, đổi 2026-10-10), không còn mở từ nút bánh răng trên AppBar. Các màn con (đổi PIN, nhập PIN để tắt bảo mật, cập nhật Client Secret...) mở đè lên Màn chính và ẩn thanh điều hướng (DH-05).

| Nhóm | Mục | Giá trị / mặc định |
|---|---|---|
| Hiển thị | Loại tệp hiển thị | Ảnh ✓, Video ✓, PDF ✓ (phải bật ít nhất 1 loại) |
| | ~~Ẩn thư mục không có tệp phù hợp~~ | Không làm (TM-04, chốt 2026-10-07) |
| | ~~Hiện tệp không hỗ trợ (dạng mờ)~~ | Không làm (chốt 2026-10-07, cùng lý do TM-04: tốn công, không cần thiết) |
| | Giao diện | Theo hệ thống / Sáng / Tối |
| | Ngôn ngữ | Theo hệ thống / Tiếng Việt / English (mặc định: Theo hệ thống) |
| Bảo mật | Bảo vệ ứng dụng | Bật/Tắt. Mặc định **Tắt** cho tới khi người dùng thiết lập PIN (ở hộp thoại KN-13 hoặc tại đây) |
| | Đổi mã PIN | Khi đang bật |
| | Mở khóa bằng sinh trắc học | Khi đang bật |
| | Bảo vệ màn hình | Bật/Tắt (mặc định Tắt): bật thì **mọi màn** của app chặn chụp và quay màn hình, ẩn nội dung ở danh sách app gần đây (FLAG_SECURE toàn app). Không phụ thuộc có bật PIN hay không. Thêm 2026-10-07 |
| | Tự khóa khi rời app | Ngay lập tức / 10 giây / 30 giây / 1 / 5 / 15 phút (mặc định 1 phút; 10 giây và 30 giây thêm 2026-10-07) |
| | Xóa dữ liệu khi nhập sai quá nhiều (10 lần) | Tắt; chỉ hiện khi đang bật bảo mật |
| Video | Bước tua khi chạm đúp | 5 / 10 / 15 giây (mặc định 10) |
| | Tốc độ mặc định | 0.25x / 0.5x / 0.75x / 1x / 1.25x / 1.5x / 2x (mặc định 1x); áp dụng khi mở video, người xem vẫn đổi được trong lúc xem |
| | Khung hình mặc định | Vừa khung / Cắt đầy / Kéo giãn (mặc định: nhớ lựa chọn gần nhất) |
| | Hướng màn hình khi mở video | Dọc / Ngang (mặc định Dọc) |
| | Chế độ phát | Không lặp / Tự phát tiếp / Lặp một video / Lặp danh sách (mặc định Tự phát tiếp). Cùng giá trị với nút Chế độ phát (VD-20) |
| | Nhớ vị trí xem | Bật |
| | Thời lượng tối đa của Short | 3 đến 10 phút, bước 1 phút (mặc định 3 phút). Chỉ video dài tối đa bằng giá trị này mới vào tab Short (SV-01). Chỉ hiện khi loại Video đang bật. Thêm 2026-10-10 |
| PDF | Kiểu đọc | Cuộn dọc / Lật trang ngang |
| Bộ nhớ đệm | Dung lượng đang dùng | Hiển thị theo loại (thumbnail, ảnh, video, PDF) |
| | Giới hạn tối đa | Tùy chỉnh từ 1 đến 10 GB (mặc định 2 GB); chốt 2026-10-07, thay cho 4 mốc 1/2/5/10 |
| | Tỉ lệ chia theo loại | Thumbnail / Ảnh / Video / PDF: mỗi mốc 0 đến 100%, tổng luôn 100% (chỉnh một mốc thì phần còn lại tự tính lại). Mặc định 10% / 30% / 45% / 15%. Mỗi loại có trần riêng = giới hạn tối đa × tỉ lệ, dọn tệp lâu không dùng nhất trước. Chốt 2026-10-07 |
| | Xóa bộ nhớ đệm | Có xác nhận |
| Kết nối | Thông tin | UPN, Tenant ID và Client ID (che bớt, vd. `a1b2••••9f0e`), thời điểm đồng bộ gần nhất. Loại tài khoản: OneDrive for Business |
| | Cập nhật Client Secret | |
| | Ngày hết hạn secret | Người dùng tự nhập (không bắt buộc) |
| | Ngắt kết nối | |

**Quy tắc nghiệp vụ**

- **CD-01** (loại tệp): Thay đổi áp dụng ngay cho Thư mục, Thư viện, Short, tìm kiếm và dải "Xem tiếp / Đọc tiếp". Tắt loại Video thì ẩn mục Short trên thanh điều hướng đáy (DH-01). Tắt một loại **không** xóa lịch sử xem hay cache của loại đó; bật lại thì hiện lại như cũ. Không cho tắt cả 3 loại.
- **CD-02** (bật bảo mật): đặt mã PIN 6 số (nhập 2 lần, áp dụng BM-05 đến BM-07), hỏi bật sinh trắc học, mã hóa lại config.
- **CD-03** (tắt bảo mật): **bắt buộc nhập mã PIN hiện tại**, hiện cảnh báo như BM-03, mã hóa lại config ở chế độ thiết bị. Tùy chọn "Xóa dữ liệu khi nhập sai quá nhiều" (CD-08) cũng tự tắt.
- **CD-04** (cập nhật Client Secret): yêu cầu xác thực lại (mã PIN / sinh trắc học nếu bảo mật bật); kiểm tra kết nối với secret mới (như KN-07) thành công mới lưu, thất bại thì giữ secret cũ. Chỉ đổi Client Secret; Tenant ID, Client ID và UPN muốn đổi thì phải ngắt kết nối và kết nối lại (CD-05). Chế độ bảo mật (thiết bị hay PIN) và sinh trắc học giữ nguyên: ở chế độ PIN, config được mã hóa lại bằng khóa phiên đang giữ, không cần dẫn xuất lại từ PIN. Cũng mở được từ nút "Cập nhật" của banner CD-06 và đi qua cùng bước xác thực lại.
- **CD-05** (ngắt kết nối): xác nhận 2 bước, sau đó xóa config, khóa mã hóa, dữ liệu đồng bộ, lịch sử xem, cache và cài đặt, bỏ trạng thái của mọi tab (DH-02), rồi quay về màn Kết nối.
- **CD-06** (nhắc hết hạn secret): nếu có nhập ngày hết hạn, hiện thông báo trong app từ 14 ngày trước ngày đó. Thông báo (banner) hiện ở **cả tab Cài đặt và Danh sách (tab Thư mục, Thư viện)**, không chỉ ở Cài đặt (chốt 2026-10-07); mục Cài đặt trên thanh điều hướng đáy có chấm nhắc (DH-07). Ngày hết hạn nhập theo định dạng ngày của ngôn ngữ đang dùng (CD-10), có nút chọn nhanh +6 tháng, +1 năm, +2 năm và nút Xóa ngày; chỉ chấp nhận năm 2000 đến 2099. Còn 0 ngày thì banner ghi "hết hạn hôm nay"; đã qua ngày thì ghi "đã hết hạn" và đổi sang tông lỗi. Ngày hết hạn không phải bí mật nên lưu cùng cài đặt và bị xóa khi Ngắt kết nối (CD-05).
- **CD-07** (giảm giới hạn cache): nếu dung lượng hiện tại vượt giới hạn mới thì hỏi xác nhận (D6) rồi dọn ngay theo nguyên tắc tệp lâu không dùng bị xóa trước. Dọn theo **từng loại**: mỗi loại có trần riêng = giới hạn chung × tỉ lệ của loại đó, nên đổi tỉ lệ cũng dọn loại nào vượt trần mới (không hỏi). Riêng thumbnail không dọn chọn lọc được: vượt trần thì xóa hết thumbnail (tải lại khi cuộn tới), và trần mới của thumbnail có hiệu lực từ lần mở app sau (ADR-0021).
- **CD-08** (xóa dữ liệu khi nhập sai quá nhiều): khi bật, hiện cảnh báo *"Sau 10 lần nhập sai liên tiếp, toàn bộ dữ liệu và kết nối sẽ bị xóa"*, yêu cầu xác nhận bằng mã PIN. Tùy chọn này chỉ chống đoán PIN qua giao diện app, không thay thế được việc khóa màn hình thiết bị.
- **CD-09** (đổi mã PIN): nhập PIN hiện tại, rồi nhập PIN mới 2 lần (áp dụng BM-06); mã hóa lại config bằng PIN mới. Ghi ra tệp tạm rồi mới thay tệp cũ, để lỗi giữa chừng không làm mất config. Sau khi đổi, mở khóa bằng sinh trắc học bị **tắt** (khóa dẫn xuất mới), người dùng bật lại ở Cài đặt.
- **CD-11** (tự khóa khi rời app): chỉ có tác dụng khi bảo mật BẬT. Tính từ lúc **cả app** không còn màn nào hiển thị (xoay màn hình, hộp thoại sinh trắc học của hệ thống không tính). Quay lại trước mốc đã chọn thì không khóa; quá mốc thì khóa (CH-03). "Ngay lập tức" khóa ngay khi rời app. Hệ điều hành thu hồi app trong lúc chờ thì lần mở sau luôn vào màn Khóa. Trong thời gian chờ, ảnh app ở danh sách ứng dụng gần đây vẫn bị ẩn (CH-05).
- **CD-12** (bảo vệ màn hình): bật thì mọi màn của app chặn chụp, quay màn hình và ẩn nội dung ở danh sách ứng dụng gần đây, kể cả hộp thoại. Tắt (mặc định) thì chỉ các màn luôn chặn ở CH-05 (Kết nối, Khóa, Thiết lập bảo mật, nhập PIN) chặn; tab Cài đặt chụp được. Không phụ thuộc có bật PIN hay không; bị xóa về mặc định khi Ngắt kết nối (CD-05). Áp theo tab đang hiện (DH-08).
- **CD-13** (thời lượng tối đa của Short): thanh trượt 3 đến 10 phút, bước 1 phút, mặc định 3 phút. Đổi giá trị áp dụng theo SV-16 (lọc lại và xáo mới khi quay lại tab Short). Thuộc nhóm cài đặt nên bị xóa về mặc định khi Ngắt kết nối (CD-05). Thêm 2026-10-10.
- **CD-10** (ngôn ngữ):
  - Hỗ trợ **Tiếng Việt** và **English**. "Theo hệ thống" dùng ngôn ngữ của máy; nếu máy dùng ngôn ngữ khác VI/EN thì dùng **Tiếng Việt** (ngôn ngữ mặc định của app).
  - Đổi ngôn ngữ áp dụng **ngay**, không cần khởi động lại app và không làm mất màn hình đang mở hay trạng thái đang xem (kể cả trạng thái các tab, DH-02).
  - Áp dụng cho **mọi văn bản** trong app, gồm cả thông báo lỗi (bảng lỗi ở 3.1), cảnh báo, nhãn, các hộp thoại và nhãn trên thanh điều hướng đáy. Thông báo lỗi chọn theo mã lỗi, không hiển thị nguyên văn của máy chủ.
  - **Ngày, giờ, dung lượng, thời lượng và số** hiển thị theo quy ước của ngôn ngữ đang dùng (vd. tiêu đề nhóm ngày trong Thư viện, TV-01).
  - **Tên tệp và tên thư mục** lấy từ OneDrive, hiển thị nguyên văn, không dịch.
  - Lựa chọn ngôn ngữ thuộc nhóm cài đặt nên bị xóa khi Ngắt kết nối (CD-05); khi đó app quay về "Theo hệ thống". Màn Kết nối, hộp thoại KN-13 và màn Thiết lập bảo mật lần đầu luôn dùng "Theo hệ thống".
  - Thêm ngôn ngữ mới sau này chỉ cần bổ sung bộ chuỗi và một lựa chọn trong danh sách; không đổi quy tắc nghiệp vụ.

---

## 4. Quy tắc nghiệp vụ chung

### 4.1 Bảo mật và lưu config

- **CH-01**: Config luôn được mã hóa, ở cả hai chế độ:
  - Bảo mật **bật**: mã hóa bằng khóa dẫn xuất từ mã PIN kết hợp khóa phần cứng của thiết bị (Android Keystore).
  - Bảo mật **tắt** (chế độ thiết bị): mã hóa bằng khóa phần cứng của thiết bị. Đây là chế độ config được lưu ngay sau khi kết nối thành công (KN-08), cho tới khi người dùng thiết lập PIN (BM-04, CD-02).
- **CH-02**: Không lưu mã PIN hay mã băm của mã PIN. PIN đúng hay sai được xác định bằng việc giải mã config thành công hay không.
- **CH-03**: Access token chỉ giữ trong bộ nhớ, không ghi xuống đĩa; khi app bị khóa thì xóa token và config đã giải mã khỏi bộ nhớ.
- **CH-04**: Dữ liệu app không được đưa vào sao lưu tự động của Android.
- **CH-05**: Các màn Kết nối, Khóa, Thiết lập bảo mật và nhập PIN luôn chặn chụp màn hình. Tab Cài đặt **không** luôn chặn (đổi 2026-10-08, ADR-0022): chỉ chặn khi bật "Bảo vệ màn hình". Mục "Bảo vệ màn hình" ở Cài đặt › Bảo mật mở rộng việc chặn ra toàn app.
- **Hướng màn hình (2026-10-07)**: toàn app chỉ hiển thị **dọc** (cấu hình toàn app, không có trong Cài đặt). Riêng màn xem video cho xoay ngang bằng nút xoay (VD-07), rời màn thì về dọc. Tab Short luôn dọc (SV-08).
- **CH-06**: Không ghi Client Secret, access token hay header `Authorization` vào log.
- **CH-07**: Vì PIN chỉ có 1 triệu tổ hợp, việc chống đoán thử không dựa vào độ phức tạp của PIN mà dựa vào hai lớp: (1) khóa Keystore gắn với thiết bị nên không thể thử PIN ngoài máy; (2) dẫn xuất khóa chậm (Argon2id hoặc tương đương) để mỗi lần thử tốn thời gian.

### 4.2 Token

- **TK-00** (cơ chế xác thực, MVP1): chỉ dùng **Client Credentials (app-only)** với 4 trường config (Tenant ID, Client ID, Client Secret, UPN). Mọi lời gọi Graph đi qua `/users/{UPN}/drive`. Không có đăng nhập tương tác và không hỗ trợ OneDrive cá nhân.
- **TK-01**: App tự lấy token khi cần; làm mới chủ động khi còn dưới 5 phút là hết hạn.
- **TK-02**: Nhận `401` từ Graph: lấy token mới và thử lại đúng 1 lần.
- **TK-03**: Nhiều yêu cầu cùng lúc cần token mới thì chỉ gọi lấy token 1 lần, các yêu cầu khác dùng chung kết quả.
- **TK-04**: Lấy token thất bại do secret sai/hết hạn (AADSTS7000215, AADSTS7000222): dừng mọi yêu cầu, hiện thông báo kèm nút đi tới "Cập nhật Client Secret". Không tự thử lại.
- **TK-05**: Lỗi mạng hoặc `5xx` khi lấy token: thử lại với thời gian chờ tăng dần.
- **TK-06**: `429` / `503`: chờ đúng thời gian trong header `Retry-After` rồi thử lại.

### 4.3 Đồng bộ dữ liệu

- **DB-01**: Lần đầu: quét toàn bộ drive bằng delta, lưu danh sách tệp và thư mục vào CSDL trên máy, lưu mốc `deltaLink`. Quét xong lần đầu thì mục Short mới hiện trên thanh điều hướng đáy (DH-01).
- **DB-02**: Các lần sau: chỉ lấy thay đổi kể từ mốc trước (thêm, sửa, xóa).
- **DB-03**: Nhận `410` (mốc không còn hợp lệ): bỏ mốc cũ và quét lại toàn bộ ở nền; danh sách hiện tại vẫn dùng được trong lúc quét.
- **DB-04**: Đồng bộ lần đầu bị gián đoạn (tắt app, mất mạng): lần sau tiếp tục từ trang đang quét dở, không quét lại từ đầu. Nếu gián đoạn do **app bị khóa** (CH-03, config bị xóa khỏi bộ nhớ) thì đồng bộ tự chạy tiếp ngay khi mở khóa, người dùng không phải kéo làm mới.
- **DB-05**: Màn hình luôn đọc từ CSDL trên máy (trừ TM-07), nên mở nhanh và dùng được khi offline.

### 4.4 Bộ nhớ đệm

- **BN-01**: Cache gồm 4 phần: thumbnail, ảnh gốc, đoạn video đã xem (kể cả phần tải trước ở tab Short, SV-13), tệp PDF. Tổng dung lượng không vượt giới hạn trong Cài đặt. **Mỗi phần có trần riêng** = giới hạn chung × tỉ lệ của phần đó (Cài đặt › Bộ nhớ đệm › Tỉ lệ chia theo loại, mặc định 10% / 30% / 45% / 15%); phần nào vượt trần riêng thì xóa tệp lâu không dùng nhất **trong phần đó** trước, không lấy dung lượng của phần khác. Phần chưa dùng hết trần không nhường cho phần khác. Riêng thumbnail không dọn chọn lọc được (xem CD-07). Chốt 2026-10-07 (ADR-0021).
- **BN-02**: Tệp trong cache được nhận diện theo mã tệp và phiên bản nội dung (`cTag`). Tệp đổi nội dung trên OneDrive thì bản cũ trong cache bị bỏ; đổi tên hay di chuyển thì vẫn dùng lại cache.
- **BN-03**: Tải dở bị gián đoạn thì lần sau tải tiếp phần còn thiếu, không tải lại từ đầu; tệp chỉ được dùng sau khi tải đủ và đúng dung lượng.

---

## 5. Định dạng hỗ trợ

### 5.1 Danh sách định dạng

Nhận diện theo `file.mimeType`; nếu không có thì theo đuôi tên tệp.

| Loại | Định dạng | Ghi chú |
|---|---|---|
| Ảnh | jpg, jpeg, png, webp, gif, bmp, heic, heif | HEIC/HEIF cần Android 9 trở lên |
| Video | mp4, m4v, mkv, webm, mov, 3gp | Khả năng phát phụ thuộc codec phần cứng; AVI, WMV có thể không phát được. Máy không có bộ giải mã phần cứng cho định dạng đó (vd. HEVC 10-bit trên một số máy MediaTek) thì app dùng bộ giải mã phần mềm dự phòng (ADR-0018): phát được nhưng tốn pin và có thể rớt khung. Máy giải mã được bằng phần cứng thì không dùng bộ phần mềm. Dolby Vision profile 5 (không có lớp nền HEVC) có thể không phát được |
| PDF | pdf | Không hỗ trợ PDF có mật khẩu |

---

## 6. Phạm vi phiên bản

| Tính năng | Bản 1 | Bản 2 |
|---|---|---|
| Kết nối **OneDrive for Business** bằng Tenant ID / Client ID / Client Secret / UPN (Client Credentials) | ✓ | |
| Đa ngôn ngữ **Tiếng Việt + English**, chọn trong Cài đặt (CD-10) | ✓ | |
| Hộp thoại hỏi thiết lập PIN sau kết nối (KN-13), thiết lập bảo mật (mã PIN 6 số), màn Khóa | ✓ | |
| Thanh điều hướng đáy: Thư mục, Thư viện, Short, Cài đặt; mỗi tab giữ trạng thái riêng (mục 3.4) | ✓ | |
| Danh sách: tab Thư mục, tab Thư viện, tìm kiếm, sắp xếp | ✓ | |
| Tab Short: video ngắn vuốt dọc, thứ tự ngẫu nhiên, lọc theo thời lượng tối đa (mục 3.4.4) | ✓ | |
| Dải Xem tiếp / Đọc tiếp | ✓ | |
| Xem video: điều khiển ở mục 3.5, nhớ vị trí | ✓ | |
| Video: chế độ phát (Không lặp / Tự phát tiếp / Lặp một video / Lặp danh sách), nút Phát lại | ✓ | |
| Video: phát ngẫu nhiên (shuffle) trong màn Xem video (khác với thứ tự ngẫu nhiên của tab Short) | | ✓ |
| Video: 3 chế độ khung hình, zoom hai ngón, bảng thông tin tệp | ✓ | |
| Video: nút xoay dọc/ngang, mặc định vào ở hướng dọc (đổi được ở Cài đặt) | ✓ | |
| Video: tỉ lệ khung hình tùy chọn (16:9, 4:3, 21:9...) | | ✓ |
| Xem ảnh: zoom, vuốt chuyển ảnh, thông tin ảnh | ✓ | |
| Xem PDF: cuộn dọc, lật ngang, zoom, nhớ trang, bảng thông tin tệp | ✓ | |
| Cài đặt: các mục ở mục 3.8 | ✓ | |
| Picture-in-Picture; phụ đề `.srt` cùng tên; chọn track âm thanh | | ✓ |
| Trình chiếu ảnh | | ✓ |
| Truyện CBZ; đọc từ phải sang trái (manga); chế độ đọc ban đêm; dải thumbnail trang PDF | | ✓ |
| Ghim tệp để xem offline | | ✓ |
| Chỉ tải ảnh gốc / video khi có Wi-Fi | | ✓ |

**Ngoài phạm vi MVP1:** OneDrive cá nhân (tài khoản Microsoft cá nhân) và đăng nhập Microsoft tương tác (Authorization Code + PKCE / Device Code); ngôn ngữ ngoài Tiếng Việt và English (kiến trúc cho phép thêm sau). Chưa lên kế hoạch cho bản nào.

---

## 7. Điểm còn mở

- Có cần "Kệ truyện" (danh sách mọi PDF trong drive, giống tab Thư viện cho ảnh/video) không, hay chỉ duyệt PDF qua Thư mục?
- Có cho phép mở tệp không hỗ trợ bằng ứng dụng khác trên máy không (cần tải tệp về trước).
- Ngưỡng "tỉ lệ gần" để video dọc ở tab Short dùng Cắt đầy (SV-08, đang đề xuất phần bị cắt không quá khoảng 20% mỗi chiều): chốt khi thử máy ở Lát 8.