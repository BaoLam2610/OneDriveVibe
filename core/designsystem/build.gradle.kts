plugins {
    id("odv.kmp.library")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    android {
        namespace = "com.lambao.odv.core.designsystem"
        // Font (res/font) nằm trong module này; ADR-0011: tài nguyên giao diện dùng Android res/, không dùng Compose Resources.
        androidResources {
            enable = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            // ODVRemoteImage (thumbnail, Lát 4). Chỉ Compose; Fetcher và cache nằm ở :core:data.
            implementation(libs.coil.compose)
        }
        androidMain.dependencies {
            // WindowCompat: đổi màu icon system bar theo theme của app (ODVSystemBars).
            implementation(libs.androidx.core.ktx)
            // ODVCollectEffects: thu Effect theo vòng đời STARTED.
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
    }
}
