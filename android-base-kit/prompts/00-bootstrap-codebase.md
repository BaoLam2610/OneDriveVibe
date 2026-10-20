# Prompt 00: Dựng codebase từ con số 0

## Trước khi dùng

1. Tạo repo trống (có `git init`), chép **nội dung bên trong** `template/` (`CLAUDE.md`, `CONTRIBUTING.md`, `docs/`) vào gốc repo. Không chép `README.md`, `bai-hoc-tu-onedrivevibe.md` hay `prompts/` của Base Kit (xem mục "Cách dùng" ở `README.md` của Base Kit).
2. Thay các chỗ trống `{{TEN_DU_AN}}`, `{{PACKAGE}}`, `Pfx`, `{{MIN_SDK}}`, `{{NGAY}}` (xem `README.md` của Base Kit). Trong `CLAUDE.md`, chọn một trong hai câu ở mục "Build và test".
3. Mở Claude Code ở gốc repo, dán phần **Prompt** bên dưới (sửa các dòng có `<...>`).
4. Prompt chạy **từng phase**. Sau mỗi phase AI dừng; bạn build, đọc diff, commit, rồi trả lời "tiếp phase N".

Nếu chưa có thiết kế: phase 3 dựng token **giữ chỗ** có đánh dấu rõ, để thay khi có file token thật.

---

## Prompt

````text
Bạn là kỹ sư Android cấp cao. Nhiệm vụ: dựng codebase khởi đầu cho dự án {{TEN_DU_AN}} theo đúng bộ tài liệu đã có trong repo này. Chưa có tính năng nghiệp vụ; chỉ dựng khung, một feature mẫu để minh họa, công cụ debug, test nền và CI.

## Đọc trước (bắt buộc, đọc hết rồi mới làm)
- CLAUDE.md
- docs/README.md, docs/architecture.md, docs/tech-stack.md, docs/debug-tools.md
- Mọi file trong docs/conventions/ và docs/adr/
- docs/process/lam-viec-voi-ai.md

Khi tài liệu mâu thuẫn nhau: ADR thắng tech-stack, tech-stack thắng conventions. Gặp mâu thuẫn thì báo tôi, không tự chọn.

## Thông tin dự án
- Package gốc: {{PACKAGE}}
- Tiền tố UI: Pfx
- minSdk: {{MIN_SDK}}, targetSdk/compileSdk: bản stable mới nhất
- Ngôn ngữ app: <vi (mặc định), en>
- Backend: <URL gốc của API hoặc "chưa có, dùng nguồn giả">
- Xác thực: <OAuth2 bearer + refresh token | API key | chưa có>
- CI: <GitHub Actions | GitLab CI | Bitbucket | chưa có>
- Offline-first: <có (viết thêm ADR) | không>

## Luật làm việc
- Làm TỪNG PHASE theo danh sách dưới. Hết mỗi phase thì DỪNG và báo cáo theo mẫu "Báo cáo phase", chờ tôi trả lời "tiếp".
- Không ghi đè file có sẵn bằng Write; chỉ Edit, Read lại trước khi sửa.
- {{CHON_MOT: "Được chạy ./gradlew để kiểm sau mỗi phase" | "Không chạy Gradle; tôi tự build. Liệt kê rủi ro build ở cuối mỗi phase"}}.
- Phiên bản thư viện: tra bản STABLE mới nhất (Context7, Maven Central, Google Maven), không alpha/beta trừ khi không có bản stable và phải ghi lý do. Ghi các cặp phải khớp nhau (Kotlin–KSP–Compose compiler) bằng comment trong libs.versions.toml. Không lấy phiên bản từ trí nhớ.
- API ít phổ biến hoặc mới (Navigation 3, Room driver, Koin verify, Konsist, Turbine): đọc tài liệu hoặc mã nguồn đúng phiên bản đã chọn trước khi viết.
- Không tự chế màu, cỡ chữ, khoảng cách, câu chữ. Chưa có thiết kế thì dùng token giữ chỗ (phase 3) và đánh dấu.
- Không commit, không push. Cuối mỗi phase gợi ý một tên commit (Conventional Commits).
- Mọi chỗ đi khác tài liệu thì comment nêu lý do và ADR.

## Các phase

### Phase 0: Kế hoạch và phiên bản (KHÔNG viết code)
- Liệt kê phiên bản đề xuất cho mọi thư viện ở docs/tech-stack.md (bảng: thư viện, phiên bản, nguồn tra, ghi chú cặp).
- Mốc tham khảo (bản đã chạy ổn ở dự án trước, 10/2026; chỉ để biết mức tối thiểu, vẫn phải tra bản stable mới nhất): Kotlin 2.4.20, AGP 9.1.1, KSP 2.3.12, Lifecycle 2.11.0, Navigation 3 1.2.0, Koin 4.2.2, Coroutines 1.11.0, kotlinx.serialization 1.11.0, Ktor 3.6.0, Room 3.0.3 (nhóm androidx.room3), DataStore 1.2.1, Coil 3.6.3, Paging 3.5.1, Kermit 2.2.0, AppCompat 1.8.0. Bản mới hơn có thay đổi phá vỡ thì báo tôi.
- Liệt kê các module sẽ tạo và convention plugin sẽ có.
- Nêu câu hỏi còn mở (nếu có). DỪNG chờ duyệt.

