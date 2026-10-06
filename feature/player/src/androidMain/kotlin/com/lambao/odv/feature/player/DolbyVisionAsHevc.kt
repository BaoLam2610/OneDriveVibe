@file:androidx.annotation.OptIn(UnstableApi::class)

package com.lambao.odv.feature.player

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.DataReader
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorInput
import androidx.media3.extractor.ExtractorOutput
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.PositionHolder
import androidx.media3.extractor.SeekMap
import androidx.media3.extractor.SniffFailure
import androidx.media3.extractor.TrackOutput

/**
 * Coi video Dolby Vision profile 8 là HEVC Main10 ngay ở bước đọc tệp, khi màn hình không có Dolby Vision (bug CRITICAL video HDR iPhone,
 * tiến độ MVP1; ADR-0018).
 *
 * Extractor của Media3 gắn nhãn video này là `video/dolby-vision` (`dvhe.08.xx`). Máy không có decoder Dolby Vision nên Media3 tự lùi về
 * decoder HEVC, nhưng renderer FFmpeg dự phòng (ADR-0018) không nhận mime lạ đó, nên máy thiếu decoder 10-bit (Helio G99) không có đường
 * nào phát được. Profile 8 mang sẵn lớp nền HEVC Main10 tương thích ngược nên đổi nhãn thành `video/hevc` là đúng bản chất; Media3 vốn
 * cũng làm đúng việc đó khi chọn decoder, ở đây chỉ làm sớm hơn để cả hai loại renderer cùng thấy một định dạng HEVC rõ profile.
 *
 * Chỉ profile 8 (lớp nền tương thích): profile 5 không có lớp nền HEVC dùng được (màu sẽ sai) nên giữ nguyên. Màn hình có Dolby Vision thì không
 * bọc gì để vẫn dùng decoder Dolby Vision.
 */
internal class DolbyVisionAsHevcExtractorsFactory(
    private val displaySupportsDolbyVision: Boolean,
    private val delegate: ExtractorsFactory = DefaultExtractorsFactory(),
) : ExtractorsFactory {

    override fun createExtractors(): Array<Extractor> = wrap(delegate.createExtractors())

    override fun createExtractors(uri: Uri, responseHeaders: Map<String, List<String>>): Array<Extractor> =
        wrap(delegate.createExtractors(uri, responseHeaders))

    private fun wrap(extractors: Array<Extractor>): Array<Extractor> =
        if (displaySupportsDolbyVision) extractors else Array(extractors.size) { RewritingExtractor(extractors[it]) }
}

/** Mã codec HEVC Main10 (profile 2, tương thích 4) cấp 5.1, tầng Main: lớp nền của Dolby Vision profile 8 luôn là Main10. */
private const val HEVC_MAIN10_CODECS = "hvc1.2.4.L153.B0"

private fun Format.dolbyVisionProfile8AsHevc(): Format {
    val codecs = codecs ?: return this
    val isProfile8 = sampleMimeType == MimeTypes.VIDEO_DOLBY_VISION && (codecs.startsWith("dvhe.08") || codecs.startsWith("dvh1.08"))
    if (!isProfile8) return this
    playerLog.i { "[Decoder] video Dolby Vision profile 8 ($codecs), màn hình không có Dolby Vision: coi là HEVC Main10" }
    return buildUpon().setSampleMimeType(MimeTypes.VIDEO_H265).setCodecs(HEVC_MAIN10_CODECS).build()
}

private class RewritingExtractor(private val delegate: Extractor) : Extractor {
    override fun sniff(input: ExtractorInput): Boolean = delegate.sniff(input)

    override fun getSniffFailureDetails(): List<SniffFailure> = delegate.sniffFailureDetails

    override fun init(output: ExtractorOutput) = delegate.init(RewritingExtractorOutput(output))

    override fun read(input: ExtractorInput, seekPosition: PositionHolder): Int = delegate.read(input, seekPosition)

    override fun seek(position: Long, timeUs: Long) = delegate.seek(position, timeUs)

    override fun release() = delegate.release()

    override fun getUnderlyingImplementation(): Extractor = delegate.underlyingImplementation
}

private class RewritingExtractorOutput(private val delegate: ExtractorOutput) : ExtractorOutput {
    override fun track(id: Int, type: Int): TrackOutput {
        val track = delegate.track(id, type)
        return if (type == C.TRACK_TYPE_VIDEO) RewritingTrackOutput(track) else track
    }

    override fun endTracks() = delegate.endTracks()

    override fun seekMap(seekMap: SeekMap) = delegate.seekMap(seekMap)
}

private class RewritingTrackOutput(private val delegate: TrackOutput) : TrackOutput {
    override fun format(format: Format) = delegate.format(format.dolbyVisionProfile8AsHevc())

    override fun durationUs(durationUs: Long) = delegate.durationUs(durationUs)

    override fun sampleData(input: DataReader, length: Int, allowEndOfInput: Boolean, sampleDataPart: Int): Int =
        delegate.sampleData(input, length, allowEndOfInput, sampleDataPart)

    override fun sampleData(data: ParsableByteArray, length: Int, sampleDataPart: Int) =
        delegate.sampleData(data, length, sampleDataPart)

    override fun sampleMetadata(timeUs: Long, flags: Int, size: Int, offset: Int, cryptoData: TrackOutput.CryptoData?) =
        delegate.sampleMetadata(timeUs, flags, size, offset, cryptoData)
}
