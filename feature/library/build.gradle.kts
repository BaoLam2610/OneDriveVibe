plugins {
    id("odv.kmp.feature")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    android {
        namespace = "com.lambao.odv.feature.library"
    }

    sourceSets {
        commonMain.dependencies {
            // cachedIn cho luồng PagingData của Thư viện (Lát 4). PagingData nằm trong chữ ký của :core:domain.
            implementation(libs.androidx.paging.common)
        }
        androidMain.dependencies {
            // LazyPagingItems cho lưới Thư viện.
            implementation(libs.androidx.paging.compose)
        }
    }
}
