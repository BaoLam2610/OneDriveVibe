# Tham chiếu OneDrive API (Microsoft Graph)

- **Tài khoản OneDrive**: `lamnb@lamnb.onmicrosoft.com`
- **Tenant**: Paraline (`lamnb.onmicrosoft.com`)
- **API**: Microsoft Graph v1.0, base URL `https://graph.microsoft.com/v1.0`
- **Kiểu xác thực**: Application permission + Client Credentials (không cần người dùng đăng nhập)

> Quy ước: `{upn}` = `lamnb@lamnb.onmicrosoft.com`. Gốc drive của tài khoản: `/users/{upn}/drive`.

---

## 1. Xác thực

### 1.1 Thông tin cần có

| Giá trị | Lấy ở đâu (Entra admin center) |
|---|---|
| `tenant_id` | Overview của tenant, hoặc Overview của App registration (*Directory (tenant) ID*) |
| `client_id` | App registrations → app → Overview (*Application (client) ID*) |
| `client_secret` | App registrations → app → Certificates & secrets → New client secret (copy cột **Value**) |

### 1.2 Quyền (Application permissions), cần Grant admin consent

| Nhu cầu | Quyền |
|---|---|
| Chỉ đọc | `Files.Read.All` |
| Đọc và ghi | `Files.ReadWrite.All` |
| Giới hạn một site/thư mục cụ thể | `Sites.Selected` hoặc `Files.SelectedOperations.Selected` |

### 1.3 Lấy access token

```
POST https://login.microsoftonline.com/{tenant_id}/oauth2/v2.0/token
Content-Type: application/x-www-form-urlencoded

client_id={client_id}
&client_secret={client_secret}
&scope=https://graph.microsoft.com/.default
&grant_type=client_credentials
```

- Token sống khoảng 60 phút; nên cache và làm mới khi sắp hết hạn.
- Client Credentials **không** trả về `refresh_token`; khi token sắp hết hạn chỉ cần gọi lại request trên.
- Mọi API bên dưới gửi kèm header: `Authorization: Bearer {access_token}`.

### 1.4 Lỗi khi lấy token (đến từ Entra ID, không phải OneDrive)

Endpoint token trả HTTP `400`/`401` với body dạng:

```json
{
  "error": "invalid_client",
  "error_description": "AADSTS7000215: Invalid client secret provided. ...",
  "error_codes": [7000215],
  "trace_id": "...",
  "correlation_id": "..."
}
```

Đọc mã từ `error_codes` (hoặc tiền tố `AADSTSxxxx` trong `error_description`), không dựa vào câu chữ của message.

| Mã | `error` | Ý nghĩa | Cách xử lý |
|---|---|---|---|
| AADSTS900023 | `invalid_request` | `tenant_id` không phải GUID hay tên miền hợp lệ | Kiểm tra định dạng tenant_id |
| AADSTS90002 | `invalid_request` | Tenant không tồn tại | Kiểm tra lại tenant_id |
| AADSTS700016 | `unauthorized_client` | `client_id` không tồn tại trong tenant này | Kiểm tra client_id và tenant_id có cùng một tenant |
| AADSTS7000215 | `invalid_client` | Secret sai | Hay gặp do copy nhầm cột **Secret ID** thay vì **Value** |
| AADSTS7000222 | `invalid_client` | Secret đã hết hạn | Tạo secret mới trong Certificates & secrets |
| AADSTS7000112 | `unauthorized_client` | App đã bị vô hiệu hóa | Bật lại app (Enterprise applications → Properties) |
| AADSTS1002012 | `invalid_scope` | Scope không hợp lệ | Client Credentials phải dùng scope kết thúc bằng `/.default` |

- Lỗi `invalid_client` / secret hết hạn: **không** retry tự động, cần cập nhật cấu hình.
- Lỗi mạng hoặc `5xx` từ endpoint token: retry với backoff.

---

## 2. Thông tin drive

| Mục đích | Request |
|---|---|
| Thông tin drive và dung lượng | `GET /users/{upn}/drive` |
| Chỉ lấy quota | `GET /users/{upn}/drive?select=quota` |

