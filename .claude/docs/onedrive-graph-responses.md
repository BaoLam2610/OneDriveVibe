# Mẫu response OneDrive API (Microsoft Graph)

- **Tài khoản**: `lamnb@lamnb.onmicrosoft.com` (OneDrive for Business, `driveType: business`)
- **Nguồn**: response thực tế ghi nhận ngày 2026-10-01
- **Tài liệu API đi kèm**: `.claude/docs/onedrive-graph-api.md`

> Tên tệp, ID, `downloadUrl` và token trong tài liệu này đã được thay bằng **giá trị mẫu**. Cấu trúc và kiểu dữ liệu giữ nguyên như response thật.

---

## 1. Lấy access token

`POST https://login.microsoftonline.com/{tenant_id}/oauth2/v2.0/token`

### 1.1 Thành công (HTTP 200)

```json
{
  "token_type": "Bearer",
  "expires_in": 3599,
  "ext_expires_in": 3599,
  "access_token": "eyJ0eXAiOiJKV1QiLCJub25jZSI6..."
}
```

| Trường | Kiểu | Ý nghĩa |
|---|---|---|
| `token_type` | string | Luôn là `Bearer`; gửi kèm header `Authorization: Bearer {access_token}` |
| `expires_in` | number (giây) | Thời gian sống của token, khoảng 1 giờ |
| `ext_expires_in` | number (giây) | Thời gian sống mở rộng, chỉ dùng khi dịch vụ xác thực gặp sự cố; không dựa vào giá trị này |
| `access_token` | string (JWT) | Token để gọi Graph |

- **Không có** `refresh_token` (đặc điểm của Client Credentials). Khi sắp hết hạn chỉ cần gọi lại request lấy token.
- Thời điểm hết hạn nên tính là `thời điểm nhận response + expires_in`, rồi trừ một khoảng an toàn (ví dụ 5 phút).
- Không ghi `access_token` vào log.

### 1.2 Thất bại

Xem cấu trúc body và bảng mã `AADSTS` ở mục **1.4** của `onedrive-graph-api.md`.

---

## 2. Liệt kê thư mục

`GET /users/{upn}/drive/root/children` (tương tự với `/items/{id}/children` và `root:/{path}:/children`)

### 2.1 Cấu trúc chung

```json
{
  "@odata.context": "https://graph.microsoft.com/v1.0/$metadata#Collection(driveItem)",
  "value": [ { ...driveItem... }, { ...driveItem... } ],
  "@odata.nextLink": "https://graph.microsoft.com/v1.0/...&$skiptoken=..."
}
```

- `value`: mảng các `driveItem` (thư mục và tệp lẫn lộn, không theo thứ tự cố định nếu không có `$orderby`).
- `@odata.nextLink`: **chỉ xuất hiện khi còn trang tiếp**. Response mẫu không có trường này vì thư mục gốc chỉ có ít item.

Một `driveItem` thuộc một trong các dạng ở mục 2.2 đến 2.7. Cách phân biệt:

| Dạng | Dấu hiệu |
|---|---|
| Thư mục thường | có `folder`, không có `package` |
| Thư mục đặc biệt | có `folder` và `specialFolder` |
| Sổ tay OneNote | có `folder` **và** `package.type = "oneNote"` |
| Tệp | có `file` |
| Video | có `file` và `video` |
| Ảnh | có `file` và `image` (và thường có `photo`) |

### 2.2 Thư mục thường