### Phase 1: Gradle và khung module
- settings.gradle.kts (includeBuild("build-logic"), TYPESAFE_PROJECT_ACCESSORS, repositories), build.gradle.kts gốc (plugin `apply false`), gradle.properties (bật configuration cache nếu các plugin hỗ trợ, nonTransitiveRClass), gradle/libs.versions.toml, .editorconfig, .gitignore (local.properties, keystore, build/, .idea có chọn lọc).
- build-logic/convention: pfx.android.application, pfx.android.library, pfx.android.compose, pfx.android.feature (library + compose + :core:domain, :core:ui, :core:designsystem, :core:common, koin-androidx-compose, lifecycle; testImplementation :core:testing, junit, coroutines-test, turbine), pfx.jvm.library (Kotlin/JVM), pfx.android.room (KSP + room + schemaDirectory), pfx.quality (ktlint + detekt + compose-rules). Mỗi plugin một việc.
- Module rỗng (chỉ build.gradle.kts + package): :app, :core:common, :core:domain, :core:data, :core:network, :core:database, :core:designsystem, :core:ui, :core:testing, :tools:debug.
- Phụ thuộc giữa module đúng bảng ở docs/architecture.md mục 1. Feature KHÔNG thấy :core:data/:core:network/:core:database.

### Phase 2: Nền chung
- :core:common: AppResult (+ map, flatMap, fold, onSuccess, onFailure, getOrNull, getOrElse), AppError (Unknown.toString an toàn), helper `catching` (không nuốt CancellationException), DispatcherProvider + DefaultDispatcherProvider.
- :core:domain: các gói model/repository/platform/settings/hook/usecase (mỗi gói một file README ngắn hoặc KDoc package); `DataResetter` (hook), `LogoutUseCase` (gọi mọi DataResetter trong NonCancellable, mỗi cái try/catch riêng), interface `ThemeSettings`, `AppLocaleController`, `NetworkMonitor`, `PreferencesInspector`.
- :core:ui: BaseMviViewModel (đúng như conventions/mvi.md), CollectEffects, UiError + toUiError + message(), rememberDelayed, pfxLocale(), chuỗi common_* (vi + en).
- :core:testing: MainDispatcherRule, TestDispatcherProvider, FixedClock, testMvi helper, FakeNetworkMonitor.
- Test: AppResult (getOrElse/flatMap), catching (ném lại CancellationException).

### Phase 3: Design system
- :core:designsystem: PfxTheme (sáng/tối, KHÔNG dynamic color), PfxColors, PfxTypography (includeFontPadding = false, LineHeightStyle Center/Trim.None), PfxSpacing, PfxRadius, PfxSize, PfxMotion, PfxTransitions (fade theo token), PfxIcon (rỗng, chờ bộ icon).
- Token GIỮ CHỖ khi chưa có thiết kế: dùng một bảng màu trung tính tối thiểu, đặt trong `PlaceholderTokens.kt` có KDoc "GIỮ CHỖ: thay bằng token từ docs/design/tokens.json". Không rải giá trị giữ chỗ ra màn hình.
- Component nền: PfxDialog, PfxBottomSheet (gọi LocalPfxTopOverlay để nút bọ vẽ trên chúng), PfxListSkeleton, PfxGridSkeleton (shimmer, đứng yên khi Giảm chuyển động), PfxSecureWindowPolicy (cấp tiến trình, track(window), appWide + đếm yêu cầu theo Window) và composable PfxSecureWindow() cho màn nhạy cảm.
- :app/src/debug: DesignGalleryActivity (không LAUNCHER, exported=false) hiện token và component.

### Phase 4: Mạng và CSDL
- :core:network: createApiClient (đúng conventions/data-layer.md mục 2.1: OkHttp engine, ContentNegotiation, HttpTimeout, HttpRequestRetry, Auth bearer nếu dự án có token, followRedirects=false, expectSuccess=false, KHÔNG Logging), client tải không bearer, safeGet/safePost đổi lỗi sang AppError, HttpTrafficEntry + HttpTrafficRecorder + masked() (docs/debug-tools.md mục 3.1), ghi lưu lượng ở một chỗ, NetworkConstants.kt, networkModule (recorder lấy bằng getOrNull).
- :core:database: AppDatabase (exportSchema, KDoc "Lịch sử version"), driver theo docs/tech-stack.md mục 3, databaseModule. Entity đầu tiên là của feature mẫu (phase 7).