Response `quota` gồm `total`, `used`, `remaining`, `deleted`, `state` (đơn vị byte). Mọi trường đều có thể vắng mặt, kiểu số cần `Long` (64-bit).

| `state` | Ý nghĩa |
|---|---|
| `normal` | Bình thường |
| `nearing` | Gần hết hạn mức |
| `critical` | Rất gần hết hạn mức |
| `exceeded` | `used` đã vượt `total` |

> Request `GET /users/{upn}/drive?$select=id,driveType,quota` phù hợp để **kiểm tra kết nối**: thành công nghĩa là token hợp lệ, đã có quyền và drive tồn tại.

**Lưu ý về `quota`**

- **`used` có thể lớn hơn `total`.** Đây là hành vi hợp lệ của Graph, không phải lỗi của app hay của response: khi đó `state = "exceeded"` và `remaining = 0`. Ví dụ thực tế (đã bỏ `id`):

  ```json
  "quota": { "deleted": 411290, "remaining": 0, "state": "exceeded", "total": 10737418240, "used": 274836750191 }
  ```

  Ở ví dụ trên `total` đúng 10 GiB nhưng `used` ≈ 274,8 GB. Cũng đã có người gặp `used` ≈ 3,4 TB so với `total` ≈ 3 TB ở một dự án OneDrive client khác.
- **`total` không nhất thiết bằng hạn mức hiển thị ở admin center hay hạn mức theo license.** Hạn mức của OneDrive for Business gắn với site cá nhân (`-my.sharepoint.com`) của user và có thể bị đặt thấp hơn hoặc chênh với con số bạn thấy ở nơi khác (mặc định thường 1 TB, admin có thể nâng lên, tối đa 5 TB). Để đối chiếu, kiểm tra Storage limit của site trong SharePoint admin center hoặc `Get-SPOSite ... | Select StorageQuota, StorageUsageCurrent`.
- **Không dùng quota để quyết định logic.** App chỉ đọc nên `exceeded` không chặn duyệt, phát hay tải tệp; chỉ dùng `quota` để hiển thị theo KN-12 của đặc tả nghiệp vụ, và phải chịu được trường thiếu, `total = 0` hay `used > total`.
- Mã `507 quotaLimitReached` (mục 9.2) chỉ phát sinh khi **ghi**, không xuất hiện ở luồng đọc của app.

---

## 3. Đọc dữ liệu

### 3.1 Liệt kê nội dung thư mục

```
GET /users/{upn}/drive/root/children
GET /users/{upn}/drive/root:/{duong/dan/thu/muc}:/children
GET /users/{upn}/drive/items/{folder-id}/children
```

Tham số truy vấn (OData) hữu ích:

| Tham số | Ví dụ | Tác dụng |
|---|---|---|
| `$select` | `$select=id,name,size,file,folder,lastModifiedDateTime` | Chỉ lấy trường cần thiết |
| `$top` | `$top=200` | Số item mỗi trang |
| `$orderby` | `$orderby=name` | Sắp xếp |
| `$expand` | `$expand=thumbnails` | Lấy kèm thumbnail |

- Nếu còn trang tiếp, response có `@odata.nextLink`: gọi tiếp URL đó cho đến khi hết.
- Item có thuộc tính `folder` là thư mục; item có `file` là tệp (`file.mimeType` cho biết loại).

### 3.2 Lấy metadata một item

```
GET /users/{upn}/drive/items/{item-id}
GET /users/{upn}/drive/root:/{duong/dan/file}
```

Các trường thường dùng: `id`, `name`, `size`, `webUrl`, `file`, `folder`, `parentReference`, `createdDateTime`, `lastModifiedDateTime`, `eTag`, `cTag`. Với file media có thể có thêm `video`, `audio`, `image`, `photo`.

### 3.3 Tải nội dung file

**Cách A: `@microsoft.graph.downloadUrl`**

```
GET /users/{upn}/drive/items/{item-id}?select=id,name,@microsoft.graph.downloadUrl
```

- Trả về URL tải đã xác thực sẵn, **sống ngắn (khoảng 1 giờ)**.
- Khi gọi URL này **không** gửi header `Authorization`.
- Hỗ trợ **HTTP Range** (`Range: bytes=0-1023`) để tải từng phần hoặc tiếp tục tải dở.