```json
{
  "createdBy": {
    "application": { "id": "b26aadf8-566f-4478-926f-589f601d9c74", "displayName": "OneDrive" },
    "user": {
      "email": "lamnb@lamnb.onmicrosoft.com",
      "id": "0912fcdc-3f3a-4b12-bab4-cb50faba7476",
      "displayName": "Bao Lam"
    }
  },
  "createdDateTime": "2024-12-20T12:40:14Z",
  "eTag": "\"{AEB61F3C-B124-4ED4-920B-1FB5BAFC2B7D},5\"",
  "id": "01P2FY5MXXXXXXXXXXXXXXXXXXXXXXXXXX",
  "lastModifiedBy": { "...": "cùng cấu trúc với createdBy" },
  "lastModifiedDateTime": "2026-09-11T16:18:48Z",
  "name": "Thu muc mau",
  "parentReference": {
    "driveType": "business",
    "driveId": "b!XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX",
    "id": "01P2FY5MV6Y2GOVW7725BZO354PWSELRRZ",
    "name": "Documents",
    "path": "/drive/root:",
    "siteId": "84eb582f-9071-44f9-9bbe-bf4c5ebd34d8"
  },
  "webUrl": "https://lamnb-my.sharepoint.com/personal/lamnb_lamnb_onmicrosoft_com/Documents/Thu%20muc%20mau",
  "fileSystemInfo": {
    "createdDateTime": "2024-12-20T12:40:14Z",
    "lastModifiedDateTime": "2026-09-11T16:18:48Z"
  },
  "folder": { "childCount": 117 },
  "isAuthoritative": false,
  "size": 202569360463
}
```

- `folder.childCount`: số item **con trực tiếp** (tệp và thư mục con).
- `size` của thư mục: **tổng dung lượng toàn bộ nội dung bên trong** (tính cả thư mục con). Giá trị có thể rất lớn (mẫu trên khoảng 202 GB).
- Thư mục **không có** `cTag` (chỉ tệp mới có).

### 2.3 Thư mục đặc biệt (`specialFolder`)

Giống thư mục thường, có thêm:

```json
"folder": { "childCount": 1120 },
"specialFolder": { "name": "attachments" }
```

Các giá trị `specialFolder.name` gặp trong drive này:

| `specialFolder.name` | Tên hiển thị | Ghi chú |
|---|---|---|
| `apps` | Apps | Thư mục dữ liệu ứng dụng |
| `attachments` | Attachments | Tệp đính kèm (Outlook/Teams) |
| `photos` | Pictures | Thư mục ảnh mặc định |
| `copilotuploads` | Microsoft Copilot Chat Files | Tệp tải lên Copilot |

### 2.4 Sổ tay OneNote (`package`)

```json
"name": "So tay mau",
"folder": { "childCount": 9 },
"package": { "type": "oneNote" },
"size": 13266511,
"webUrl": "https://lamnb-my.sharepoint.com/personal/lamnb_lamnb_onmicrosoft_com/_layouts/15/Doc.aspx?sourcedoc=%7B...%7D&file=So%20tay%20mau&action=edit&mobileredirect=true&wdorigin=Sharepoint"
```

- Có cả `folder` lẫn `package`. Nên coi đây là **một tài liệu OneNote**, không phải thư mục để duyệt vào.

### 2.5 Tệp thường (PDF, Office...)

```json
{
  "@microsoft.graph.downloadUrl": "https://lamnb-my.sharepoint.com/personal/lamnb_lamnb_onmicrosoft_com/_layouts/15/download.aspx?UniqueId={guid}&Translate=false&tempauth={token-tam-thoi}&ApiVersion=2.0",
  "@microsoft.graph.downloadUrlNoAuth": "https://lamnb-my.sharepoint.com/personal/lamnb_lamnb_onmicrosoft_com/_layouts/15/download.aspx?UniqueId={guid}&Translate=false&ApiVersion=2.0",
  "createdBy": {
    "user": {
      "email": "lamnb@lamnb.onmicrosoft.com",
      "id": "0912fcdc-3f3a-4b12-bab4-cb50faba7476",
      "displayName": "Bao Lam"
    }
  },
  "createdDateTime": "2025-07-13T14:27:23Z",
  "eTag": "\"{6979301F-22A2-4EDF-9E87-8AA6E447F5F6},2\"",
  "id": "01P2FY5MXXXXXXXXXXXXXXXXXXXXXXXXXX",
  "lastModifiedBy": { "...": "cùng cấu trúc với createdBy" },
  "lastModifiedDateTime": "2025-07-13T14:27:25Z",
  "name": "tai-lieu-mau.pdf",
  "parentReference": { "...": "như mục 2.2" },
  "webUrl": "https://lamnb-my.sharepoint.com/personal/lamnb_lamnb_onmicrosoft_com/Documents/tai-lieu-mau.pdf",
  "cTag": "\"c:{6979301F-22A2-4EDF-9E87-8AA6E447F5F6},2\"",
  "file": {
    "fileExtension": ".pdf",
    "hashes": { "quickXorHash": "f7Bntibiyl7hNih2afL8kkLwJkI=" },
    "mimeType": "application/pdf"
  },
  "fileSystemInfo": {
    "createdDateTime": "2025-07-13T14:27:23Z",
    "lastModifiedDateTime": "2025-07-13T14:27:25Z"
  },
  "isAuthoritative": false,
  "size": 4919048
}
```

