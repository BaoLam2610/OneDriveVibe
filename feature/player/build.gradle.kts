plugins {
    id("odv.kmp.library")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    android {
        namespace = "com.lambao.odv.feature.player"
        // ADR-0011: chuỗi nằm trong Android res/ (values VI, values-en EN) của module này.
        androidResources {
            enable = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:domain"))
            implementation(project(":core:designsystem"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            // BaseMviViewModel kế thừa ViewModel; :core:common không lộ lifecycle nên feature tự khai báo.
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.koin.core)
            implementation(libs.koin.core.viewmodel)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
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
