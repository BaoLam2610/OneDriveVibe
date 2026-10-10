package com.lambao.odv.core.data.prefs

import co.touchlab.kermit.Logger
import com.lambao.odv.core.domain.hook.ConnectionResetter
import kotlinx.coroutines.CancellationException

/**
 * Xóa cài đặt hiển thị khi ngắt kết nối (CD-05: "xóa config, khóa mã hóa, dữ liệu đồng bộ, lịch sử xem, cache và cài đặt").
 * Mỗi tệp DataStore chỉ được một instance nên nhận đúng các [PreferencesDataSource] đã có thay vì tự mở lại tệp.
 * Tệp vẫn còn nhưng rỗng; lựa chọn lần sau về mặc định. Thêm tệp DataStore mới (Lát 9: ngôn ngữ...) thì thêm vào danh sách ở Koin.
 *
 * Nuốt lỗi và chỉ ghi tên loại ngoại lệ (hợp đồng [ConnectionResetter], CH-06).
 */
internal class PreferencesResetter(
    private val sources: List<PreferencesDataSource>,
) : ConnectionResetter {

    private val log = Logger.withTag("Prefs")

    override suspend fun reset() {
        for (source in sources) {
            try {
                source.clear()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.w { "Không xóa được một tệp tùy chọn: ${e::class.simpleName}" }
            }
        }
    }
}
