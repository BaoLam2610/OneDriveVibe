plugins {
    id("odv.kmp.library")
}

kotlin {
    android {
        namespace = "com.lambao.odv.core.domain"
    }

    sourceSets {
        commonMain.dependencies {
            // ADR-0010: domain chỉ được phụ thuộc :core:common. `api` vì AppResult/AppError nằm trong chữ ký repository.
            api(project(":core:common"))
            // api: PagingData nằm trong chữ ký DriveRepository.libraryPages (Lát 4). paging-common là Kotlin thuần, KMP.
            api(libs.androidx.paging.common)
        }
    }
}
