package com.lambao.odv.feature.pdfviewer

import android.content.res.Configuration
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lambao.odv.core.designsystem.component.ODVBottomSheet
import com.lambao.odv.core.designsystem.component.ODVButton
import com.lambao.odv.core.designsystem.component.ODVButtonStyle
import com.lambao.odv.core.designsystem.component.ODVDialog
import com.lambao.odv.core.designsystem.component.ODVIconButton
import com.lambao.odv.core.designsystem.component.ODVInfoRow
import com.lambao.odv.core.domain.usecase.account.ObserveItemAccountUseCase
import org.koin.compose.koinInject
import com.lambao.odv.core.designsystem.component.ODVMediaDefaults
import com.lambao.odv.core.designsystem.component.ODVOptionRow
import com.lambao.odv.core.designsystem.component.ODVProgressBar
import com.lambao.odv.core.designsystem.component.ODVProgressColors
import com.lambao.odv.core.designsystem.component.ODVSidePanel
import com.lambao.odv.core.designsystem.component.ODVTextField
import com.lambao.odv.core.designsystem.component.ODVViewerPill
import com.lambao.odv.core.designsystem.component.ODVViewerTopBar
import com.lambao.odv.core.designsystem.format.odvFormatFileSize
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.odvLocale
import com.lambao.odv.core.designsystem.theme.ODVDuration
import com.lambao.odv.core.designsystem.theme.ODVMediaColors
import com.lambao.odv.core.designsystem.theme.ODVTheme
import com.lambao.odv.core.domain.model.PdfFile
import com.lambao.odv.core.domain.model.PdfReadingStyle
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Lớp điều khiển phủ lên trang (PD-04, PD-06): thanh trên (quay lại, tên tệp, kiểu đọc, thông tin) và thanh dưới (nút số trang + thanh tiến độ)
 * nền `media.toolbar`; khi ẩn thì còn viên thuốc số trang ở góc dưới phải. Có thêm thông báo "Đang ở trang X" khi mở lại tệp (PD-05).
 */
@Composable
internal fun BoxScope.PdfControls(
    state: PdfViewerState,
    file: PdfFile,
    onIntent: (PdfViewerIntent) -> Unit,
    onBack: () -> Unit,
) {
    val fade = tween<Float>(ODVDuration.fast)
    val count = state.pageCount
    val current = state.currentPage + 1

    AnimatedVisibility(
        visible = state.controlsVisible,
        modifier = Modifier.align(Alignment.TopCenter),
        enter = fadeIn(fade),
        exit = fadeOut(fade),
    ) {
        // Nền phủ cả vùng status bar để icon hệ thống đọc được; thanh nằm dưới status bar.
        Box(Modifier.fillMaxWidth().background(ODVMediaColors.toolbar).statusBarsPadding()) {
            ODVViewerTopBar(
                title = file.item.name,
                backContentDescription = stringResource(R.string.pdf_back),
                onBack = onBack,
                actions = {
                    ODVIconButton(
                        ODVIcon.Tune,
                        stringResource(R.string.pdf_style),
                        { onIntent(PdfViewerIntent.ShowStyle) },
                        colors = ODVMediaDefaults.iconButtonColors(),
                    )
                    ODVIconButton(
                        ODVIcon.Info,
                        stringResource(R.string.pdf_info),
                        { onIntent(PdfViewerIntent.ShowInfo) },
                        colors = ODVMediaDefaults.iconButtonColors(),
                    )
                },
            )
        }
    }
    AnimatedVisibility(
        visible = state.controlsVisible,
        modifier = Modifier.align(Alignment.BottomCenter),
        enter = fadeIn(fade),
        exit = fadeOut(fade),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(ODVMediaColors.toolbar)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PageButton(current, count) { onIntent(PdfViewerIntent.ShowJump) }
            ODVProgressBar(
                progress = current.toFloat() / count,
                height = 4.dp,
                colors = ODVProgressColors(track = ODVMediaColors.track, indicator = ODVMediaColors.accent),
            )
        }
    }
    AnimatedVisibility(
        visible = !state.controlsVisible,
        modifier = Modifier.align(Alignment.BottomEnd),
        enter = fadeIn(fade),
        exit = fadeOut(fade),
    ) {
        ODVViewerPill(
            text = stringResource(R.string.pdf_page_counter, current, count),
            mono = true,
            modifier = Modifier.navigationBarsPadding().padding(16.dp),
        )
    }

    state.resumeNotice?.let { page ->
        LaunchedEffect(page) {
            delay(PdfViewerConstants.NOTICE_MS)
            onIntent(PdfViewerIntent.NoticeShown)
        }
        ODVViewerPill(
            text = stringResource(R.string.pdf_resume_notice, page),
            live = true,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 112.dp),
        )
    }
}

