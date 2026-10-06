// Convention plugin cho mọi module :feature:* (ADR-0010, ADR-0016): gom phần giống nhau của 5 module feature.
// Module chỉ cần: plugins { id("odv.kmp.feature"); alias(libs.plugins.composeMultiplatform); alias(libs.plugins.composeCompiler) }
// rồi tự khai báo namespace và phần dependency riêng (Paging, Coil, Media3...).
//
// Cố ý KHÔNG áp dụng hai plugin Compose ở đây: áp dụng từ precompiled script cần thêm artifact vào classpath của build-logic,
// khó kiểm khi không build được. Mỗi module vẫn khai báo hai alias Compose như trước.

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    id("odv.kmp.library")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

extensions.configure<KotlinMultiplatformExtension> {
    (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryTarget>("android") {
        // ADR-0011: chuỗi nằm trong Android res/ (values VI, values-en EN) của chính module feature.
        androidResources {
            enable = true
        }
    }

    sourceSets.getByName("commonMain").dependencies {
        implementation(project(":core:common"))
        implementation(project(":core:domain"))
        implementation(project(":core:designsystem"))
        implementation(libs.findLibrary("compose.runtime").get())
        implementation(libs.findLibrary("compose.foundation").get())
        implementation(libs.findLibrary("compose.material3").get())
        implementation(libs.findLibrary("compose.ui").get())
        // BaseMviViewModel kế thừa ViewModel; :core:common không lộ lifecycle nên feature tự khai báo.
        implementation(libs.findLibrary("androidx.lifecycle.viewmodel").get())
        implementation(libs.findLibrary("koin.core").get())
        implementation(libs.findLibrary("koin.core.viewmodel").get())
    }

    sourceSets.getByName("androidMain").dependencies {
        implementation(libs.findLibrary("androidx.activity.compose").get())
        implementation(libs.findLibrary("androidx.lifecycle.runtimeCompose").get())
        implementation(libs.findLibrary("koin.compose.viewmodel").get())
    }
}
