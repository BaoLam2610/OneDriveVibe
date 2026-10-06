package com.lambao.odv.core.data

import com.lambao.odv.core.domain.usecase.connection.CheckSavedConnectionUseCase
import com.lambao.odv.core.domain.usecase.connection.ConnectUseCase
import com.lambao.odv.core.domain.usecase.connection.ResolveStartDestinationUseCase
import com.lambao.odv.core.domain.usecase.folder.GetFolderPathUseCase
import com.lambao.odv.core.domain.usecase.folder.ObserveFolderContentUseCase
import com.lambao.odv.core.domain.usecase.folder.SearchDriveUseCase
import com.lambao.odv.core.domain.usecase.library.ObserveLibraryDaysUseCase
import com.lambao.odv.core.domain.usecase.library.ObserveLibraryPagesUseCase
import com.lambao.odv.core.domain.usecase.security.EnableBiometricUseCase
import com.lambao.odv.core.domain.usecase.security.EnableProtectionUseCase
import com.lambao.odv.core.domain.usecase.security.GetLockStatusUseCase
import com.lambao.odv.core.domain.usecase.security.GetLockoutRemainingUseCase
import com.lambao.odv.core.domain.usecase.security.InitializeSecurityUseCase
import com.lambao.odv.core.domain.usecase.security.LockAppUseCase
import com.lambao.odv.core.domain.usecase.security.ObserveLockStateUseCase
import com.lambao.odv.core.domain.usecase.security.ObserveProtectionUseCase
import com.lambao.odv.core.domain.usecase.security.UnlockWithBiometricUseCase
import com.lambao.odv.core.domain.usecase.security.UnlockWithPinUseCase
import com.lambao.odv.core.domain.usecase.sync.ObserveOnlineAndSyncUseCase
import com.lambao.odv.core.domain.usecase.sync.ObserveSyncStatusUseCase
import com.lambao.odv.core.domain.usecase.sync.RefreshSyncUseCase
import com.lambao.odv.core.domain.usecase.sync.SyncIfStaleUseCase
import com.lambao.odv.core.domain.usecase.viewer.GetImageInfoUseCase
import com.lambao.odv.core.domain.usecase.viewer.GetStreamUrlUseCase
import com.lambao.odv.core.domain.usecase.viewer.ObserveViewerItemsUseCase
import com.lambao.odv.core.domain.usecase.viewer.OpenOriginalImageUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/**
 * Liên kết Koin của các UseCase (ADR-0016). Đặt ở `:core:data` chứ không phải `:core:domain` để domain giữ thuần Kotlin, chỉ phụ
 * thuộc `:core:common` (ADR-0010). `DisconnectUseCase` nằm ở [dataModule] vì cần `getAll()`. Ghép ở MainApplication qua `coreModules`.
 */
val useCaseModule = module {
    // Khởi động và kết nối
    factoryOf(::CheckSavedConnectionUseCase)
    factoryOf(::ResolveStartDestinationUseCase)
    factoryOf(::ConnectUseCase)

    // Bảo mật
    factoryOf(::ObserveLockStateUseCase)
    factoryOf(::ObserveProtectionUseCase)
    factoryOf(::InitializeSecurityUseCase)
    factoryOf(::LockAppUseCase)
    factoryOf(::UnlockWithPinUseCase)
    factoryOf(::UnlockWithBiometricUseCase)
    factoryOf(::GetLockStatusUseCase)
    factoryOf(::GetLockoutRemainingUseCase)
    factoryOf(::EnableProtectionUseCase)
    factoryOf(::EnableBiometricUseCase)

    // Đồng bộ
    factoryOf(::ObserveSyncStatusUseCase)
    factoryOf(::SyncIfStaleUseCase)
    factoryOf(::RefreshSyncUseCase)
    factoryOf(::ObserveOnlineAndSyncUseCase)

    // Thư mục và Thư viện
    factoryOf(::ObserveFolderContentUseCase)
    factoryOf(::SearchDriveUseCase)
    factoryOf(::GetFolderPathUseCase)
    factoryOf(::ObserveLibraryPagesUseCase)
    factoryOf(::ObserveLibraryDaysUseCase)

    // Màn xem ảnh và video
    factoryOf(::ObserveViewerItemsUseCase)
    factoryOf(::GetImageInfoUseCase)
    factoryOf(::OpenOriginalImageUseCase)
    factoryOf(::GetStreamUrlUseCase)
}
