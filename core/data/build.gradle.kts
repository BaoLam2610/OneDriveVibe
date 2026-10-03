plugins {
    id("odv.kmp.library")
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    android {
        namespace = "com.lambao.odv.core.data"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:domain"))
            implementation(project(":core:network"))
            implementation(project(":core:database"))
            implementation(project(":core:security"))
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.core)
            // Tùy chọn hiển thị của tab Thư mục (TM-05, TM-06): DataStore Preferences là KMP.
            implementation(libs.androidx.datastore.preferences)
            // Trình tải ảnh (Lát 4): Fetcher tự viết gọi Graph và cache thumbnail trên đĩa (BN-01 → BN-03).
            // api: AndroidDataModule trả ImageLoader cho giao diện qua Koin.
            api(libs.coil.core)
        }
        androidMain.dependencies {
            // androidDataModule cần androidContext() để tạo tệp DataStore.
            implementation(libs.koin.android)
            // Decoder GIF động (AN-06, Lát 5) đăng ký trong ImageLoader của androidDataModule.
            implementation(libs.coil.gif)
        }
    }
}
