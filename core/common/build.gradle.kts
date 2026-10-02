plugins {
    id("odv.kmp.library")
}

kotlin {
    android {
        namespace = "com.lambao.odv.core.common"
    }

    sourceSets {
        commonMain.dependencies {
            // api: Flow và CoroutineDispatcher nằm trong API công khai (DispatcherProvider, BaseMviViewModel).
            api(libs.kotlinx.coroutines.core)
            // api: mọi module dùng chung một Logger. Release không có writer nào (ADR-0012).
            api(libs.kermit)
            // implementation (không phải api): BaseMviViewModel cần ViewModel, nhưng không để lộ sang :core:domain
            // (domain phụ thuộc common và phải là Kotlin thuần, ADR-0002). Module feature tự thêm lifecycle-viewmodel.
            implementation(libs.androidx.lifecycle.viewmodel)
        }
        androidMain.dependencies {
            // Dispatchers.Main trên Android.
            implementation(libs.kotlinx.coroutines.android)
        }
    }
}
