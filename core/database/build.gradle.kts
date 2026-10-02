plugins {
    id("odv.kmp.library")
}

kotlin {
    android {
        namespace = "com.lambao.odv.core.database"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
        }
    }
}
