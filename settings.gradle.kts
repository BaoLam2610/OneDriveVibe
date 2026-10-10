rootProject.name = "OneDriveVibe"

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":androidApp")

// ADR-0010: module core.
include(":core:common")
include(":core:domain")
include(":core:data")
include(":core:network")
include(":core:database")
include(":core:security")
include(":core:designsystem")
include(":core:media")

// Module feature tạo khi bắt đầu lát dùng tới nó (ADR-0010).
include(":feature:auth")
include(":feature:browser")
include(":feature:library")
include(":feature:imageviewer")
include(":feature:player")
include(":feature:settings")
include(":feature:shorts")

// Công cụ chỉ cho bản debug (ADR-0012). androidApp gắn bằng debugImplementation.
include(":tools:debug")

// Đã bỏ :shared (module mẫu của template KMP). Phần dùng chung nay nằm ở các module :core:*.