**Cách B: endpoint `/content`**

```
GET /users/{upn}/drive/items/{item-id}/content
GET /users/{upn}/drive/root:/{duong/dan/file}:/content
```

- Trả `302 Redirect` tới downloadUrl; client HTTP cần tự follow redirect.

**Tải dưới dạng định dạng khác (convert)**

```
GET /users/{upn}/drive/items/{item-id}/content?format=pdf
```

- Áp dụng cho file Office (docx, xlsx, pptx...).

### 3.4 Thumbnail

```
GET /users/{upn}/drive/items/{item-id}/thumbnails
GET /users/{upn}/drive/items/{item-id}/thumbnails/0/large/content
GET /users/{upn}/drive/items/{item-id}/thumbnails/0/c400x600/content
```

- Kích thước có sẵn: `small`, `medium`, `large`; hoặc tùy chọn dạng `c{rộng}x{cao}`.

### 3.5 Tìm kiếm

```
GET /users/{upn}/drive/root/search(q='tu-khoa')
```

- Tìm theo tên và nội dung; chỉ mục có độ trễ nên file mới upload có thể chưa tìm thấy ngay.

### 3.6 Xem phiên bản cũ của file

```
GET /users/{upn}/drive/items/{item-id}/versions
```

### 3.7 Lọc chỉ lấy file ảnh hoặc video

OneDrive API **không có endpoint nào chỉ trả về ảnh hoặc video**. Có 3 cách lọc:

**Cách 1: Lọc phía client theo thuộc tính (khuyến nghị, chắc chắn nhất)**

| Loại | Cách nhận biết |
|---|---|
| Ảnh | có facet `image` (hoặc `photo`), hoặc `file.mimeType` bắt đầu bằng `image/` |
| Video | có facet `video`, hoặc `file.mimeType` bắt đầu bằng `video/` |

```
GET /users/{upn}/drive/root/children?$select=id,name,size,file,image,photo,video
```

```python
def is_media(item):
    mt = item.get("file", {}).get("mimeType", "")
    return "image" in item or "video" in item or mt.startswith(("image/", "video/"))

media = [i for i in items if is_media(i)]
```

Với drive lớn (hàng TB), nên quét toàn bộ bằng **delta** (mục 6) một lần, lọc ảnh/video rồi lưu vào CSDL riêng; các lần sau chỉ xử lý phần thay đổi, không cần duyệt thư mục đệ quy.

**Cách 2: Drive search theo đuôi file**

```
GET /users/{upn}/drive/root/search(q='.mp4')
GET /users/{upn}/drive/root/search(q='.jpg')
```

- Phải gọi riêng từng đuôi (`.mp4`, `.mkv`, `.mov`, `.jpg`, `.png`...) và vẫn nên lọc lại phía client vì kết quả có thể khớp theo tên hoặc nội dung.
- Chỉ mục search có độ trễ, file mới upload có thể chưa xuất hiện ngay.

**Cách 3: Microsoft Search API (lọc bằng KQL)**

```
POST https://graph.microsoft.com/v1.0/search/query
{
  "requests": [{
    "entityTypes": ["driveItem"],
    "query": { "queryString": "filetype:mp4 OR filetype:mkv OR filetype:jpg OR filetype:png" },
    "from": 0,
    "size": 100
  }]
}
```

- Lọc được nhiều đuôi trong một lần gọi.
- Với quyền kiểu **Application** (app-only), Search API có thêm yêu cầu (ví dụ phải truyền `region`) và hỗ trợ có thể hạn chế; cần kiểm tra lại trong tài liệu *Use the Microsoft Search API* trước khi dựa vào cách này.

**Lưu ý**

- `$filter` trên `children` của OneDrive for Business hỗ trợ rất hạn chế, nên không thể viết kiểu `$filter=file/mimeType eq 'video/mp4'` một cách đáng tin cậy.
- Các thư mục đặc biệt như `/drive/special/photos` hay `/drive/special/cameraroll` chủ yếu dùng được với OneDrive cá nhân, không phải tài khoản `onmicrosoft.com`.
- Facet `video` và `image` chỉ có khi OneDrive đã phân tích xong file, nên file vừa upload có thể thiếu; khi đó dựa vào `mimeType` hoặc đuôi tên file.

