# Nghiên cứu: kiến trúc player để video kế tiếp hiện hình ngay khi vuốt (tab Short)
*Ngày: 2026-10-11 | Nguồn: 9 | Độ tin cậy: Trung bình (không app lớn nào công bố chi tiết nội bộ; có tài liệu chính thức của Google/Meta và SDK của ByteDance)*

## Tóm tắt
Có hai trường phái, cả hai đều được app lớn dùng thật:
1. **Một player + PreloadManager** (khuyến nghị của Google/Media3). Meta đã chuyển Facebook và Instagram Reels sang cách này năm 2026, **bỏ** cách cũ là "warmup" nhiều player vì tốn bộ nhớ. Video kế cận được chuẩn bị và đệm sẵn trong bộ nhớ nhưng tài liệu không nói là giải mã/vẽ sẵn khung hình.
2. **Một player phát + một player "pre-render" video kế tiếp** (SDK video ngắn của BytePlus/ByteDance, công ty của TikTok). Player phụ giải mã và **vẽ sẵn khung hình đầu** của video kế tiếp, chỉ một video kế tiếp, không giữ video trước; khung hình đó dùng luôn làm ảnh bìa.

Yêu cầu "vuốt sang là hình đã hiện, không có khoảnh khắc đen" chỉ trường phái 2 đáp ứng trọn vẹn. Trường phái 1 rút thời gian tới khung hình đầu rất ngắn nhưng vẫn phải giải mã khung đầu sau khi vuốt.

