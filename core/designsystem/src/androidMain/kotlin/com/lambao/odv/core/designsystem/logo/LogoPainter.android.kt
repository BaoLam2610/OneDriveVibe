package com.lambao.odv.core.designsystem.logo

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.lambao.odv.core.designsystem.R

@Composable
internal actual fun rememberLogoPainter(variant: ODVLogoVariant): Painter = painterResource(
    when (variant) {
        ODVLogoVariant.Color -> R.drawable.logo_mark
        ODVLogoVariant.Ink -> R.drawable.logo_mark_ink
        ODVLogoVariant.White -> R.drawable.logo_mark_white
    },
)
