package com.lambao.odv.core.data.account

import com.lambao.odv.core.common.error.AppError
import com.lambao.odv.core.common.result.AppResult
import com.lambao.odv.core.common.result.getOrElse
import com.lambao.odv.core.domain.model.ItemKey
import com.lambao.odv.core.domain.repository.ConfigRepository

/** Đích của một lời gọi Graph: UPN của tài khoản sở hữu mục và id Graph thô của mục (ADR-0030). */
internal data class GraphTarget(val upn: String, val graphId: String)

/**
 * Đổi id mờ của mục (`<drive id>|<id Graph>`, [ItemKey]) sang thứ Graph cần: UPN của tài khoản và id thô. Đọc config **mỗi lần** để
 * khi app khóa (CH-03) lời gọi dừng ngay với `AppError.AppLocked`, và khi tài khoản đã bị xóa thì trả lỗi thay vì gọi nhầm.
 */
internal class AccountDirectory(
    private val configs: ConfigRepository,
) {

    /** Id không có tiền tố hoặc tài khoản không còn trong config thì `Http(404, "accountNotFound")`. */
    suspend fun resolve(itemKey: String): AppResult<GraphTarget> {
        val accountId = ItemKey.accountIdOf(itemKey) ?: return AppResult.Failure(notFound())
        val upn = upnOf(accountId).getOrElse { return AppResult.Failure(it) }
        return AppResult.Success(GraphTarget(upn, ItemKey.graphIdOf(itemKey)))
    }

    suspend fun upnOf(accountId: String): AppResult<String> {
        val profile = configs.load().getOrElse { return AppResult.Failure(it) }
        val account = profile.accounts.firstOrNull { it.id == accountId } ?: return AppResult.Failure(notFound())
        return AppResult.Success(account.upn)
    }

    private fun notFound() = AppError.Http(status = 404, code = "accountNotFound")
}