## 1. Khuyến nghị chính thức của Google/Media3
- `DefaultPreloadManager` giữ một tập nguồn ứng viên xếp hạng theo khoảng cách tới vị trí hiện tại, chuẩn bị và đệm trước ([Android Developers Blog, 09/2025](https://android-developers.googleblog.com/2025/09/introducing-preloading-with-media3.html)).
- Cơ chế: "The first period of the next window is prepared and video, audio and text samples are buffered. The preloaded period is later queued into the player with buffered samples immediately available and ready to be fed to the codec for rendering" ([developer.android.com, Part 1](https://developer.android.com/blog/posts/elevating-media-playback-introducing-preloading-with-media3-part-1)). Nghĩa là dữ liệu đã sẵn nhưng **chưa giải mã**; đây là suy luận từ câu chữ, bài không nói thẳng.
- Bắt buộc dựng ExoPlayer và manager từ **cùng một builder** để chia sẻ thành phần bên trong (cùng nguồn). Không có khuyến nghị số lượng player; phần 2 của loạt bài (chưa đọc được) nói về chia sẻ thành phần tùy biến.
- Media3 1.8.0 có `ExoPlayer.setScrubbingModeEnabled(true)` cho tua liên tục khi kéo thanh ([Media3 1.8.0](https://android-developers.googleblog.com/2025/08/media3-180-whats-new.html)).

## 2. Meta (Instagram Reels, Facebook)
- Trước: "warmup" khởi tạo **nhiều player lần lượt**, "consumed significant memory", nên chỉ preload được vài video ([Android Developers Blog, 03/2026](https://android-developers.googleblog.com/2026/03/instagram-and-facebook-deliver-instant.html)).
- Sau: dùng `DefaultPreloadManager`, một player quản lý nhiều video ("manage many videos using a single player instance"). Reels dùng "adjacent preload": video trước và sau nằm sẵn trong bộ nhớ.
- Kết quả chỉ định tính: khởi động nhanh hơn, Time to First Frame tốt hơn, giờ xem tăng. Preload quá tay làm tăng bộ nhớ và giật khi cuộn, phải thêm cơ chế tạm dừng preload khi CPU/IO căng.

## 3. ByteDance/TikTok (qua SDK BytePlus)
- Pre-render: "preemptively creating a player to decode and render the next video"; "Only one video can be pre-rendered"; không hỗ trợ video trước; SDK tự giải phóng engine cũ trước khi tạo engine mới, có tùy chọn giải phóng theo LRU ([BytePlus Android short video best practices](https://docs.byteplus.com/vi/docs/byteplus-vod/docs-android-player-short-video-best-practices)).
- Dùng khung hình đầu đã vẽ sẵn làm ảnh bìa; preload thêm index+1..n chỉ ở mức dữ liệu.
- Tuyên bố của nhà cung cấp: thời gian tới khung hình đầu 100 đến 300ms, không có phương pháp đo.

## 4. Giới hạn bộ giải mã phần cứng
- Số phiên giải mã đồng thời **tùy máy và tùy codec**, khai báo trong cấu hình codec của hãng (vd. `concurrent-instances max="2"` cho một số thành phần trên máy Google, 16 cho bộ HEVC ở máy khác) ([AOSP media_codecs](https://android.googlesource.com/device/google/zuma/+/10343c4/media_codecs_aosp_c2.xml)).
- Hết phiên thì ExoPlayer ném `DecoderInitializationException`; app khác đang giữ decoder cũng gây lỗi này ([báo cáo trên issue ExoPlayer](https://lightrun.com/answers/google-exoplayer-release-codec-resources-hold-by-other-apps)). Lời khuyên chung: giữ ít player, tái dùng qua pool, giải phóng khi rời màn ([Uptech](https://www.uptech.team/blog/how-to-play-multiple-videos-using-the-recyclerview-and-exoplayer)).
- Các bài thiết kế không chính thức mô tả pool khoảng 3 player trước/hiện tại/sau ([techinterview](https://www.techinterview.org/post/3233474985/design-tiktok-video-feed-mobile/)); đây là nguồn phụ, chưa kiểm chứng.

## Đối chiếu với OneDriveVibe
- Hiện tại: một player + tải trước byte và link bằng `CacheWriter` (ADR-0026), gần trường phái 1 nhưng chưa chuẩn bị track. Kiểm tay: còn 100 đến 200ms đen khi vuốt.
- Thiết kế hai player trong `tien-do-mvp1.md` **trùng với cách của ByteDance**: chỉ pre-render một video kế tiếp, video trước nạp theo kiểu thường. Đây là hướng đã được dùng ở quy mô lớn, không phải tự chế.
- Rủi ro riêng của dự án: máy Xiaomi/MediaTek dùng FFmpeg phần mềm cho HEVC 10-bit (ADR-0018), hai phiên phần mềm tốn CPU; giới hạn phiên phần cứng tùy máy.

## Khuyến nghị
1. Làm hai player theo mẫu ByteDance (lát con 8d, ADR-0027 sửa ADR-0024 mục 1), có các chốt an toàn:
   - Chỉ một player pre-render, chỉ video kế tiếp; tạo lười, giải phóng khi rời Màn chính và khi app bị khóa.
   - Lỗi khởi tạo decoder ở player pre-render thì bỏ pre-render cho video đó và quay về cách hiện tại (không báo lỗi cho người dùng).
   - Không pre-render khi video hiện tại đang dùng bộ giải mã FFmpeg phần mềm, hoặc khi chính video kế tiếp cần FFmpeg (biết qua log `onVideoDecoderInitialized`).
   - Giữ `CacheWriter` cho video trước/sau ở mức dữ liệu.
2. Nếu thử máy thấy rủi ro decoder lớn, phương án lùi là chuyển sang `DefaultPreloadManager` (cách của Meta) với `TRACKS_SELECTED` + đoạn đầu đã tải, chấp nhận vài chục ms giải mã sau khi vuốt.

## Nguồn
1. [Introducing preloading with Media3, Part 1 (Android Developers Blog)](https://android-developers.googleblog.com/2025/09/introducing-preloading-with-media3.html): PreloadManager và cùng builder.
2. [Elevating media playback, Part 1 (developer.android.com)](https://developer.android.com/blog/posts/elevating-media-playback-introducing-preloading-with-media3-part-1): cơ chế preload, PreloadStatus.
3. [Instagram and Facebook deliver instant playback (Android Developers Blog, 03/2026)](https://android-developers.googleblog.com/2026/03/instagram-and-facebook-deliver-instant.html): Meta bỏ nhiều player, dùng một player + PreloadManager.
4. [Media3 1.8.0 what's new](https://android-developers.googleblog.com/2025/08/media3-180-whats-new.html): scrubbing mode.
5. [BytePlus Android Player short video best practices](https://docs.byteplus.com/vi/docs/byteplus-vod/docs-android-player-short-video-best-practices): pre-render một video kế tiếp.
6. [AOSP media_codecs_aosp_c2.xml (zuma)](https://android.googlesource.com/device/google/zuma/+/10343c4/media_codecs_aosp_c2.xml): concurrent-instances.
7. [ExoPlayer: codec resources held by other apps](https://lightrun.com/answers/google-exoplayer-release-codec-resources-hold-by-other-apps): DecoderInitializationException.
8. [Uptech: multiple videos with RecyclerView and ExoPlayer](https://www.uptech.team/blog/how-to-play-multiple-videos-using-the-recyclerview-and-exoplayer): pool có giới hạn.
9. [Design TikTok-style video feed](https://www.techinterview.org/post/3233474985/design-tiktok-video-feed-mobile/): pool ~3 player (nguồn phụ).

## Phương pháp
6 truy vấn web (WebSearch; không có MCP firecrawl/exa), đọc kỹ 3 nguồn (bài Meta, Media3 Part 1, BytePlus). Câu hỏi con: khuyến nghị Media3; app lớn làm gì; giới hạn bộ giải mã. Thiếu: không có tài liệu chính thức của TikTok hay YouTube Shorts; chưa đọc được phần 2 và 3 của loạt bài Media3.
