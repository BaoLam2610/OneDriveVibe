package com.lambao.odv.core.designsystem

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** Tìm Activity bao ngoài một Context (Compose có thể bọc Context nhiều lớp). Null nếu không có (ví dụ preview). */
internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
