package com.lambao.odv.core.domain.usecase

import com.lambao.odv.core.domain.hook.ConnectionResetter
import com.lambao.odv.core.domain.repository.SecurityRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Ngắt kết nối và xóa dữ liệu (CD-05): Quên PIN (KH-03), nhập sai quá nhiều (KH-06), nút Ngắt kết nối (Lát 9).
 *
 * Thứ tự quan trọng: dữ liệu của các lát (Room, cache, lịch sử, cài đặt) xóa **trước**, rồi mới xóa config và khóa
 * Keystore. Nếu bị dừng giữa chừng thì config còn nên Splash vẫn đòi PIN hoặc cho thử lại, thay vì để lại dữ liệu mồ côi
 * không còn cách nào dọn.
 */
class DisconnectUseCase(
    private val resetters: List<ConnectionResetter>,
    private val security: SecurityRepository,
) {
    // NonCancellable: ViewModel gọi từ màn Khóa có thể bị clear giữa chừng; xóa dở dang (KH-06, CD-05) là không chấp nhận.
    suspend operator fun invoke() = withContext(NonCancellable) {
        for (resetter in resetters) {
            try {
                resetter.reset()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Một phần dữ liệu không xóa được không được chặn việc xóa config và khóa: phần quan trọng nhất.
            }
        }
        security.wipe()
    }
}
