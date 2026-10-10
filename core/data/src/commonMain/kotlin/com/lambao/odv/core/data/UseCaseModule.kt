package com.lambao.odv.core.data

import com.lambao.odv.core.domain.usecase.cache.ClearCacheUseCase
import com.lambao.odv.core.domain.usecase.cache.GetCacheUsageUseCase
import com.lambao.odv.core.domain.usecase.cache.ObserveCacheBudgetUseCase
import com.lambao.odv.core.domain.usecase.cache.SetCacheLimitUseCase
import com.lambao.odv.core.domain.usecase.cache.SetCacheSharesUseCase
import com.lambao.odv.core.domain.usecase.connection.CheckSavedConnectionUseCase
import com.lambao.odv.core.domain.usecase.connection.ConnectUseCase
import com.lambao.odv.core.domain.usecase.connection.UpdateClientSecretUseCase
import com.lambao.odv.core.domain.usecase.connection.ResolveStartDestinationUseCase
import com.lambao.odv.core.domain.usecase.folder.GetFolderPathUseCase
import com.lambao.odv.core.domain.usecase.folder.ObserveFolderContentUseCase
import com.lambao.odv.core.domain.usecase.folder.SearchDriveUseCase
import com.lambao.odv.core.domain.usecase.library.ObserveLibraryDaysUseCase
import com.lambao.odv.core.domain.usecase.library.ObserveLibraryPagesUseCase
import com.lambao.odv.core.domain.usecase.security.ChangePinUseCase
import com.lambao.odv.core.domain.usecase.security.DisableBiometricUseCase
import com.lambao.odv.core.domain.usecase.security.DisableProtectionUseCase
import com.lambao.odv.core.domain.usecase.security.EnableBiometricUseCase
import com.lambao.odv.core.domain.usecase.security.EnableProtectionUseCase
import com.lambao.odv.core.domain.usecase.security.GetBiometricStatusUseCase
import com.lambao.odv.core.domain.usecase.security.GetLockStatusUseCase
import com.lambao.odv.core.domain.usecase.security.GetLockoutRemainingUseCase
import com.lambao.odv.core.domain.usecase.security.InitializeSecurityUseCase
import com.lambao.odv.core.domain.usecase.security.LockAppUseCase
import com.lambao.odv.core.domain.usecase.security.ObserveLockStateUseCase
import com.lambao.odv.core.domain.usecase.security.ObserveProtectionUseCase
import com.lambao.odv.core.domain.usecase.security.UnlockWithBiometricUseCase
import com.lambao.odv.core.domain.usecase.security.UnlockWithPinUseCase
import com.lambao.odv.core.domain.usecase.security.VerifyPinUseCase
import com.lambao.odv.core.domain.usecase.settings.GetLanguageUseCase
import com.lambao.odv.core.domain.usecase.settings.ObserveConnectionInfoUseCase
import com.lambao.odv.core.domain.usecase.settings.ObserveLastListTabUseCase
import com.lambao.odv.core.domain.usecase.settings.ObserveShortMaxMinutesUseCase
import com.lambao.odv.core.domain.usecase.shorts.GetShortVideoUseCase
import com.lambao.odv.core.domain.usecase.shorts.ObserveShortTabAvailableUseCase
import com.lambao.odv.core.domain.usecase.shorts.ObserveShortVideoIdsUseCase
import com.lambao.odv.core.domain.usecase.settings.SetLastListTabUseCase
import com.lambao.odv.core.domain.usecase.settings.ObserveSecretExpiryNoticeUseCase
import com.lambao.odv.core.domain.usecase.settings.ObserveSecretExpiryUseCase
import com.lambao.odv.core.domain.usecase.settings.ObserveThemeModeUseCase
import com.lambao.odv.core.domain.usecase.settings.SetLanguageUseCase
import com.lambao.odv.core.domain.usecase.settings.SetSecretExpiryUseCase
import com.lambao.odv.core.domain.usecase.settings.SetThemeModeUseCase
import com.lambao.odv.core.domain.usecase.settings.ToggleFileKindUseCase
import com.lambao.odv.core.domain.usecase.sync.ObserveOnlineAndSyncUseCase
import com.lambao.odv.core.domain.usecase.sync.ObserveSyncStatusUseCase
import com.lambao.odv.core.domain.usecase.sync.RefreshSyncUseCase
import com.lambao.odv.core.domain.usecase.sync.SyncIfStaleUseCase
import com.lambao.odv.core.domain.usecase.sync.SyncOnForegroundUseCase
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
    factoryOf(::UpdateClientSecretUseCase)

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
    factoryOf(::SyncOnForegroundUseCase)
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

    // Cài đặt (Lát 7)
    factoryOf(::ObserveThemeModeUseCase)
    factoryOf(::SetThemeModeUseCase)
    // Tab Short (Lát 8b): danh sách video, chi tiết, điều kiện hiện mục và cài đặt thời lượng tối đa (SV-01, DH-01, CD-13).
    factoryOf(::ObserveShortVideoIdsUseCase)
    factoryOf(::GetShortVideoUseCase)
    factoryOf(::ObserveShortTabAvailableUseCase)
    factoryOf(::ObserveShortMaxMinutesUseCase)
    // Tab Thư mục/Thư viện dùng gần nhất (DH-06).
    factoryOf(::ObserveLastListTabUseCase)
    factoryOf(::SetLastListTabUseCase)
    factoryOf(::ObserveSecretExpiryUseCase)
    factoryOf(::SetSecretExpiryUseCase)
    factoryOf(::ObserveSecretExpiryNoticeUseCase)
    factoryOf(::GetLanguageUseCase)
    factoryOf(::SetLanguageUseCase)
    factoryOf(::ObserveConnectionInfoUseCase)
    factoryOf(::ToggleFileKindUseCase)

    // Bộ nhớ đệm (7d): các kho (thumbnail, ảnh gốc, video, PDF) tự đăng ký CacheStore; getAll() gom chúng như DisconnectUseCase.
    factoryOf(::ObserveCacheBudgetUseCase)
    factory { GetCacheUsageUseCase(getAll()) }
    factory { ClearCacheUseCase(getAll()) }
    factory { SetCacheLimitUseCase(get(), getAll()) }
    factory { SetCacheSharesUseCase(get(), getAll()) }
    factoryOf(::VerifyPinUseCase)
    factoryOf(::ChangePinUseCase)
    factoryOf(::DisableProtectionUseCase)
    factoryOf(::DisableBiometricUseCase)
    factoryOf(::GetBiometricStatusUseCase)
}