Các biến thể gặp trong response thực tế:

- Tệp Office (`.docx`, `.pptx`): `webUrl` trỏ tới trình xem/sửa Office (`_layouts/15/Doc.aspx?...&action=edit`) thay vì đường dẫn tệp trực tiếp.
- Tệp được chia sẻ có thêm `"shared": { "scope": "users" }`.
- Tệp rỗng có `size: 0` và `quickXorHash: "AAAAAAAAAAAAAAAAAAAAAAAAAAA="`.
- `lastModifiedBy.user` có thể chỉ có `displayName` (ví dụ `"Guest Contributor"`), không có `id` và `email`.
- `createdBy.application` có thể không có (tệp tải lên trực tiếp).

### 2.6 Tệp video

Giống tệp thường (2.5), có thêm các facet `video`, `photo`, `media`:

```json
{
  "@microsoft.graph.downloadUrl": "https://lamnb-my.sharepoint.com/.../download.aspx?UniqueId={guid}&Translate=false&tempauth={token-tam-thoi}&ApiVersion=2.0",
  "@microsoft.graph.downloadUrlNoAuth": "https://lamnb-my.sharepoint.com/.../download.aspx?UniqueId={guid}&Translate=false&ApiVersion=2.0",
  "createdDateTime": "2026-05-23T05:14:41Z",
  "id": "01P2FY5MXXXXXXXXXXXXXXXXXXXXXXXXXX",
  "lastModifiedDateTime": "2026-05-23T05:17:54Z",
  "name": "video-mau-01.mp4",
  "cTag": "\"c:{9056099E-BE0A-440A-B617-31955E35F494},2\"",
  "file": {
    "fileExtension": ".mp4",
    "hashes": { "quickXorHash": "sWq+WDPXiy25+wj6h+86DjF8MsI=" },
    "mimeType": "video/mp4"
  },
  "fileSystemInfo": {
    "createdDateTime": "2026-05-23T05:14:41Z",
    "lastModifiedDateTime": "2026-05-23T05:17:54Z"
  },
  "media": {
    "aboutVisibility": "all",
    "analyticsVisibility": "all",
    "chatVisibility": "all",
    "interactivity": { "isInteractiveContentShown": true },
    "isNoiseSuppressionControlShown": true,
    "isRecapDisabled": false,
    "isWatermarkEnabled": false,
    "noiseSuppressionEnabledByDefault": false,
    "notesVisibility": "all",
    "tableOfContentsVisibility": "none",
    "viewpoint": {
      "areReactionsAllowed": true,
      "isAutomaticTranscriptionAllowed": false,
      "isTranscriptionAllowed": false,
      "isTranscriptionTranslationAllowed": false
    }
  },
  "photo": {
    "alternateTakenDateTime": "2026-05-23T05:14:41Z"
  },
  "size": 974001611,
  "video": {
    "audioBitsPerSample": 16,
    "audioChannels": 2,
    "audioSamplesPerSecond": 48000,
    "bitRate": 1378510,
    "duration": 5162131,
    "fourCC": "H264",
    "frameRate": 50.0,
    "height": 720,
    "width": 1280
  }
}
```

