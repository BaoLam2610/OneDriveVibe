package com.lambao.odv.core.security

/**
 * Tên các bí mật mà `:core:security` phải biết vì xử lý riêng. **Không được đổi giá trị:** đổi là bộ đếm sai PIN trên máy người
 * dùng bị mất (tên tệp trên đĩa là `{tên}.bin`, ADR-0014).
 */
object SecretNames {
    /**
     * Bộ đếm sai PIN. `SecretStore.wipeAll()` xóa nó **sau cùng** để nếu bị dừng giữa chừng thì config còn mà bộ đếm không về 0
     * trước (KH-06). `:core:data` (`LockoutStore`) dùng đúng hằng số này, nên hai nơi không thể lệch nhau.
     */
    const val LOCKOUT = "lock_state"
}
