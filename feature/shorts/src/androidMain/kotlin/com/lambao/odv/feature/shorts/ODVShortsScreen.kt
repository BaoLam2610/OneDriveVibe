package com.lambao.odv.feature.shorts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lambao.odv.core.designsystem.ODVCollectEffects
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

/**
 * Tab Short của Màn chính (mục 3.4.4, SV-01 → SV-16): xem video ngắn vuốt dọc ngay trong tab, thứ tự ngẫu nhiên. Nền luôn đen và video vẽ
 * tràn dưới thanh trạng thái (SV-10), nên màn không đệm inset trên; Màn chính đặt thanh đáy N4 bên dưới. [onOpenSettings] chạy ở trạng thái
 * trống (SH9) để chuyển sang tab Cài đặt tăng "Thời lượng tối đa của Short".
 *
 * Trạng thái khôi phục sau khi hệ điều hành thu hồi tiến trình (seed, chỉ số, vị trí, DH-02) giữ bằng `rememberSaveable` rồi đưa lại cho
 * ViewModel khi vào tab. Player nằm ở [ShortPlayerHolder] (ViewModel của Màn chính) nên rời tab chỉ tạm dừng, không giải phóng (ADR-0024).
 */
@Composable
fun ODVShortsScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    reselectSignal: Int = 0,
    viewModel: ShortsViewModel = koinViewModel(),
) {
    val holder: ShortPlayerHolder = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var savedSeed by rememberSaveable { mutableStateOf(ShortsConstants.NO_SEED) }
    var savedMax by rememberSaveable { mutableStateOf(0) }
    var savedIndex by rememberSaveable { mutableStateOf(0) }
    var savedPosition by rememberSaveable { mutableStateOf(0L) }
    var reshuffledShown by remember { mutableStateOf(false) }

    ODVShortsSystemBars()

    // Mỗi lần tab được dựng lại là một lần "vào tab": ViewModel quyết định giữ nguyên hay dựng/xáo mới (DH-02, SV-02, SV-16).
    LaunchedEffect(Unit) {
        val restore = if (savedSeed != ShortsConstants.NO_SEED) ShortsSnapshot(savedSeed, savedMax, savedIndex, savedPosition) else null
        viewModel.onIntent(ShortsIntent.Enter(restore))
    }
    LaunchedEffect(state.phase, state.seed, state.maxMinutes, state.index) {
        if (state.phase == ShortsPhase.Ready) {
            savedSeed = state.seed
            savedMax = state.maxMinutes
            savedIndex = state.index
        }
    }
    ODVCollectEffects(viewModel.effects) { effect ->
        when (effect) {
            ShortsEffect.Reshuffled -> reshuffledShown = true
        }
    }
    LaunchedEffect(reshuffledShown) {
        if (reshuffledShown) {
            delay(ShortsConstants.LABEL_MS)
            reshuffledShown = false
        }
    }

    ShortsContent(
        state = state,
        controller = holder.controller,
        reshuffledShown = reshuffledShown,
        onIntent = viewModel::onIntent,
        onPositionChanged = { savedPosition = it },
        onPreload = holder::preload,
        reselectSignal = reselectSignal,
        onOpenSettings = onOpenSettings,
        modifier = modifier,
    )
}
