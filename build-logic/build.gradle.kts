plugins {
    `kotlin-dsl`
}

dependencies {
    // compileOnly: chỉ để script plugin biên dịch được; plugin thật do root build nạp (tránh nạp trùng classloader).
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
}
