plugins {
    id("odv.kmp.library")
}

kotlin {
    android {
        namespace = "com.lambao.odv.core.security"
    }

    sourceSets {
        commonMain.dependencies {
            // api: SecretStore trả AppResult/AppError.
            api(project(":core:common"))
        }
        androidMain.dependencies {
            // Binding Koin của bản Android (cần androidContext()).
            implementation(libs.koin.android)
        }
    }
}
