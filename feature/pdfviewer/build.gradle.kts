plugins {
    id("odv.kmp.feature")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    android {
        namespace = "com.lambao.odv.feature.pdfviewer"
    }

    sourceSets {
        androidMain.dependencies {
            // koinInject cho hàng "Tài khoản" của bảng thông tin (Lát T, mục 4.5); koinViewModel đã có từ convention plugin.
            implementation(libs.koin.compose)
            // WindowCompat: ẩn/hiện system bar khi chạm một lần (PD-06) và icon sáng trên nền đen.
            implementation(libs.androidx.core.ktx)
            // Dựng trang bằng android.graphics.pdf.PdfRenderer của hệ thống, không thêm thư viện PDF (tech-stack.md).
        }
    }
}
