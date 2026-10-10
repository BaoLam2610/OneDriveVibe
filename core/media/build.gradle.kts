plugins {
    id("odv.kmp.library")
}

kotlin {
    android {
        namespace = "com.lambao.odv.core.media"
    }

    sourceSets {
        commonMain.dependencies {
            // api: ExoPlayerFactory nhận GetStreamUrlUseCase (domain) và VideoCache nhận DispatcherProvider (common) ở hàm dựng công khai.
            api(project(":core:common"))
            api(project(":core:domain"))
        }
        androidMain.dependencies {
            // api: ExoPlayerHandle và VideoCache.get() trả kiểu của Media3 (ExoPlayer, Cache) ra ngoài module (ADR-0025).
            api(libs.androidx.media3.exoplayer)
            // Decoder phần mềm FFmpeg dự phòng cho video máy không giải mã được, vd HEVC 10-bit trên Helio G99 (ADR-0018, GPL-3.0).
            implementation(libs.nextlib.media3ext)
            // Binding Koin của bản Android (androidMediaModule cần Context).
            implementation(libs.koin.core)
        }
    }
}