---

## 4. Ghi dữ liệu (cần `Files.ReadWrite.All`)

### 4.1 Upload

| Trường hợp | Request |
|---|---|
| File nhỏ (dưới 250 MB) | `PUT /users/{upn}/drive/root:/{duong/dan/ten.ext}:/content` (body là nội dung file) |
| File lớn | Tạo upload session rồi gửi từng chunk (xem 4.2) |

Xử lý trùng tên: thêm query `?@microsoft.graph.conflictBehavior=rename|replace|fail`.

### 4.2 Upload file lớn (upload session)

**Bước 1: tạo session**

```
POST /users/{upn}/drive/root:/{duong/dan/ten.ext}:/createUploadSession
Content-Type: application/json

{ "item": { "@microsoft.graph.conflictBehavior": "rename" } }
```

Response có `uploadUrl`.

**Bước 2: gửi từng chunk**

```
PUT {uploadUrl}
Content-Length: {kich-thuoc-chunk}
Content-Range: bytes 0-{end}/{tong-kich-thuoc}
```

- Kích thước mỗi chunk là **bội số của 320 KiB** (327,680 byte), tối đa khoảng 60 MiB.
- Gửi tuần tự theo thứ tự; chunk cuối trả về metadata của item đã tạo.
- Gọi `GET {uploadUrl}` để biết các khoảng byte còn thiếu khi cần tiếp tục.
- Hủy session: `DELETE {uploadUrl}`.
- `uploadUrl` **không** cần header Authorization.

### 4.3 Quản lý thư mục và file

| Mục đích | Request |
|---|---|
| Tạo thư mục | `POST /users/{upn}/drive/root/children` với `{ "name": "TenThuMuc", "folder": {}, "@microsoft.graph.conflictBehavior": "rename" }` |
| Tạo thư mục con | `POST /users/{upn}/drive/items/{parent-id}/children` (body như trên) |
| Đổi tên | `PATCH /users/{upn}/drive/items/{item-id}` với `{ "name": "ten-moi.ext" }` |
| Di chuyển | `PATCH /users/{upn}/drive/items/{item-id}` với `{ "parentReference": { "id": "{folder-id-dich}" } }` |
| Sao chép | `POST /users/{upn}/drive/items/{item-id}/copy` với `{ "parentReference": { "id": "{folder-id-dich}" }, "name": "ban-sao.ext" }` (bất đồng bộ, trả `202` kèm URL theo dõi) |
| Xóa (vào thùng rác) | `DELETE /users/{upn}/drive/items/{item-id}` |

---

## 5. Chia sẻ

| Mục đích | Request |
|---|---|
| Tạo link chia sẻ | `POST /users/{upn}/drive/items/{item-id}/createLink` với `{ "type": "view", "scope": "anonymous" }` |
| Mời người dùng cụ thể | `POST /users/{upn}/drive/items/{item-id}/invite` |
| Xem danh sách quyền | `GET /users/{upn}/drive/items/{item-id}/permissions` |
| Thu hồi quyền/link | `DELETE /users/{upn}/drive/items/{item-id}/permissions/{perm-id}` |

- `type`: `view`, `edit`, `embed`. `scope`: `anonymous`, `organization`, `users`.
- `scope: anonymous` có thể bị admin tenant tắt trong cài đặt chia sẻ của SharePoint/OneDrive.

---

## 6. Đồng bộ thay đổi (Delta)

```
GET /users/{upn}/drive/root/delta
GET {@odata.nextLink}     # nếu còn trang
GET {@odata.deltaLink}    # lần sau, chỉ lấy thay đổi từ mốc trước
```

- Lần đầu trả toàn bộ cây thư mục; các lần sau chỉ trả item được thêm, sửa, xóa (item xóa có thuộc tính `deleted`).
- Lưu lại `deltaLink` cuối cùng để dùng cho lần đồng bộ kế tiếp.
- Hiệu quả hơn nhiều so với quét lại toàn bộ khi drive lớn.
- Nếu `deltaLink` không còn hợp lệ, API trả `410 Gone` (xem mục 9); khi đó bỏ `deltaLink` cũ và quét lại từ đầu.

