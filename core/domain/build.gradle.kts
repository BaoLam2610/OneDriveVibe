plugins {
    id("odv.kmp.library")
}

kotlin {
    android {
        namespace = "com.lambao.odv.core.domain"
    }

    sourceSets {
        commonMain.dependencies {
            // ADR-0010: domain chỉ được phụ thuộc :core:common
            implementation(project(":core:common"))
        }
    }
}
