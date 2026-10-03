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
            // Argon2id (ADR-0008, ADR-0014): native nên chỉ ở androidMain, dùng sau interface PinKeyDeriver.
            implementation(libs.argon2kt)
        }
    }
}
