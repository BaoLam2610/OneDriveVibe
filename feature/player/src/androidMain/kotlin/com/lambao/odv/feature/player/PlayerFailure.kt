@file:androidx.annotation.OptIn(UnstableApi::class)

package com.lambao.odv.feature.player

import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlaybackException
import com.lambao.odv.core.common.error.AppError
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Vì sao video không phát được, để giao diện chọn đúng thẻ lỗi thay vì để màn hình đen (VD-15, VD-16). */
internal enum class PlayerFailure {
    /** Định dạng hoặc codec máy không giải mã được (VD-15). */
    Unsupported,

    /** Mất mạng hoặc không tới được máy chủ (VD-16). Có mạng lại thì tiếp tục được. */
    Network,

    /** Video đã bị xóa trên OneDrive (404/410), phát hiện khi lấy link hoặc tải. */
    Removed,

    /** Lỗi khác (máy chủ trả lỗi, link không làm mới được...). Thử lại được. */
    Other,
}

/**
 * Phân loại lỗi của ExoPlayer. Nhìn cả chuỗi nguyên nhân vì ExoPlayer gói lỗi gốc (mạng, HTTP, hay lỗi lấy link của
 * [StreamDataSource]) vào một mã chung như `ERROR_CODE_IO_UNSPECIFIED`.
 */
internal fun PlaybackException.toFailure(): PlayerFailure {
    val chain = causeChain()

    if (chain.any { it is VideoRemovedException }) return PlayerFailure.Removed
    if (chain.any { it is StreamUrlException && (it.error is AppError.Network || it.error is AppError.Timeout) }) {
        return PlayerFailure.Network
    }
    if (chain.any { it is HttpDataSource.InvalidResponseCodeException && it.responseCode in PlayerConstants.REMOVED_STATUSES }) {
        return PlayerFailure.Removed
    }
    return when (errorCode) {
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        -> PlayerFailure.Network

        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> PlayerFailure.Removed

        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
        -> PlayerFailure.Unsupported

        // ExoPlayer không phải lúc nào cũng gán mã mạng cho lỗi socket: soi thêm nguyên nhân.
        else -> if (chain.any { it is UnknownHostException || it is SocketTimeoutException || it is ConnectException }) {
            PlayerFailure.Network
        } else {
            PlayerFailure.Other
        }
    }
}

private fun Throwable.causeChain(): List<Throwable> =
    generateSequence<Throwable>(this) { it.cause }.take(PlayerConstants.CAUSE_DEPTH).toList()

/**
 * Mô tả lỗi cho log: tên mã lỗi của ExoPlayer, rồi chuỗi nguyên nhân dạng `A <- B <- C` (tên lớp, kèm mã HTTP nếu có).
 * Cố ý không ghi message hay stack trace: message của ngoại lệ mạng có thể chứa URL ký (CH-06).
 */
internal fun PlaybackException.describe(): String {
    val chain = causeChain().joinToString(" <- ") { error ->
        val name = error.javaClass.simpleName
        if (error is HttpDataSource.InvalidResponseCodeException) "$name(${error.responseCode})" else name
    }
    // Lỗi từ renderer (giải mã): ghi định dạng video và mức hỗ trợ của máy để biết vì sao không phát được (vd Dolby Vision, HEVC 10-bit).
    val renderer = (this as? ExoPlaybackException)?.takeIf { it.type == ExoPlaybackException.TYPE_RENDERER }?.let { e ->
        val format = e.rendererFormat
        " định dạng=${format?.sampleMimeType}/${format?.codecs} ${format?.width}x${format?.height} " +
            "hỗ trợ=${formatSupportName(e.rendererFormatSupport)}"
    }.orEmpty()
    return "$errorCodeName [$chain]$renderer"
}

/** Media3 không có hàm đổi mã [C.FormatSupport] sang chữ, nên tự ánh xạ để log đọc được. */
private fun formatSupportName(support: Int): String = when (support) {
    C.FORMAT_HANDLED -> "HANDLED"
    C.FORMAT_EXCEEDS_CAPABILITIES -> "EXCEEDS_CAPABILITIES"
    C.FORMAT_UNSUPPORTED_DRM -> "UNSUPPORTED_DRM"
    C.FORMAT_UNSUPPORTED_SUBTYPE -> "UNSUPPORTED_SUBTYPE"
    C.FORMAT_UNSUPPORTED_TYPE -> "UNSUPPORTED_TYPE"
    else -> "?$support"
}
