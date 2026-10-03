package com.lambao.odv.time

import com.lambao.odv.core.domain.repository.UtcOffsetProvider
import java.util.TimeZone

/** [UtcOffsetProvider] cho Android: múi giờ mặc định của máy tại thời điểm gọi. */
class AndroidUtcOffsetProvider : UtcOffsetProvider {
    override fun currentOffsetMs(): Long = TimeZone.getDefault().getOffset(System.currentTimeMillis()).toLong()
}
