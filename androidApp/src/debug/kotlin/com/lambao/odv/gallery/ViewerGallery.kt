package com.lambao.odv.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.component.ODVButton
import com.lambao.odv.core.designsystem.component.ODVButtonSize
import com.lambao.odv.core.designsystem.component.ODVButtonStyle
import com.lambao.odv.core.designsystem.component.ODVViewerBufferSpinner
import com.lambao.odv.core.designsystem.component.ODVViewerCenterControls
import com.lambao.odv.core.designsystem.component.ODVViewerDoubleTapRipple
import com.lambao.odv.core.designsystem.component.ODVViewerDownloadProgress
import com.lambao.odv.core.designsystem.component.ODVViewerHud
import com.lambao.odv.core.designsystem.component.ODVIconButton
import com.lambao.odv.core.designsystem.component.ODVInfoRow
import com.lambao.odv.core.designsystem.component.ODVViewerLockHoldButton
import com.lambao.odv.core.designsystem.component.ODVMediaDefaults
import com.lambao.odv.core.designsystem.component.ODVViewerError
import com.lambao.odv.core.designsystem.component.ODVViewerMiniProgress
import com.lambao.odv.core.designsystem.component.ODVViewerNetworkNotice
import com.lambao.odv.core.designsystem.component.ODVViewerNextUpCard
import com.lambao.odv.core.designsystem.component.ODVViewerPill
import com.lambao.odv.core.designsystem.component.ODVPlayState
import com.lambao.odv.core.designsystem.component.ODVPlayerLabels
import com.lambao.odv.core.designsystem.component.ODVViewerResetZoomButton
import com.lambao.odv.core.designsystem.component.ODVViewerSeekBar
import com.lambao.odv.core.designsystem.component.ODVSidePanel
import com.lambao.odv.core.designsystem.component.ODVViewerSpeedButton
import com.lambao.odv.core.designsystem.component.ODVViewerBottomBar
import com.lambao.odv.core.designsystem.component.ODVViewerButton
import com.lambao.odv.core.designsystem.component.ODVViewerTopBar
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.designsystem.theme.ODVMediaColors
import com.lambao.odv.core.designsystem.theme.ODVTheme

private val PlayerLabels = ODVPlayerLabels(
    previous = "Video trước", next = "Video kế tiếp", rewind = "Tua lùi 10 giây", forward = "Tua tới 10 giây",
    play = "Phát", pause = "Tạm dừng", replay = "Phát lại", loading = "Đang tải video",
)

/** Khung nền đen mô phỏng màn xem (luôn đen, không đổi theo theme). */
@Composable
private fun MediaFrame(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.clip(ODVTheme.shapes.md).background(ODVMediaColors.background)) { content() }
}

