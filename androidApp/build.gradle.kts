import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    // Route Navigation 3 là @Serializable để back stack được lưu và khôi phục (ADR-0003).
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    // ADR-0010: app ghép các module core. Khi làm feature thì thêm :feature:* ở đây.
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    // Chỉ để ghép module Koin ở MainApplication (networkModule, securityModule). Code app không gọi thẳng các module này.
    implementation(project(":core:network"))
    implementation(project(":core:security"))
    implementation(project(":core:designsystem"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:browser"))

    implementation(libs.androidx.activity.compose)
    // ADR-0011: AppCompatActivity + AppCompatDelegate.setApplicationLocales() để đổi ngôn ngữ trong app.
    implementation(libs.androidx.appcompat)
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)

    // Koin (ADR-0004)
    implementation(libs.koin.android)
    implementation(libs.koin.core.viewmodel)
    implementation(libs.koin.compose.viewmodel)
    // koinInject (cổng khóa trong ODVNavDisplay, ẩn nút bọ trong MainActivity): khai báo thẳng, không dựa vào transitive.
    implementation(libs.koin.compose)

    // Navigation 3 (ADR-0003)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    // SplashViewModel kế thừa BaseMviViewModel (ViewModel không lộ qua :core:common).
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    // ProcessLifecycleOwner cho tự khóa khi cả app xuống nền (CH-03, ADR-0014).
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.kotlinx.serialization.core)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
    // ADR-0012: công cụ debug chỉ có trong bản debug.
    debugImplementation(project(":tools:debug"))
}

android {
    namespace = "com.lambao.odv"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.lambao.odv"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    androidResources {
        // ADR-0011: chỉ giữ VI (mặc định, values/) và EN. Bỏ chuỗi ngôn ngữ khác của thư viện để giao diện không
        // lẫn ngôn ngữ khi máy dùng ngôn ngữ ngoài VI/EN (CD-10: khi đó app dùng Tiếng Việt).
        localeFilters += setOf("vi", "en")
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}
