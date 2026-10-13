package com.lambao.odv.core.media

import com.lambao.odv.core.domain.hook.NextVideoPreloadSwitch

/** Mặc định của bản phát hành: luôn chuẩn bị sẵn video kế tiếp (ADR-0028). Bản debug ghi đè bằng công tắc ở màn Debug. */
internal object AlwaysEnabledNextVideoPreload : NextVideoPreloadSwitch {
    override fun isEnabled(): Boolean = true
}
