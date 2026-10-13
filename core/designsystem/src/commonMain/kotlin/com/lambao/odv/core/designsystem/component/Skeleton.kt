package com.lambao.odv.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.lambao.odv.core.designsystem.theme.ODVSize
import com.lambao.odv.core.designsystem.theme.ODVTheme
import kotlinx.coroutines.delay

// Chờ chừng này mili giây rồi mới hiện khung chờ: tải xong trước đó thì đi thẳng sang nội dung, không chớp khung (mục 4.2, Skeleton).
private const val SKELETON_DELAY_MS = 200L

// Một vòng quét của vệt sáng.
private const val SHIMMER_PERIOD_MS = 1_200

// Vệt sáng rộng bằng nửa chiều rộng khối chứa; độ đậm 6% màu `ink` (sáng lên ở giao diện tối, sẫm đi ở giao diện sáng).
private const val SHIMMER_BAND_FRACTION = 0.5f
private const val SHIMMER_ALPHA = 0.06f

/**
 * `true` khi [active] đã kéo dài quá [delayMs]. Dùng để chỉ hiện khung chờ ([ODVListSkeleton]...) cho lần tải lâu thật; tải nhanh thì
 * [active] về `false` trước khi hết hạn nên không hiện gì (tránh chớp).
 */
@Composable
fun rememberODVDelayed(active: Boolean, delayMs: Long = SKELETON_DELAY_MS): Boolean {
    var elapsed by remember { mutableStateOf(false) }
    LaunchedEffect(active) {
        if (active) {
            delay(delayMs)
            elapsed = true
        } else {
            elapsed = false
        }
    }
    return active && elapsed
}

/**
 * Vệt sáng quét ngang qua mọi phần đã vẽ của khối (Skeleton, mục 4.2). Đặt một lần ở khối chứa để các ô quét cùng nhịp; vệt chỉ phủ lên
 * chỗ đã có nội dung (các ô), không phủ khe giữa chúng. Bật Giảm hiệu ứng thì đứng yên, không quét.
 */
@Composable
fun Modifier.odvShimmer(): Modifier {
    if (ODVTheme.motion.reduceMotion) return this
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = SHIMMER_PERIOD_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerProgress",
    )
    val highlight = ODVTheme.colors.ink.copy(alpha = SHIMMER_ALPHA)
    return this
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val band = size.width * SHIMMER_BAND_FRACTION
            val start = -band + (size.width + band) * progress
            drawRect(
                brush = Brush.horizontalGradient(listOf(Color.Transparent, highlight, Color.Transparent), startX = start, endX = start + band),
                blendMode = BlendMode.SrcAtop,
            )
        }
}

/** Một khối đặc của khung chờ: nền `surface-2`. Hình dạng và kích thước do [modifier] quyết định. */
@Composable
fun ODVSkeletonBlock(modifier: Modifier = Modifier) {
    Box(modifier.background(ODVTheme.colors.surface2))
}

/**
 * Khung chờ của Danh sách (chế độ danh sách ở tab Thư mục): [rows] hàng giống FileRow (thumbnail 56, hai dòng chữ), cùng nhịp quét.
 * TalkBack chỉ đọc [contentDescription] một lần cho cả khối.
 */
@Composable
fun ODVListSkeleton(contentDescription: String, modifier: Modifier = Modifier, rows: Int = 10) {
    Column(modifier.fillMaxSize().clip(ODVTheme.shapes.sm).odvShimmer().clearAndSetSemantics { this.contentDescription = contentDescription }) {
        repeat(rows) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ODVSkeletonBlock(Modifier.size(56.dp).clip(ODVTheme.shapes.sm))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ODVSkeletonBlock(Modifier.fillMaxWidth(0.6f).height(14.dp).clip(ODVTheme.shapes.sm))
                    ODVSkeletonBlock(Modifier.fillMaxWidth(0.35f).height(12.dp).clip(ODVTheme.shapes.sm))
                }
            }
        }
    }
}

/** Khung chờ của Lưới 2 cột ở tab Thư mục (cùng khe 12 và 16, đệm 16 như lưới thật): ô thumbnail rồi hai dòng chữ. */
@Composable
fun ODVCardGridSkeleton(contentDescription: String, modifier: Modifier = Modifier, rows: Int = 4) {
    Column(
        modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp)
            .clip(ODVTheme.shapes.sm)
            .odvShimmer()
            .clearAndSetSemantics { this.contentDescription = contentDescription },
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(rows) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(2) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ODVSkeletonBlock(Modifier.fillMaxWidth().height(ODVSize.fileCardThumb).clip(ODVTheme.shapes.sm))
                        ODVSkeletonBlock(Modifier.fillMaxWidth(0.7f).height(14.dp).clip(ODVTheme.shapes.sm))
                        ODVSkeletonBlock(Modifier.fillMaxWidth(0.4f).height(12.dp).clip(ODVTheme.shapes.sm))
                    }
                }
            }
        }
    }
}

/** Khung chờ của lưới Thư viện: [columns] cột ô vuông, khe 2dp như lưới thật, một thanh ngắn thay tiêu đề ngày ở đầu. */
@Composable
fun ODVPhotoGridSkeleton(contentDescription: String, modifier: Modifier = Modifier, columns: Int = 4, rows: Int = 12) {
    Column(
        modifier.fillMaxSize().clip(ODVTheme.shapes.sm).odvShimmer().clearAndSetSemantics { this.contentDescription = contentDescription },
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        ODVSkeletonBlock(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp).width(120.dp).height(14.dp).clip(ODVTheme.shapes.sm))
        repeat(rows) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(columns) { ODVPhotoCellPlaceholder(Modifier.weight(1f)) }
            }
        }
    }
}
