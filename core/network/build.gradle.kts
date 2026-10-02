plugins {
    id("odv.kmp.library")
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    android {
        namespace = "com.lambao.odv.core.network"
    }

    sourceSets {
        commonMain.dependencies {
            // api: GraphApi trả AppResult/AppError.
            api(project(":core:common"))
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.serialization.kotlinxJson)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.core)
        }
        androidMain.dependencies {
            // ADR-0006: OkHttp engine trên Android. HttpClient() ở commonMain tự chọn engine này từ classpath.
            implementation(libs.ktor.client.okhttp)
        }
    }
}
