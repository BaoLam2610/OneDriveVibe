plugins {
    id("odv.kmp.feature")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    android {
        namespace = "com.lambao.odv.feature.shorts"
    }

    sourceSets {
        androidMain.dependencies {
            // Nền phát dùng chung với màn Xem video: ExoPlayerFactory, VideoCache, phân loại lỗi (ADR-0025). Không phụ thuộc :feature:player.
            implementation(project(":core:media"))
            // WindowCompat: icon system bar sáng trên nền đen (tab Short luôn nền đen).
            implementation(libs.androidx.core.ktx)
            // Thumbnail làm nền lúc video đang tải.
            implementation(libs.coil.compose)
            // PlayerSurface (TextureView) và kiểu ExoPlayer lộ ra từ :core:media.
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.uiCompose)
        }
    }
}
