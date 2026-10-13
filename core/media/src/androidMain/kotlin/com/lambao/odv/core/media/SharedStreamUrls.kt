package com.lambao.odv.core.media

import com.lambao.odv.core.common.dispatcher.DispatcherProvider
import com.lambao.odv.core.domain.hook.ConnectionResetter
import com.lambao.odv.core.domain.usecase.viewer.GetStreamUrlUseCase

/**
 * Bản [StreamUrlProvider] dùng chung của **tab Short** (ADR-0029): bộ tải trước ([VideoPreloader]), player cùng manager tải trước
 * ([ExoPlayerFactory.createWithPreload]) và poster ([FirstFrameProvider]) cùng cần link của một video lúc vuốt tới, nên dùng chung một bản đồ link và
 * một việc lấy link cho mỗi id thay vì mỗi bên gọi Graph riêng (trước đó 2 đến 3 lần mỗi video). Màn Xem video **không** dùng bản này: mỗi player ở đó
 * giữ provider riêng, link chết theo màn xem.
 *
 * Vòng đời (CH-03): `ShortPlayerHolder` dừng mọi người dùng rồi gọi [clear] một lần khi app bị khóa và khi Màn chính bị bỏ. Mất mạng **không** xóa
 * link vì video đang phát vẫn cần. Là [ConnectionResetter] nên Ngắt kết nối (CD-05) cũng xóa link, không để link ký sống thêm tối đa 45 phút trong
 * một singleton (`DisconnectUseCase` lấy mọi [ConnectionResetter] đã đăng ký).
 */
class SharedStreamUrls(
    getStreamUrl: GetStreamUrlUseCase,
    dispatchers: DispatcherProvider,
) : ConnectionResetter {

    val provider: StreamUrlProvider = StreamUrlProvider(getStreamUrl, dispatchers)

    /** Quên mọi link và hủy các việc đang lấy link (xem [StreamUrlProvider.clear]). */
    fun clear() = provider.clear()

    override suspend fun reset() = provider.clear()
}
