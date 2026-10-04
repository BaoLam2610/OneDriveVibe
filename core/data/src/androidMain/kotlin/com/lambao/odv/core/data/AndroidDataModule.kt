package com.lambao.odv.core.data

import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.memory.MemoryCache
import coil3.request.crossfade
import com.lambao.odv.core.data.original.FileOriginalImageRepository
import com.lambao.odv.core.data.prefs.DataStoreBrowserPreferences
import com.lambao.odv.core.data.prefs.DataStorePlayerPreferences
import com.lambao.odv.core.data.thumbnail.FixedThumbnailQuality
import com.lambao.odv.core.data.thumbnail.GraphThumbnailFetcherFactory
import com.lambao.odv.core.data.thumbnail.ThumbnailCacheResetter
import com.lambao.odv.core.data.thumbnail.ThumbnailKeyer
import com.lambao.odv.core.domain.repository.BrowserPreferences
import com.lambao.odv.core.domain.repository.ConfigRepository
import com.lambao.odv.core.domain.repository.ConnectionResetter
import com.lambao.odv.core.domain.repository.OriginalImageRepository
import com.lambao.odv.core.domain.repository.PlayerPreferences
import com.lambao.odv.core.domain.repository.ThumbnailCache
import com.lambao.odv.core.domain.repository.ThumbnailQuality
import com.lambao.odv.core.network.GraphApi
import okio.Path.Companion.toOkioPath
import org.koin.dsl.binds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Binding Koin của `:core:data` cần Context (bản Android): DataStore lưu tùy chọn hiển thị. Ghép ở MainApplication cùng
 * [dataModule]. Tệp `browser.preferences_pb` nằm trong bộ nhớ riêng của app, không sao lưu (CH-04).
 */
val androidDataModule = module {
    single<BrowserPreferences> { DataStoreBrowserPreferences(createPreferencesDataStore(androidContext(), "browser")) }
    // Chế độ phát và khung hình của màn xem video (Lát 6b, VD-06, VD-20): tệp DataStore riêng (mỗi tệp chỉ được một instance).
    single<PlayerPreferences> { DataStorePlayerPreferences(createPreferencesDataStore(androidContext(), "player")) }
    // Trình tải thumbnail (Lát 4). Cache đĩa nằm ở cacheDir nên không được sao lưu (CH-04) và hệ thống có thể dọn khi
    // thiếu chỗ; mất thì chỉ phải tải lại.
    // Tỉ lệ chất lượng mặc định 100%; bản debug ghi đè bằng giá trị chỉnh ở màn Debug (DebugTools, module Koin đứng sau).
    single<ThumbnailQuality> { FixedThumbnailQuality }
    single<ImageLoader> { createImageLoader(androidContext(), get(), get(), get()) }
    // Xóa cache thumbnail khi ngắt kết nối: DisconnectUseCase gom mọi ConnectionResetter bằng getAll(). Cũng là ThumbnailCache
    // để màn Debug xóa tay.
    single { ThumbnailCacheResetter(get(), get()) } binds arrayOf(ConnectionResetter::class, ThumbnailCache::class)
    // Ảnh gốc (Lát 5): kho tệp riêng, tách khỏi cache thumbnail vì thumbnail cố ý chất lượng thấp; cũng là ConnectionResetter
    // để ngắt kết nối xóa sạch (CD-05).
    single {
        FileOriginalImageRepository(get(), get(), get(), androidContext().cacheDir.resolve("originals"), ORIGINAL_CACHE_MAX_BYTES)
    } binds arrayOf(OriginalImageRepository::class, ConnectionResetter::class)
}

/** Trần dung lượng cache ảnh gốc (BN-01), tạm cố định; Lát 9 nối với giới hạn cache trong Cài đặt (CD). */
private const val ORIGINAL_CACHE_MAX_BYTES = 1024L * 1024 * 1024

/** Trần dung lượng cache thumbnail (BN-01). Lát 9 nối giới hạn này với Cài đặt (CD). */
private const val THUMBNAIL_CACHE_MAX_BYTES = 200L * 1024 * 1024

/** Bộ nhớ RAM cho thumbnail đã giải mã: 15% heap, thấp hơn mặc định của Coil vì Thư viện có thể cuộn rất dài. */
private const val THUMBNAIL_MEMORY_PERCENT = 0.15

private fun createImageLoader(
    context: Context,
    api: GraphApi,
    configs: ConfigRepository,
    quality: ThumbnailQuality,
): ImageLoader =
    ImageLoader.Builder(context)
        .components {
            // GIF động (AN-06): ImageDecoder từ Android 9, bản thấp hơn dùng decoder tự viết của coil-gif.
            if (Build.VERSION.SDK_INT >= 28) add(AnimatedImageDecoder.Factory()) else add(GifDecoder.Factory())
            add(ThumbnailKeyer(quality))
            add(GraphThumbnailFetcherFactory(api, configs, quality))
        }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("thumbnails").toOkioPath())
                .maxSizeBytes(THUMBNAIL_CACHE_MAX_BYTES)
                .build()
        }
        .memoryCache { MemoryCache.Builder().maxSizePercent(context, THUMBNAIL_MEMORY_PERCENT).build() }
        .crossfade(true)
        .build()

private fun createPreferencesDataStore(context: Context, name: String): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(
        // Tệp hỏng thì bắt đầu lại từ mặc định; đây chỉ là tùy chọn hiển thị.
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        produceFile = { context.preferencesDataStoreFile(name) },
    )