/** Nút số trang "72 / 310" (PD-04): viền `media.buffer` cao 40 nằm trong vùng chạm cao 48; chạm để nhập số trang cần nhảy tới. */
@Composable
private fun PageButton(current: Int, count: Int, onClick: () -> Unit) {
    val description = stringResource(R.string.pdf_page_button_description, current, count)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Box(
            Modifier
                .heightIn(min = 48.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
                .semantics { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .height(40.dp)
                    .border(1.dp, ODVMediaColors.buffer, ODVTheme.shapes.full)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.pdf_page_counter, current, count),
                    style = ODVTheme.typography.timecode,
                    color = ODVMediaColors.onMedia,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Các lớp phủ mở theo [PdfViewerState.overlay] và [PdfViewerState.info]; không có thì không vẽ gì. */
@Composable
internal fun PdfDialogs(state: PdfViewerState, onIntent: (PdfViewerIntent) -> Unit) {
    when (state.overlay) {
        PdfOverlay.Jump -> PdfJumpDialog(
            pageCount = state.pageCount,
            currentPage = state.currentPage + 1,
            onJump = { onIntent(PdfViewerIntent.JumpTo(it)) },
            onDismiss = { onIntent(PdfViewerIntent.DismissOverlay) },
        )
        PdfOverlay.Style -> PdfStyleSheet(
            selected = state.readingStyle,
            onSelect = { onIntent(PdfViewerIntent.SelectStyle(it)) },
            onDismiss = { onIntent(PdfViewerIntent.DismissOverlay) },
        )
        null -> Unit
    }
    state.info?.let { info ->
        val title = stringResource(R.string.pdf_info_title)
        val close = { onIntent(PdfViewerIntent.HideInfo) }
        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Hướng ngang: bảng bên phải rộng 360 thay cho bottom sheet (thiet-ke-ui.md mục 4.5).
            ODVSidePanel(title, stringResource(R.string.pdf_info_close), close) { PdfInfoContent(info) }
        } else {
            ODVBottomSheet(onDismissRequest = close, title = title) { PdfInfoContent(info) }
        }
    }
}

/**
 * Hộp thoại nhảy tới trang (PD-04, P5): ô nhập số với bàn phím số của hệ thống, hợp lệ khi nằm trong 1..[pageCount]. Số đang gõ nằm trong
 * `rememberSaveable` nên xoay màn hình không mất.
 */
@Composable
private fun PdfJumpDialog(pageCount: Int, currentPage: Int, onJump: (Int) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val number = text.toIntOrNull()
    val valid = number != null && number in 1..pageCount
    ODVDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.pdf_jump_title),
        extra = {
            ODVTextField(
                value = text,
                onValueChange = { input -> text = input.filter(Char::isDigit).take(MAX_PAGE_DIGITS) },
                label = stringResource(R.string.pdf_jump_label),
                placeholder = currentPage.toString(),
                helperText = stringResource(R.string.pdf_jump_hint, pageCount),
                errorText = if (text.isNotEmpty() && !valid) stringResource(R.string.pdf_jump_error, pageCount) else null,
                mono = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
    ) {
        ODVButton(stringResource(R.string.pdf_cancel), onDismiss, style = ODVButtonStyle.Ghost)
        ODVButton(stringResource(R.string.pdf_jump_go), { number?.let(onJump) }, enabled = valid)
    }
}

/** Bảng chọn kiểu đọc (nút `tune` ở thanh trên): chạm là đặt giá trị và đóng ngay, cùng cài đặt với mục Kiểu đọc trong Cài đặt. */
@Composable
private fun PdfStyleSheet(selected: PdfReadingStyle, onSelect: (PdfReadingStyle) -> Unit, onDismiss: () -> Unit) {
    ODVBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.pdf_style_title)) {
        PdfReadingStyle.entries.forEach { style ->
            ODVOptionRow(
                label = stringResource(
                    when (style) {
                        PdfReadingStyle.Vertical -> R.string.pdf_style_vertical
                        PdfReadingStyle.Horizontal -> R.string.pdf_style_horizontal
                    },
                ),
                selected = style == selected,
                onClick = { onSelect(style) },
            )
        }
    }
}

/**
 * Các dòng của bảng thông tin PDF (PD-08): tên tệp, dung lượng, số trang (chỉ khi tệp đã mở được), ngày tải lên, ngày sửa gần nhất, thư mục
 * chứa. Trường nào không có dữ liệu thì ẩn dòng. Số liệu dùng JetBrains Mono (thiet-ke-ui.md mục 4.5).
 */
@Composable
private fun PdfInfoContent(info: PdfViewerInfo, modifier: Modifier = Modifier) {
    val item = info.file.item
    val locale = odvLocale()
    val dateFormat = remember(locale) { SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "yMMMMdHm"), locale) }
    val root = stringResource(R.string.pdf_info_root)
    Column(modifier.verticalScroll(rememberScrollState())) {
        ODVInfoRow(stringResource(R.string.pdf_info_name), item.name)
        ODVInfoRow(stringResource(R.string.pdf_info_size), odvFormatFileSize(item.sizeBytes, locale.toLanguageTag()), mono = true)
        if (info.pageCount > 0) ODVInfoRow(stringResource(R.string.pdf_info_pages), info.pageCount.toString(), mono = true)
        item.createdAt?.let { ODVInfoRow(stringResource(R.string.pdf_info_uploaded), dateFormat.format(Date(it)), mono = true) }
        item.modifiedAt?.let { ODVInfoRow(stringResource(R.string.pdf_info_modified), dateFormat.format(Date(it)), mono = true) }
        ODVInfoRow(
            stringResource(R.string.pdf_info_folder),
            (listOf(root) + info.folderPath.map { it.name }).joinToString(" / "),
        )
        // Tài khoản chứa tệp: chỉ khi app có từ 2 tài khoản (TA-03, mục 4.5).
        val observeAccount = koinInject<ObserveItemAccountUseCase>()
        val account by remember(item.id) { observeAccount(item.id) }.collectAsStateWithLifecycle(initialValue = null)
        account?.let { ODVInfoRow(stringResource(R.string.pdf_info_account), "${it.displayName}\n${it.email}") }
    }
}

/** PDF dài nhất vẫn dưới 7 chữ số trang; giới hạn để ô nhập không nhận chuỗi số vô nghĩa. */
private const val MAX_PAGE_DIGITS = 7
