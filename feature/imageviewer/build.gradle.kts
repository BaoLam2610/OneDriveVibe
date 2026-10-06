plugins {
    id("odv.kmp.feature")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    android {
        namespace = "com.lambao.odv.feature.imageviewer"
    }

    sourceSets {
        androidMain.dependencies {
            // WindowCompat: ẩn/hiện system bar khi chạm một lần (AN-04) và icon sáng trên nền đen.
            implementation(libs.androidx.core.ktx)
            // AsyncImage cho thumbnail lớn và GIF động (AN-01, AN-06).
            implementation(libs.coil.compose)
            // Zoom/pan, chạm đúp, subsampling ảnh lớn (AN-02, AN-06). Chỉ Android ở MVP1 (xem tech-stack.md).
            implementation(libs.telephoto.zoomable.image.coil3)
        }
    }
}
