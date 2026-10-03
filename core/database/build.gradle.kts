plugins {
    id("odv.kmp.library")
    // Room KMP (ADR-0007): KSP sinh DAO và OdvDatabaseConstructor; plugin androidx.room xuất schema JSON.
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    android {
        namespace = "com.lambao.odv.core.database"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            // api: :core:data dùng DriveDao/entity (kiểu Room trong chữ ký) nên cần thấy runtime của Room.
            api(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
        }
        androidMain.dependencies {
            // Binding Koin của bản Android (cần androidContext()).
            implementation(libs.koin.android)
        }
    }
}

dependencies {
    // ADR-0001: MVP1 chỉ có target Android nên chỉ cần kspAndroid. MVP2 thêm kspIosArm64... cùng target iOS.
    add("kspAndroid", libs.androidx.room.compiler)
}

room {
    // Schema giữ version = 1 đến khi phát hành (ADR-0007); file JSON để so khi đổi cột lúc dev.
    schemaDirectory("$projectDir/schemas")
}