Các trường `createdBy`, `lastModifiedBy`, `parentReference`, `webUrl`, `eTag`, `isAuthoritative` giống mục 2.5, đã lược bớt.

### 2.7 Tệp ảnh (chưa có mẫu thực tế)

Response gốc của drive chưa có tệp ảnh. Theo tài liệu Microsoft, tệp ảnh có thêm:

```json
"image": { "width": 4032, "height": 3024 },
"photo": {
  "takenDateTime": "2025-01-01T08:00:00Z",
  "cameraMake": "Apple",
  "cameraModel": "iPhone 15",
  "fNumber": 1.8,
  "exposureNumerator": 1,
  "exposureDenominator": 120,
  "focalLength": 6.9,
  "iso": 50,
  "orientation": 1
}
```

> Cần đối chiếu lại khi có response thực tế của tệp ảnh. Các trường trong `photo` chỉ có khi ảnh chứa dữ liệu EXIF tương ứng.

---

## 3. Từ điển trường `driveItem`

### 3.1 Trường chung

| Trường | Kiểu | Có ở | Ý nghĩa / ghi chú |
|---|---|---|---|
| `id` | string | mọi item | ID duy nhất trong drive, dùng cho mọi API `/items/{id}` |
| `name` | string | mọi item | Tên hiển thị, có thể chứa Unicode (tiếng Việt, CJK...) |
| `size` | number (byte) | mọi item | Tệp: dung lượng tệp. Thư mục: tổng dung lượng bên trong. Cần kiểu số 64-bit (`Long`) |
| `eTag` | string | mọi item | Thay đổi khi item thay đổi bất kỳ (kể cả đổi tên, metadata) |
| `cTag` | string | chỉ tệp | Thay đổi khi **nội dung** tệp thay đổi; không đổi khi đổi tên hay di chuyển |
| `createdDateTime` | string (ISO 8601, UTC) | mọi item | Thời điểm tạo trên OneDrive |
| `lastModifiedDateTime` | string (ISO 8601, UTC) | mọi item | Thời điểm sửa gần nhất trên OneDrive |
| `fileSystemInfo.createdDateTime` | string (ISO 8601, UTC) | mọi item | Thời điểm tạo theo hệ thống tệp phía client (nếu client gửi lên); nếu không thì bằng `createdDateTime` |
| `fileSystemInfo.lastModifiedDateTime` | string (ISO 8601, UTC) | mọi item | Tương tự, cho thời điểm sửa |
| `createdBy` / `lastModifiedBy` | object | mọi item | Gồm `user` (`email`, `id`, `displayName`) và có thể có `application` (`id`, `displayName`). Các trường con có thể thiếu |
| `parentReference` | object | mọi item | Thông tin thư mục cha (xem 3.2) |
| `webUrl` | string | mọi item | Link mở trên trình duyệt (đã URL-encode); cần đăng nhập Microsoft để mở |
| `isAuthoritative` | boolean | mọi item | Trường nội bộ của dịch vụ, không cần dùng |
| `folder.childCount` | number | thư mục | Số item con trực tiếp |
| `specialFolder.name` | string | thư mục đặc biệt | Xem bảng mục 2.3 |
| `package.type` | string | gói (OneNote) | `oneNote`: sổ tay OneNote |
| `file.mimeType` | string | tệp | Ví dụ `video/mp4`, `application/pdf` |
| `file.fileExtension` | string | tệp | Đuôi tệp, có dấu chấm, ví dụ `.mp4` |
| `file.hashes.quickXorHash` | string (Base64) | tệp | Mã băm nội dung; hai tệp cùng hash là trùng nội dung |
| `shared.scope` | string | tệp/thư mục được chia sẻ | `users`, `organization`, `anonymous` |
| `@microsoft.graph.downloadUrl` | string | tệp | Link tải trực tiếp, đã kèm xác thực tạm thời (`tempauth`), sống khoảng 1 giờ |
| `@microsoft.graph.downloadUrlNoAuth` | string | tệp | Cùng link nhưng **không** kèm `tempauth`; cần xác thực riêng với SharePoint nên thường không dùng trực tiếp được (chưa kiểm chứng) |
| `video` | object | video | Xem 3.3 |
| `photo` | object | ảnh, video | Ảnh: dữ liệu EXIF (xem 2.7). Video: thường chỉ có `alternateTakenDateTime` |
| `image` | object | ảnh | `width`, `height` (pixel) |
| `media` | object | video | Cài đặt hiển thị của Microsoft Stream; không cần cho việc đọc/tải tệp |

