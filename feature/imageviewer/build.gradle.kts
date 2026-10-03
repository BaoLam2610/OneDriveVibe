plugins {
    id("odv.kmp.library")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    android {
        namespace = "com.lambao.odv.feature.imageviewer"
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
            implementation(libs.koin.compose.viewmodel)
            // WindowCompat: ẩn/hiện system bar khi chạm một lần (AN-04) và icon sáng trên nền đen.
            implementation(libs.androidx.core.ktx)
            // AsyncImage cho thumbnail lớn và GIF động (AN-01, AN-06).
            implementation(libs.coil.compose)
            // Zoom/pan, chạm đúp, subsampling ảnh lớn (AN-02, AN-06). Chỉ Android ở MVP1 (xem tech-stack.md).
            implementation(libs.telephoto.zoomable.image.coil3)
        }
    }
}