---

## 7. Batch (gộp tối đa 20 request)

```
POST https://graph.microsoft.com/v1.0/$batch
Content-Type: application/json

{
  "requests": [
    { "id": "1", "method": "GET", "url": "/users/{upn}/drive/items/{id1}" },
    { "id": "2", "method": "GET", "url": "/users/{upn}/drive/items/{id2}" }
  ]
}
```

- Mỗi request con trả `status` và `body` riêng; một request lỗi không làm hỏng cả lô.

---

## 8. Webhook (thông báo khi có thay đổi)

```
POST /subscriptions
{
  "changeType": "updated",
  "notificationUrl": "https://your-domain/webhook",
  "resource": "/users/{upn}/drive/root",
  "expirationDateTime": "2026-10-08T00:00:00Z",
  "clientState": "chuoi-bi-mat"
}
```

- Webhook chỉ báo "có thay đổi"; sau đó gọi **delta** (mục 6) để biết thay đổi cụ thể.
- Subscription OneDrive hết hạn sau tối đa khoảng 30 ngày; cần gia hạn bằng `PATCH /subscriptions/{id}`.
- `notificationUrl` phải là HTTPS công khai và phản hồi được bước xác thực (validation token).

---

## 9. Mã lỗi và giới hạn thường gặp

> Mục này là lỗi từ **Graph/OneDrive** (`graph.microsoft.com`). Lỗi khi lấy token từ `login.microsoftonline.com` xem mục 1.4.

### 9.1 Cấu trúc body lỗi

```json
{
  "error": {
    "code": "itemNotFound",
    "message": "The resource could not be found.",
    "innerError": {
      "date": "2026-10-01T15:42:00",
      "request-id": "...",
      "client-request-id": "..."
    }
  }
}
```

- Xử lý theo **HTTP status** kết hợp `error.code`; không dựa vào `message` (có thể thay đổi).
- Ghi lại `request-id` và `date` khi log lỗi để tra cứu.

### 9.2 Bảng mã lỗi

| HTTP | `error.code` thường gặp | Ý nghĩa | Cách xử lý |
|---|---|---|---|
| `400` | `invalidRequest` | Request sai cú pháp (đường dẫn, tham số OData, `$select` không hợp lệ...) | Sửa request, không retry |
| `401` | `InvalidAuthenticationToken` | Token hết hạn, sai hoặc thiếu | Lấy token mới, retry **một** lần |
| `403` | `accessDenied` | Thiếu quyền hoặc chưa Grant admin consent. Message *"Either scp or roles claim need to be present in the token"* nghĩa là token không có quyền Application nào | Kiểm tra quyền `Files.Read.All` (hoặc `Files.ReadWrite.All`) và admin consent; không retry |
| `404` | `itemNotFound` | Item hoặc đường dẫn không tồn tại | Xóa item khỏi cache local nếu có |
| `404` | `ResourceNotFound` (message thường chứa *"mysite not found"*) | UPN sai, user chưa có license, hoặc OneDrive chưa được khởi tạo | Kiểm tra UPN; đăng nhập OneDrive một lần trên web để khởi tạo |
| `409` | `nameAlreadyExists` | Xung đột tên | Đặt `conflictBehavior` |
| `410` | `resyncRequired` | `deltaLink` hết hiệu lực | Bỏ `deltaLink`, quét delta lại từ đầu |
| `416` | `invalidRange` | `Content-Range`/`Range` không hợp lệ | Kiểm tra lại khoảng byte |
| `429` | `activityLimitReached` / `TooManyRequests` | Bị throttle | Chờ đúng số giây trong header `Retry-After` rồi thử lại |
| `500` / `502` / `504` | `generalException`, `serviceNotAvailable`... | Lỗi phía dịch vụ | Retry với exponential backoff |
| `503` | `serviceNotAvailable` | Dịch vụ tạm quá tải | Chờ `Retry-After` (nếu có) rồi thử lại |
| `507` | `quotaLimitReached` | Hết dung lượng | Giải phóng dung lượng hoặc nâng quota |

