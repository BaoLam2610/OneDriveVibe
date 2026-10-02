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
        }
    }
}