/** Board "17 Components · Trình xem". Chuỗi để trực tiếp vì chỉ dùng khi phát triển. */
@OptIn(ExperimentalLayoutApi::class)
internal fun LazyListScope.viewerPage() {
    item { SectionTitle("ViewerTopBar") }
    item {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MediaFrame(Modifier.fillMaxWidth()) {
                ODVViewerTopBar("phim-tai-lieu-01.mp4", "Quay lại", {}) {
                    ODVIconButton(ODVIcon.Info, "Thông tin tệp", {}, colors = ODVMediaDefaults.iconButtonColors())
                    ODVIconButton(ODVIcon.Lock, "Khóa thao tác", {}, colors = ODVMediaDefaults.iconButtonColors())
                }
            }
            MediaFrame(Modifier.fillMaxWidth()) {
                ODVViewerTopBar("IMG_2041.HEIC", "Quay lại", {}, withGradient = true) {
                    ODVIconButton(ODVIcon.Info, "Thông tin ảnh", {}, colors = ODVMediaDefaults.iconButtonColors())
                }
            }
            MediaFrame(Modifier.fillMaxWidth()) {
                ODVViewerTopBar("truyen-tap-07.pdf", "Quay lại", {}) {
                    ODVIconButton(ODVIcon.Tune, "Kiểu đọc", {}, colors = ODVMediaDefaults.iconButtonColors())
                }
            }
        }
    }

    item { SectionTitle("SeekBar · ViewerBottomBar · MiniProgress (kéo thử)") }
    item {
        var position by remember { mutableFloatStateOf(0.14f) }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MediaFrame(Modifier.fillMaxWidth()) {
                ODVViewerBottomBar(
                    seekBar = {
                        ODVViewerSeekBar(position, 0.38f, { position = it }, "Vị trí phát", "Đã xem ${(position * 100).toInt()}%")
                    },
                    timeText = "12:04 / 1:26:02",
                    modifier = Modifier.padding(vertical = 8.dp),
                ) {
                    ODVViewerSpeedButton("1x", "Tốc độ phát 1x", {})
                    ODVIconButton(ODVIcon.Playlist, "Chế độ phát: Tự phát tiếp", {}, colors = ODVMediaDefaults.iconButtonColors())
                    ODVIconButton(ODVIcon.Fit, "Khung hình: Vừa khung", {}, colors = ODVMediaDefaults.iconButtonColors())
                    ODVIconButton(ODVIcon.Rotate, "Xoay màn hình", {}, colors = ODVMediaDefaults.iconButtonColors())
                }
            }
            MediaFrame(Modifier.fillMaxWidth()) { ODVViewerMiniProgress(position, 0.38f) }
        }
    }

    item { SectionTitle("Cụm điều khiển giữa (chạm nút giữa để đổi trạng thái)") }
    item {
        var state by rememberSaveable { mutableStateOf(1) }
        var atEnd by rememberSaveable { mutableStateOf(false) }
        MediaFrame(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ODVViewerCenterControls(
                    state = ODVPlayState.entries[state], labels = PlayerLabels,
                    onPlayPause = { state = (state + 1) % ODVPlayState.entries.size },
                    onPrevious = {}, onNext = {}, onRewind = {}, onForward = {},
                    hasNext = !atEnd,
                )
                Text("Trạng thái: ${ODVPlayState.entries[state].name}", color = ODVMediaColors.onMediaMuted, style = ODVTheme.typography.meta, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
        ODVButton("Video cuối danh sách: ${if (atEnd) "bật" else "tắt"}", { atEnd = !atEnd }, style = ODVButtonStyle.Tonal, size = ODVButtonSize.Sm, modifier = Modifier.padding(top = 8.dp))
    }

    item { SectionTitle("HUD · gợn chạm đúp · khóa thao tác") }
    item {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MediaFrame(Modifier.size(160.dp, 280.dp)) { ODVViewerHud(0.4f, ODVIcon.Sun, "40%", "Độ sáng 40%", Modifier.align(Alignment.CenterStart).padding(start = 16.dp)) }
            MediaFrame(Modifier.size(160.dp, 280.dp)) { ODVViewerHud(0.65f, ODVIcon.Volume, "65%", "Âm lượng 65%", Modifier.align(Alignment.CenterEnd).padding(end = 16.dp)) }
            MediaFrame(Modifier.size(300.dp, 200.dp)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(0.5f).align(Alignment.CenterEnd)) { ODVViewerDoubleTapRipple("+20 giây", onRightSide = true) }
            }
            MediaFrame(Modifier.size(260.dp, 220.dp)) {
                ODVViewerLockHoldButton("Giữ để mở khóa thao tác", "Giữ để mở khóa", {}, Modifier.align(Alignment.Center), subHint = "Mọi thao tác chạm khác đang bị chặn")
            }
        }
    }

    item { SectionTitle("Pill · ResetZoom · BufferSpinner · DownloadProgress") }
    item {
        MediaFrame(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ODVViewerPill("Cắt đầy", prominent = true, live = true)
                ODVViewerPill("2,4x", mono = true)
                ODVViewerPill("72 / 310", mono = true)
                ODVViewerPill("Đang tải ảnh gốc", icon = ODVIcon.Sync)
                ODVViewerResetZoomButton("Đặt lại zoom", {})
                ODVViewerBufferSpinner("Đang tải video…")
                ODVViewerDownloadProgress(0.45f, "45%", "12,4 / 27,6 MB")
            }
        }
    }

    item { SectionTitle("MediaError · NetworkNotice · NextUpCard") }
    item {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MediaFrame(Modifier.fillMaxWidth()) {
                ODVViewerError(
                    "Thiết bị không hỗ trợ phát định dạng này", body = "Máy chưa có bộ giải mã cho tệp này.", fileLine = "ban-ghi.avi · AVI",
                    action = { ODVViewerButton("Quay lại", {}) },
                )
            }
            MediaFrame(Modifier.fillMaxWidth()) {
                Box(Modifier.padding(16.dp)) {
                    ODVViewerNetworkNotice("Mất kết nối mạng", "Đã phát hết phần đã tải trước. Kiểm tra mạng rồi nhấn Tiếp tục.", "Tiếp tục", {}, hint = "Nút bật lại khi có mạng")
                }
            }
            MediaFrame(Modifier.fillMaxWidth()) {
                Box(Modifier.padding(16.dp)) {
                    ODVViewerNextUpCard("Tiếp theo sau 5 giây", "phim-tai-lieu-02.mp4", "3", 0.4f, "Hủy", {})
                }
            }
        }
    }

    item { SectionTitle("Bảng bên phải (hướng ngang)") }
    item {
        MediaFrame(Modifier.fillMaxWidth().height(260.dp)) {
            ODVSidePanel("Thông tin tệp", "Đóng", {}) {
                Column {
                    ODVInfoRow("Tên tệp", "phim-tai-lieu-01.mp4")
                    ODVInfoRow("Thời lượng", "1:26:02", mono = true)
                    ODVInfoRow("Dung lượng", "974 MB")
                }
            }
        }
    }
}
