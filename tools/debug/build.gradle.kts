plugins {
    id("odv.kmp.library")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

// ADR-0012: công cụ chỉ cho bản debug. :androidApp gắn bằng debugImplementation nên không lọt vào APK release.
// Chuỗi hiển thị để trực tiếp (như gallery) vì chỉ dùng khi phát triển, không đi qua res/values (ADR-0011 áp dụng cho UI sản phẩm).
kotlin {
    android {
        namespace = "com.lambao.odv.tools.debug"
        androidResources {
            enable = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:network"))
            // Trình duyệt bảng DB đọc odv.db qua kết nối của chính Room (không mở bằng SQLite hệ thống, xem StorageReader).
            implementation(project(":core:database"))
            implementation(project(":core:designsystem"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.koin.core)
            // JsonTree: parse và in đẹp JSON trong màn chi tiết log API.
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
    }
}
