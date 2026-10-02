package com.lambao.odv.core.common.result

import com.lambao.odv.core.common.error.AppError

/**
 * Kết quả của một thao tác có thể thất bại: [Success] mang giá trị, [Failure] mang [AppError].
 *
 * Đặt tên `AppResult` để không trùng `kotlin.Result` (vốn chỉ mang Throwable, không mang lỗi có cấu trúc).
 */
sealed interface AppResult<out T> {
    data class Success<out T>(val value: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(value))
    is AppResult.Failure -> this
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(value)
    return this
}

inline fun <T> AppResult<T>.onFailure(action: (AppError) -> Unit): AppResult<T> {
    if (this is AppResult.Failure) action(error)
    return this
}

fun <T> AppResult<T>.getOrNull(): T? = (this as? AppResult.Success)?.value
