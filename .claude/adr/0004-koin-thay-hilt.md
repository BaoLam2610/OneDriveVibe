# ADR-0004: Koin thay cho Hilt

**Ngày**: 2026-10-02
**Trạng thái**: accepted
**Người quyết định**: LamBao

## Bối cảnh
Cần dependency injection dùng được trong các module KMP (`domain`, `data`...), không chỉ trong module Android.

## Quyết định
Dùng **Koin 4.x** (`viewModelOf`, `koinViewModel()`), khai báo module theo từng tầng.

## Phương án đã cân nhắc
### Hilt
- Ưu: kiểm tra phụ thuộc lúc biên dịch, chuẩn của Android.
- Nhược: chỉ dùng được trên Android (Dagger, kapt/KSP), không dùng được trong `commonMain`.
- Lý do không chọn: mâu thuẫn với ADR-0001.
### kotlin-inject / Manual DI
- Ưu: kiểm tra lúc biên dịch, hỗ trợ KMP.
- Nhược: ít tài liệu hơn, hoặc phải tự viết nhiều.
- Lý do không chọn: Koin đơn giản và đủ cho dự án cá nhân.

## Hệ quả
### Tích cực
- Một cách DI duy nhất cho cả code dùng chung và code Android.
### Tiêu cực
- Thiếu binding chỉ lộ khi chạy, không lộ lúc biên dịch.
### Rủi ro
- Crash do quên khai báo khi thêm feature. Giảm thiểu bằng cách chạy qua màn đó ở bản debug sau mỗi lần thêm module Koin.
