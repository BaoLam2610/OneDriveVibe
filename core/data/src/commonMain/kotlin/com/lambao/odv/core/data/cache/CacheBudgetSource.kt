package com.lambao.odv.core.data.cache

import com.lambao.odv.core.common.dispatcher.DispatcherProvider
import com.lambao.odv.core.domain.hook.CacheBudgetProvider
import com.lambao.odv.core.domain.model.CacheBudget
import com.lambao.odv.core.domain.model.CacheKind
import com.lambao.odv.core.domain.model.CachePolicy
import com.lambao.odv.core.domain.settings.CacheSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Giữ giới hạn bộ nhớ đệm hiện tại trong bộ nhớ để các kho đọc đồng bộ khi cần dọn (`CacheBudgetProvider.bytesFor`). Sống cùng process,
 * nên có scope riêng không hủy. Trước khi DataStore đọc xong là [CacheBudget.Default] (cùng mặc định của Cài đặt), nên không có
 * khoảng thời gian không giới hạn. Koin tạo nó lúc khởi động (`createdAtStart`) để giá trị người dùng đặt đã có khi các kho dựng lần đầu
 * (đặc biệt `ImageLoader`, Coil chỉ nhận trần lúc tạo). Vẫn có cửa sổ vài mili giây đầu tiên dùng mặc định; chấp nhận.
 */
internal class CacheBudgetSource(
    settings: CacheSettings,
    dispatchers: DispatcherProvider,
) : CacheBudgetProvider {

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)

    val budget: StateFlow<CacheBudget> =
        combine(settings.limitGb, settings.shares) { gb, shares -> CacheBudget(CachePolicy.limitBytes(gb), shares) }
            .stateIn(scope, SharingStarted.Eagerly, CacheBudget.Default)

    override fun bytesFor(kind: CacheKind): Long = budget.value.bytesFor(kind)
}