**Lỗi khi tải qua `downloadUrl`**

- URL tải trỏ tới host SharePoint, không phải Graph, nên lỗi có thể không theo cấu trúc ở 9.1.
- Nhận `401`/`403` khi đang tải thường do URL đã hết hạn (khoảng 1 giờ): gọi lại metadata của item (mục 3.3) để lấy URL mới. **Không** lấy lại access token trong trường hợp này.

### 9.3 Khuyến nghị

- Dùng `$select`, `delta`, `$batch` để giảm số lượng request.
- Retry với exponential backoff và luôn tôn trọng `Retry-After`.
- Phân biệt lỗi có thể retry (`401` một lần, `429`, `5xx`, lỗi mạng) và lỗi không nên retry (`400`, `403`, `404`).

---

## 10. Ví dụ nhanh

### curl

```bash
# 1. Lấy token
curl -X POST "https://login.microsoftonline.com/<TENANT_ID>/oauth2/v2.0/token" \
  -d "client_id=<CLIENT_ID>" \
  -d "client_secret=<CLIENT_SECRET>" \
  -d "scope=https://graph.microsoft.com/.default" \
  -d "grant_type=client_credentials"

# 2. Liệt kê thư mục gốc
curl -H "Authorization: Bearer <ACCESS_TOKEN>" \
  "https://graph.microsoft.com/v1.0/users/lamnb@lamnb.onmicrosoft.com/drive/root/children"

# 3. Lấy link tải
curl -H "Authorization: Bearer <ACCESS_TOKEN>" \
  "https://graph.microsoft.com/v1.0/users/lamnb@lamnb.onmicrosoft.com/drive/items/<ITEM_ID>?select=id,name,@microsoft.graph.downloadUrl"
```

### Python (msal + requests)

```python
import msal, requests

UPN = "lamnb@lamnb.onmicrosoft.com"
app = msal.ConfidentialClientApplication(
    client_id="CLIENT_ID",
    client_credential="CLIENT_SECRET",
    authority="https://login.microsoftonline.com/TENANT_ID",
)
token = app.acquire_token_for_client(scopes=["https://graph.microsoft.com/.default"])
headers = {"Authorization": f"Bearer {token['access_token']}"}

r = requests.get(
    f"https://graph.microsoft.com/v1.0/users/{UPN}/drive/root/children",
    headers=headers,
)
print(r.json())
```

---

## 11. Lưu ý chung

- Tài khoản cần có license bao gồm OneDrive/SharePoint và OneDrive đã được khởi tạo.
- Giữ `client_secret` trong biến môi trường hoặc secret manager; không commit lên Git. Đặt lịch gia hạn trước khi secret hết hạn (tối đa 24 tháng).
- `Files.Read.All`/`Files.ReadWrite.All` kiểu Application cho phép truy cập file của **mọi user** trong tenant; nếu chỉ cần một phạm vi hẹp, dùng `Sites.Selected` hoặc `Files.SelectedOperations.Selected`.
- Hạn mức dung lượng mỗi user phụ thuộc license và cấu hình của tenant (mặc định thường 1 TB, có thể nâng cao hơn). Giá trị `quota.total` trả về có thể thấp hơn hạn mức mong đợi và có thể nhỏ hơn `used`; xem lưu ý ở mục 2.

---

## 12. Tài liệu tham khảo

- OneDrive API overview: https://learn.microsoft.com/graph/onedrive-concept-overview
- DriveItem resource: https://learn.microsoft.com/graph/api/resources/driveitem
- Download a file: https://learn.microsoft.com/graph/api/driveitem-get-content
- Upload large files: https://learn.microsoft.com/graph/api/driveitem-createuploadsession
- Delta: https://learn.microsoft.com/graph/api/driveitem-delta
- Change notifications: https://learn.microsoft.com/graph/api/subscription-post-subscriptions
- Throttling guidance: https://learn.microsoft.com/graph/throttling
- Graph error responses: https://learn.microsoft.com/graph/errors
- Entra ID (AADSTS) error codes: https://learn.microsoft.com/entra/identity-platform/reference-error-codes
