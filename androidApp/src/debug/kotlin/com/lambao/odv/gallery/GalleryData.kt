// Sinh từ odv-tokens.json bởi script. Chỉ phục vụ màn Foundations gallery (bản debug).
package com.lambao.odv.gallery

import androidx.compose.ui.graphics.Color
import com.lambao.odv.core.designsystem.theme.ODVColors
import com.lambao.odv.core.designsystem.theme.ODVMediaColors

internal fun themeColorSwatches(c: ODVColors): List<Pair<String, Color>> = listOf(
        "bg" to c.bg,
        "surface" to c.surface,
        "surface-2" to c.surface2,
        "line" to c.line,
        "line-strong" to c.lineStrong,
        "ink" to c.ink,
        "ink-muted" to c.inkMuted,
        "ink-faint" to c.inkFaint,
        "volt" to c.volt,
        "volt-pressed" to c.voltPressed,
        "on-volt" to c.onVolt,
        "volt-soft" to c.voltSoft,
        "volt-text" to c.voltText,
        "kind-video" to c.kindVideo,
        "kind-video-soft" to c.kindVideoSoft,
        "kind-photo" to c.kindPhoto,
        "kind-photo-soft" to c.kindPhotoSoft,
        "kind-pdf" to c.kindPdf,
        "kind-pdf-soft" to c.kindPdfSoft,
        "danger" to c.danger,
        "danger-soft" to c.dangerSoft,
        "warning" to c.warning,
        "warning-soft" to c.warningSoft,
        "success" to c.success,
        "success-soft" to c.successSoft,
        "inverse-surface" to c.inverseSurface,
        "inverse-ink" to c.inverseInk,
        "inverse-accent" to c.inverseAccent,
        "media-bg" to c.mediaBg,
)

internal fun mediaColorSwatches(): List<Pair<String, Color>> = listOf(
        "background" to ODVMediaColors.background,
        "on-media" to ODVMediaColors.onMedia,
        "on-media-muted" to ODVMediaColors.onMediaMuted,
        "on-media-faint" to ODVMediaColors.onMediaFaint,
        "scrim" to ODVMediaColors.scrim,
        "scrim-controls" to ODVMediaColors.scrimControls,
        "scrim-strong" to ODVMediaColors.scrimStrong,
        "pill" to ODVMediaColors.pill,
        "toolbar" to ODVMediaColors.toolbar,
        "track" to ODVMediaColors.track,
        "buffer" to ODVMediaColors.buffer,
        "ripple" to ODVMediaColors.ripple,
        "icon-well" to ODVMediaColors.iconWell,
        "card" to ODVMediaColors.card,
        "pdf-canvas" to ODVMediaColors.pdfCanvas,
        "accent" to ODVMediaColors.accent,
        "on-accent" to ODVMediaColors.onAccent,
)

/** Tên style chữ và có dùng font mono không. */
internal val typographySamples: List<Pair<String, Boolean>> = listOf(
        "display" to false,
        "title" to false,
        "heading" to false,
        "body" to false,
        "bodyStrong" to false,
        "button" to false,
        "buttonSm" to false,
        "caption" to false,
        "label" to false,
        "timecode" to true,
        "code" to true,
        "screenTitle" to false,
        "stateTitle" to false,
        "bodySm" to false,
        "bodySmStrong" to false,
        "captionStrong" to false,
        "meta" to false,
        "stepLabel" to true,
        "timecodeSm" to true,
        "timecodeXs" to true,
        "timer" to true,
)
