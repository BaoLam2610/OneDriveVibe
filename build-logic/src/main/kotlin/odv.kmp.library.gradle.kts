// Convention plugin cho mọi module core dùng chung (ADR-0001, ADR-0010).
// Module chỉ cần: plugins { id("odv.kmp.library") } và tự khai báo namespace + dependencies.

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

extensions.configure<KotlinMultiplatformExtension> {
    // ADR-0001: MVP1 chỉ có target Android. KHÔNG khai báo iosArm64()/iosSimulatorArm64() ở đây.
    // MVP2: thêm hai target iOS tại đây và viết các `actual` iOS.
    (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryTarget>("android") {
        compileSdk = libs.findVersion("android-compileSdk").get().requiredVersion.toInt()
        minSdk = libs.findVersion("android-minSdk").get().requiredVersion.toInt()
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        // ADR-0009: không viết test tự động, nên không bật withHostTest / withDeviceTestBuilder.
    }
}
