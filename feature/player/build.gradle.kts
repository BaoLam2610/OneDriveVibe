plugins {
    id("odv.kmp.feature")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    android {
        namespace = "com.lambao.odv.feature.player"
    }

    sourceSets {
        androidMain.dependencies {
            // koinInject trong màn xem (ngoài koinViewModel đã có từ convention plugin).
            implementation(libs.koin.compose)
            // WindowCompat: icon system bar sáng trên nền đen (màn xem luôn nền đen).
            implementation(libs.androidx.core.ktx)
            // Thumbnail làm ảnh nền lúc video đang tải.
            implementation(libs.coil.compose)
            // Phát stream, SimpleCache và DataSource tự làm mới link (VD-11, VD-14). Chỉ Android ở MVP1; iOS dùng AVPlayer sau interface.
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.uiCompose)
        }
    }
}