### Phase 5: Data và app shell
- :core:data: PreferencesDataSource (conventions/data-layer.md mục 4), DataStoreThemeSettings, DataStorePreferencesInspector, DatabaseResetter (clearAllTables + VACUUM + wal_checkpoint), PreferencesResetter, DataConstants.kt (tên tệp/khóa với ghi chú "không được đổi giá trị"), dataModule, useCaseModule, `val coreModules`.
- :app: MainApplication (DebugTools.install trước startKoin; modules: appModule + coreModules + featureModules + DebugTools.koinModules cuối cùng; Koin logger ERROR), MainActivity (AppCompatActivity, enableEdgeToEdge, ThemeBootCache đọc đồng bộ + setTheme trước super.onCreate, PfxSecureWindowPolicy.track(window), CompositionLocalProvider(LocalPfxTopOverlay), DebugTools.Overlay()), AppRoute + AppNavDisplay (Navigation 3, decorator saveable + viewModelStore, Splash → Home rỗng), AndroidAppLocaleController, AndroidNetworkMonitor, ForegroundSignal nếu cần.
- Manifest: allowBackup=false, fullBackupContent=false, dataExtractionRules loại mọi miền, localeConfig, configChanges có locale|layoutDirection (comment lý do, ADR-0010), AppLocalesMetadataHolderService autoStoreLocales, network_security_config không cleartext, android:screenOrientation nếu đặc tả khóa hướng.
- res: values + values-en (app_name...), locales_config.xml, localeFilters.
- :app/src/debug/DebugTools.kt và :app/src/release/DebugTools.kt (bản release: Logger.setLogWriters(emptyList())). Bản debug ở phase này chỉ có Kermit + recorder; giao diện ở phase 6.

### Phase 6: Công cụ debug
- Làm theo docs/debug-tools.md (toàn bộ), chia nhỏ như prompts/01-port-debug-tools.md của Base Kit (tôi sẽ dán nếu cần): cổng + nút bọ + DebugActivity khung → tab API → tab Log → tab Lưu trữ + trình duyệt CSDL → tab Khác (build info có commit hash qua BuildConfig, công tắc che, bảo vệ màn hình, DebugActions).
- Kiểm: :app releaseRuntimeClasspath không có :tools:debug.

### Phase 7: Feature mẫu `:feature:sample`
Mục đích: minh họa một lát đầy đủ để team chép theo. Ghi KDoc đầu module: "Feature mẫu minh họa một lát đầy đủ; xóa khi có feature thật đầu tiên hoặc giữ làm tham chiếu".
- Domain: SampleItem, SampleRepository, ObserveSampleItemsUseCase, RefreshSampleItemsUseCase, ToggleFavoriteUseCase.
- Data: SampleItemEntity + SampleDao (AppDatabase version 1), SampleRemoteDataSource (nếu chưa có backend: bản giả trả dữ liệu cố định sau delay ngắn, có thể giả lỗi), SampleRepositoryImpl (offline-first: UI đọc Room, refresh ghi Room), đăng ký DataResetter.
- UI: SampleContract, SampleViewModel, PfxSampleScreen, PfxSampleContent (danh sách, rỗng, lỗi, skeleton sau 200 ms, kéo để làm mới, dialog xác nhận nằm trong State, Effect mở chi tiết), màn chi tiết đơn giản, route AppRoute.SampleDetail(id) có tham số mặc định minh họa, chuỗi vi + en.
- Test: SampleViewModelTest (đủ checklist ở conventions/testing.md mục 3), ToggleFavoriteUseCaseTest, FakeSampleRepository.
- :app/src/test: KoinVerifyTest, ArchitectureTest (Konsist: feature/app không import domain.repository, feature không import feature, ViewModel kế thừa BaseMviViewModel, domain không import android).

### Phase 8: Chất lượng và CI
- ktlint + detekt (config/detekt/detekt.yml, compose-rules), Android Lint (lint.xml: MissingTranslation error; baseline nếu cần).
- CI cho mọi PR: build debug, ktlintCheck, detekt, lintDebug, testDebugUnitTest; cache Gradle. Bước kiểm releaseRuntimeClasspath không có :tools:debug.
- README.md ở gốc: chạy dự án, cấu trúc module (trỏ docs/architecture.md), lệnh kiểm trước khi đẩy.
- Tạo docs/plan/tien-do.md từ docs/templates/tien-do.md, đánh dấu L0 và D theo tiến độ thật.

## Báo cáo phase (mẫu, dùng sau MỖI phase)
1. Đã làm: danh sách file tạo/sửa (đường dẫn), mỗi file một dòng.
2. Lệch so với tài liệu (nếu có) và lý do.
3. Rủi ro build: API chưa chắc, phiên bản cần đối chiếu, lỗi có thể gặp.
4. Cách kiểm: lệnh Gradle + mục kiểm tay.
5. Câu hỏi cho tôi.
6. Tên commit gợi ý.
Rồi DỪNG.

Bắt đầu với Phase 0.
````
