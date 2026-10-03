plugins {
    id("odv.kmp.library")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    android {
        namespace = "com.lambao.odv.feature.library"
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
            // cachedIn cho luồng PagingData của Thư viện (Lát 4). PagingData nằm trong chữ ký của :core:domain.
            implementation(libs.androidx.paging.common)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.koin.compose.viewmodel)
            // LazyPagingItems cho lưới Thư viện.
            implementation(libs.androidx.paging.compose)
        }
    }
}
