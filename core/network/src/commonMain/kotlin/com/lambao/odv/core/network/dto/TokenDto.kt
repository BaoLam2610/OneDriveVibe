package com.lambao.odv.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Phản hồi thành công của endpoint token. Không có `refresh_token` (Client Credentials). */
@Serializable
internal data class TokenResponseDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresInSeconds: Long,
)

/** Lỗi của endpoint token. Chọn thông báo theo `error_codes`, không theo `error_description` (onedrive-graph-api.md 1.4). */
@Serializable
internal data class TokenErrorDto(
    val error: String? = null,
    @SerialName("error_description") val errorDescription: String? = null,
    @SerialName("error_codes") val errorCodes: List<Int>? = null,
)
