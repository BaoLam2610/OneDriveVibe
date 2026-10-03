package com.lambao.odv.core.domain.repository

import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.domain.model.LockState
import com.lambao.odv.core.domain.model.SecurityMode
import com.lambao.odv.core.domain.model.UnlockResult
import kotlinx.coroutines.flow.StateFlow

/**
 * Bảo vệ bằng mã PIN: chế độ, khóa/mở khóa, đếm lần sai (ADR-0008, ADR-0014; BM, KH, CH-01 → CH-03, CH-07).
 *
 * Mọi tham số `pin` là [CharArray] do **người gọi sở hữu và tự xóa** sau khi dùng; repository không giữ lại và không
 * xóa hộ. Không ném ngoại lệ (trừ hủy coroutine).
 */
interface SecurityRepository {

    /** Trạng thái khóa hiện tại. Cổng điều hướng quan sát luồng này để không vẽ nội dung khi đang khóa. */
    val lockState: StateFlow<LockState>

    /**
     * Bảo mật đang BẬT (config ở chế độ PIN), kể cả khi đã mở khóa. UI dùng để ẩn nội dung ở danh sách app gần đây ngay từ
     * lúc bật PIN (CH-05, ADR-0014); ảnh chụp ở danh sách này được lấy trước khi app kịp khóa.
     */
    val isProtected: StateFlow<Boolean>

    /**
     * Gọi một lần khi khởi động app: đọc chế độ từ tệp config rồi đặt [lockState] (PIN thì Locked, còn lại Unlocked).
     * Gọi lại thì bỏ qua.
     */
    suspend fun initialize()

    /** Chế độ hiện tại; [SecurityMode.DEVICE] nếu chưa có config hoặc không đọc được. */
    suspend fun mode(): SecurityMode

    /**
     * Bật bảo vệ (BM-04, CD-02): mã hóa lại config đang ở chế độ thiết bị bằng khóa dẫn xuất từ [pin] (ghi tệp tạm rồi
     * thay). Lỗi giữa chừng thì config giữ nguyên ở chế độ thiết bị. Thành công thì app ở trạng thái đã mở khóa.
     */
    suspend fun enableProtection(pin: CharArray): AppResult<Unit>

    /** Mở khóa bằng PIN (KH-01). Tính vào bộ đếm sai; xem [UnlockResult]. */
    suspend fun unlockWithPin(pin: CharArray): UnlockResult

    /** Thời gian còn bị khóa nhập (KH-02), 0 nếu không. Màn Khóa dùng để hiện đồng hồ khi mở lại sau khi tắt app. */
    suspend fun lockoutRemainingMs(): Long

    /**
     * KH-06: số lần sai nữa thì dữ liệu bị xóa (CD-08), `null` nếu tùy chọn đang tắt. Màn Khóa chỉ hiện cảnh báo khi số
     * này đủ nhỏ. Tách khỏi [UnlockResult.WrongPin] vì từ lần sai thứ 5 kết quả luôn là [UnlockResult.Cooldown].
     */
    suspend fun attemptsBeforeWipe(): Int?

    /**
     * Khóa app (CH-03): xóa config đã giải mã, khóa phiên và token khỏi bộ nhớ, đặt [LockState.Locked]. Chỉ có tác dụng
     * ở chế độ PIN. Không suspend để gọi được ngay lúc app xuống nền.
     */
    fun lock()

    /** Kiểm tra PIN hiện tại (CD-03, CD-04, CD-09) mà không đổi trạng thái khóa. Dùng chung bộ đếm sai. */
    suspend fun verifyPin(pin: CharArray): UnlockResult

    /** Đổi PIN (CD-09): kiểm tra [oldPin] rồi mã hóa lại bằng [newPin]. Dùng chung bộ đếm sai. */
    suspend fun changePin(oldPin: CharArray, newPin: CharArray): UnlockResult

    /** Tắt bảo vệ (CD-03): kiểm tra [pin] rồi mã hóa lại config ở chế độ thiết bị. Dùng chung bộ đếm sai. */
    suspend fun disableProtection(pin: CharArray): UnlockResult

    /**
     * Xóa sạch phần bảo mật và config: tệp bí mật, khóa Keystore, bộ đếm sai, bản trong bộ nhớ, token (Quên PIN KH-03,
     * CD-05, KH-06). Thường gọi qua `DisconnectUseCase` để các phần dữ liệu khác cũng được xóa.
     */
    suspend fun wipe()
}
