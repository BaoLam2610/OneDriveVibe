package com.lambao.odv.core.data

import com.lambao.odv.core.data.config.ConfigCredentialsSource
import com.lambao.odv.core.data.config.ConfigRepositoryImpl
import com.lambao.odv.core.data.config.ConfigVault
import com.lambao.odv.core.data.drive.ConnectionRepositoryImpl
import com.lambao.odv.core.data.drive.FolderRepositoryImpl
import com.lambao.odv.core.data.drive.LibraryRepositoryImpl
import com.lambao.odv.core.data.drive.ShortRepositoryImpl
import com.lambao.odv.core.data.drive.ViewerRepositoryImpl
import com.lambao.odv.core.data.drive.VideoStreamRepositoryImpl
import com.lambao.odv.core.data.security.LockoutStore
import com.lambao.odv.core.data.security.SecurityRepositoryImpl
import com.lambao.odv.core.data.sync.SyncCoordinator
import com.lambao.odv.core.data.sync.SyncEngine
import com.lambao.odv.core.domain.repository.ConfigRepository
import com.lambao.odv.core.domain.hook.ConnectionResetter
import com.lambao.odv.core.domain.repository.ConnectionRepository
import com.lambao.odv.core.domain.repository.FolderRepository
import com.lambao.odv.core.domain.repository.LibraryRepository
import com.lambao.odv.core.domain.repository.SecurityRepository
import com.lambao.odv.core.domain.repository.SyncRepository
import com.lambao.odv.core.domain.repository.VideoStreamRepository
import com.lambao.odv.core.domain.repository.ShortRepository
import com.lambao.odv.core.domain.repository.ViewerRepository
import com.lambao.odv.core.domain.usecase.DisconnectUseCase
import com.lambao.odv.core.network.graph.GraphCredentialsSource
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module
import kotlin.time.Clock

/**
 * Binding Koin của `:core:data`. Ghép ở MainApplication qua `coreModules`. Dùng `singleOf(::X)` để Koin tự phân giải constructor
 * theo kiểu: thêm hay đổi tham số của lớp không phải sửa lại module này.
 */
val dataModule = module {
    // Giờ thực dùng chung (lastSyncedAt, quá hạn đồng bộ DS-04). Tiêm kiểu Clock chứ không phải `() -> Long`: rõ nghĩa và Koin
    // phân biệt được theo kiểu. Ở đây vì hiện chỉ :core:data dùng; dời sang domainModule khi UseCase cần (R5).
    single<Clock> { Clock.System }
    // ConfigVault dùng chung cho ConfigRepository và SecurityRepository: một bản config trong bộ nhớ, một trạng thái khóa.
    singleOf(::ConfigVault)
    singleOf(::LockoutStore)
    singleOf(::ConfigRepositoryImpl) bind ConfigRepository::class
    // Credentials cho GraphApi lấy từ config đã lưu, ở mỗi lời gọi (network chỉ biết interface GraphCredentialsSource).
    singleOf(::ConfigCredentialsSource) bind GraphCredentialsSource::class
    // Tùy chọn bảo mật của Cài đặt (SecuritySettings): cần DataStore nên được cài ở androidDataModule (cùng tệp DataStore `settings`).
    // BiometricAuthenticator do :androidApp cung cấp (cần Activity để hiện BiometricPrompt).
    singleOf(::SecurityRepositoryImpl) bind SecurityRepository::class
    // Dữ liệu drive chia theo việc (R5a): kết nối thử, duyệt thư mục, tab Thư viện, các màn xem.
    singleOf(::ConnectionRepositoryImpl) bind ConnectionRepository::class
    singleOf(::FolderRepositoryImpl) bind FolderRepository::class
    singleOf(::LibraryRepositoryImpl) bind LibraryRepository::class
    singleOf(::ViewerRepositoryImpl) bind ViewerRepository::class
    // Danh sách video của tab Short (SV-01): đọc id từ Room.
    singleOf(::ShortRepositoryImpl) bind ShortRepository::class
    // Link phát video (Lát 6, VD-14): trình phát hỏi lại mỗi lần mở kết nối vì link chỉ sống khoảng 1 giờ.
    singleOf(::VideoStreamRepositoryImpl) bind VideoStreamRepository::class
    // Đồng bộ delta về Room (ADR-0007). SyncCoordinator vừa là SyncRepository vừa là ConnectionResetter của Room nên
    // DisconnectUseCase (getAll) tự thấy nó mà không sửa chỗ khác.
    singleOf(::SyncEngine)
    singleOf(::SyncCoordinator) binds arrayOf(
        SyncRepository::class,
        ConnectionResetter::class,
    )
    // getAll: mỗi lát đăng ký ConnectionResetter của mình (Room, cache, cài đặt) mà không phải sửa chỗ này.
    factory { DisconnectUseCase(getAll(), get()) }
}
