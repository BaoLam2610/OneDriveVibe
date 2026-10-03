package com.lambao.odv.feature.imageviewer

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.lambao.odv.core.designsystem.component.ODVInfoRow
import com.lambao.odv.core.designsystem.format.odvFormatFileSize
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Ngày theo vi-VN (CLAUDE.md: số và ngày theo vi-VN), như các màn khác. */
private val ViVn: Locale = Locale.forLanguageTag("vi-VN")

/**
 * Các dòng của bảng thông tin ảnh (AN-05): tên tệp, kích thước ảnh, dung lượng, ngày chụp, thiết bị chụp, đường dẫn thư mục.
 * Trường nào không có dữ liệu thì ẩn dòng. Số liệu dùng JetBrains Mono (thiet-ke-ui.md mục 4.5).
 */
@Composable
internal fun ImageInfoContent(info: ImageViewerInfo, modifier: Modifier = Modifier) {
    val item = info.item
    val details = info.details
    val dateFormat = remember {
        SimpleDateFormat(DateFormat.getBestDateTimePattern(ViVn, "yMMMMdHm"), ViVn)
    }
    val takenAt = item.takenAt ?: details?.takenAt
    val root = stringResource(R.string.image_info_root)
    val width = details?.width
    val height = details?.height

    Column(modifier.verticalScroll(rememberScrollState())) {
        ODVInfoRow(stringResource(R.string.image_info_name), item.name)
        if (width != null && height != null) {
            ODVInfoRow(stringResource(R.string.image_info_dimensions), "$width × $height", mono = true)
        }
        ODVInfoRow(stringResource(R.string.image_info_size), odvFormatFileSize(item.sizeBytes), mono = true)
        if (takenAt != null) {
            ODVInfoRow(stringResource(R.string.image_info_taken), dateFormat.format(Date(takenAt)), mono = true)
        }
        val camera = details?.camera
        if (camera != null) ODVInfoRow(stringResource(R.string.image_info_camera), camera)
        ODVInfoRow(
            stringResource(R.string.image_info_folder),
            (listOf(root) + info.folderPath.map { it.name }).joinToString(" / "),
        )
    }
}
