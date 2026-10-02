plugins {
    id("odv.kmp.library")
}

kotlin {
    android {
        namespace = "com.lambao.odv.core.security"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
        }
    }
}
