@file:androidx.annotation.OptIn(UnstableApi::class)

package com.lambao.odv.feature.player

import android.content.Context
import android.hardware.display.DisplayManager
import android.media.MediaCodecInfo.CodecProfileLevel
import android.os.Build
import android.view.Display
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import java.util.concurrent.atomic.AtomicReference

/**
 * Chọn bộ giải mã video cho ExoPlayer và nhớ những bộ đã chết giữa chừng (bug CRITICAL Dolby Vision trên MediaTek).
 *
 * Media3 luôn dùng bộ giải mã đứng đầu danh sách. Ở máy MediaTek, video Dolby Vision 8.4 bị chuyển sang `c2.mtk.hevc.decoder` (màn hình
 * không có Dolby Vision), bộ này tự báo không đủ năng lực rồi chết ngay khi nhận dữ liệu; không có cách nào để Media3 tự thử bộ khác
 * vì nó chỉ chuyển bộ khi *khởi tạo* lỗi (`setEnableDecoderFallback`), không phải khi *đang giải mã* lỗi. Nên [VideoPlayerController]
 * chặn bộ vừa chết bằng [block] rồi `prepare()` lại: lần chọn kế tiếp bỏ nó đi và Media3 lấy bộ tiếp theo (thường là bộ phần mềm).
 * Danh sách chặn chỉ áp cho video đang phát, [reset] khi nạp video khác để các video HEVC thường vẫn dùng bộ phần cứng.
 *
 * Bộ chọn được gọi từ luồng phát của ExoPlayer, còn [block] và [reset] từ luồng chính, nên danh sách đọc/ghi qua [AtomicReference].
 */
internal class VideoDecoders(private val context: Context) {
    private val blocked = AtomicReference<Set<String>>(emptySet())

    val selector = MediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
        MediaCodecSelector.DEFAULT.getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunnelingDecoder)
            .filter { it.name !in blocked.get() }
    }

    /** Chặn [codecName]; false nếu nó đã bị chặn từ trước (thử lại cũng không đổi gì nên không nên thử tiếp). */
    fun block(codecName: String): Boolean {
        while (true) {
            val current = blocked.get()
            if (codecName in current) return false
            if (blocked.compareAndSet(current, current + codecName)) return true
        }
    }

    fun reset() {
        blocked.set(emptySet())
    }

    /**
     * Còn bộ giải mã nào khác [codecName], chưa bị chặn, cho [format] không. Không còn thì đừng chặn [codecName]: chặn bộ cuối cùng làm
     * Media3 bỏ luôn track video và phát mỗi tiếng (đã gặp ở log 2026-10-06), tệ hơn là báo lỗi.
     */
    fun hasAlternative(format: Format?, codecName: String): Boolean = try {
        candidateMimes(format).any { mime ->
            MediaCodecUtil.getDecoderInfos(mime, false, false).any { it.name != codecName && it.name !in blocked.get() }
        }
    } catch (e: Exception) {
        false
    }

    /** Dolby Vision profile 8 không có bộ giải mã riêng thì Media3 dùng bộ HEVC của lớp nền, nên cả hai loại đều là ứng viên. */
    private fun candidateMimes(format: Format?): List<String> {
        val mime = format?.sampleMimeType ?: return emptyList()
        return if (mime == MimeTypes.VIDEO_DOLBY_VISION) listOf(mime, MimeTypes.VIDEO_H265) else listOf(mime)
    }

    /**
     * Mô tả các bộ giải mã máy có cho [format] và loại HDR của màn hình, để log lỗi giải mã (B0): biết bộ nào đã dùng, bộ nào còn lại
     * và bộ nào tự báo hỗ trợ định dạng. Chỉ ghi tên bộ và cờ, không có dữ liệu người dùng (CH-06). Lỗi khi truy vấn chỉ ghi tên lớp.
     */
    fun describe(format: Format?): String = try {
        val decoders = candidateMimes(format).joinToString("; ") { mime ->
            val infos = MediaCodecUtil.getDecoderInfos(mime, false, false)
            val names = infos.joinToString(", ") { info ->
                val kind = if (info.hardwareAccelerated) "phần cứng" else "phần mềm"
                // Media3 1.10+ nhận thêm Context (kiểm tra theo màn hình/thiết bị); bản 1.8 chỉ có tham số Format.
                val supported = format?.let { info.isFormatSupported(context, it) }
                val flag = if (info.name in blocked.get()) " ĐÃ CHẶN" else ""
                "${info.name}($kind, hỗ trợ định dạng=$supported, profile=${profilesOf(info)}$flag)"
            }
            "$mime: ${names.ifEmpty { "không có" }}"
        }
        "bộ giải mã [$decoders] HDR màn hình=${displayHdrTypes()}"
    } catch (e: Exception) {
        "không đọc được danh sách bộ giải mã (${e.javaClass.simpleName})"
    }

    /**
     * Các profile HEVC mà bộ giải mã tự khai báo. Quyết định việc máy có giải mã được video 10-bit (Main10) hay không: nếu chỉ có `Main`
     * thì lỗi không nằm ở Surface hay Dolby Vision mà ở chỗ máy không có bộ giải mã 10-bit nào.
     */
    private fun profilesOf(info: MediaCodecInfo): String {
        val levels = info.capabilities?.profileLevels ?: return "không rõ"
        return levels.map { it.profile }.distinct().joinToString("/") { profile ->
            when (profile) {
                CodecProfileLevel.HEVCProfileMain -> "Main"
                CodecProfileLevel.HEVCProfileMain10 -> "Main10"
                CodecProfileLevel.HEVCProfileMain10HDR10 -> "Main10HDR10"
                CodecProfileLevel.HEVCProfileMain10HDR10Plus -> "Main10HDR10+"
                else -> "profile$profile"
            }
        }.ifEmpty { "không có" }
    }

    /** Giống điều kiện Media3 dùng để quyết định có tìm decoder Dolby Vision hay lùi về HEVC (`MediaCodecVideoRenderer.getDecoderInfos`). */
    fun displaySupportsDolbyVision(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val manager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        val display = manager?.getDisplay(Display.DEFAULT_DISPLAY) ?: return false
        if (!display.isHdr) return false
        return display.hdrCapabilities?.supportedHdrTypes?.contains(Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION) == true
    }

    private fun displayHdrTypes(): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return "không rõ (Android dưới 8)"
        val manager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        val display = manager?.getDisplay(Display.DEFAULT_DISPLAY) ?: return "không rõ"
        if (!display.isHdr) return "không có"
        return display.hdrCapabilities?.supportedHdrTypes?.joinToString(",") { type ->
            when (type) {
                Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION -> "DolbyVision"
                Display.HdrCapabilities.HDR_TYPE_HDR10 -> "HDR10"
                Display.HdrCapabilities.HDR_TYPE_HLG -> "HLG"
                Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS -> "HDR10+"
                else -> "loại$type"
            }
        } ?: "không rõ"
    }
}