### 3.2 `parentReference`

| Trường | Ví dụ | Ý nghĩa |
|---|---|---|
| `driveType` | `business` | Loại drive (OneDrive for Business) |
| `driveId` | `b!XXXX...` | ID của drive; giống nhau cho mọi item trong drive |
| `id` | `01P2FY5MV6Y2GOVW7725BZO354PWSELRRZ` | ID của thư mục cha (ở mẫu là ID của thư mục gốc) |
| `name` | `Documents` | Tên thư mục cha; thư mục gốc của OneDrive for Business có tên `Documents` |
| `path` | `/drive/root:` | Đường dẫn thư mục cha; thư mục con có dạng `/drive/root:/Thu muc/Con` |
| `siteId` | GUID | ID site SharePoint chứa OneDrive |

### 3.3 Facet `video`

| Trường | Đơn vị | Ví dụ | Ghi chú |
|---|---|---|---|
| `duration` | **mili giây** | `5162131` (≈ 86 phút) | Chia 1000 để ra giây |
| `bitRate` | bit/giây | `1378510` (≈ 1,38 Mbps) | |
| `width` / `height` | pixel | `1280` / `720` | Video dọc có `height > width` |
| `frameRate` | khung hình/giây | `50.0`, `28.83` | Có thể là số lẻ |
| `fourCC` | mã codec | `H264`, `AV01` | `AV01` là codec AV1; khả năng phát phụ thuộc thiết bị |
| `audioChannels` | kênh | `2` | |
| `audioSamplesPerSecond` | Hz | `48000` | |
| `audioBitsPerSample` | bit | `16` | |

---

## 4. Nhận xét rút ra từ response thực tế

1. **`children` trả sẵn `@microsoft.graph.downloadUrl`** cho mọi tệp khi không dùng `$select`. Nếu dùng `$select`, phải liệt kê tường minh `@microsoft.graph.downloadUrl` thì mới có.
2. **`downloadUrl` là thông tin nhạy cảm**: chứa `tempauth` cho phép tải tệp mà không cần token trong khoảng 1 giờ. Không ghi vào log, không chia sẻ.
3. **Video có `photo.alternateTakenDateTime`**, không có `photo.takenDateTime`. Giá trị này trong mẫu trùng với `createdDateTime`, nên không phản ánh chắc chắn ngày quay thực tế.
4. **Facet `video` có đủ thông tin kỹ thuật** (thời lượng, độ phân giải, codec), không cần tải tệp để biết.
5. **`size` có thể vượt giới hạn số nguyên 32-bit** (tệp gần 1 GB, thư mục hàng trăm GB): luôn dùng kiểu 64-bit.
6. **Thời gian đều ở UTC** (đuôi `Z`); cần đổi sang múi giờ địa phương khi hiển thị.
7. **Tên tệp có Unicode** (tiếng Việt, tiếng Trung...); `webUrl` đã được URL-encode tương ứng.
8. **Thư mục gốc của drive tên là `Documents`**, các item ở gốc có `parentReference.path = "/drive/root:"`.
9. **Trường có thể thiếu**: `createdBy.application`, `lastModifiedBy.user.id/email`, `cTag` (thư mục), `shared`, `specialFolder`, `package`, `video`, `photo`. Khi parse cần coi tất cả là tùy chọn (nullable).
