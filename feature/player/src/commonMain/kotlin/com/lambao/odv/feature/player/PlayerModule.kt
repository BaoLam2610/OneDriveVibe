package com.lambao.odv.feature.player

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Binding Koin của `:feature:player` dùng chung mọi nền tảng. Ghép ở MainApplication cùng [androidPlayerModule]. ViewModel
 * nhận ngữ cảnh mở và id video đầu tiên qua `parametersOf(context, startItemId)` vì chúng nằm trong route điều hướng.
 */
val playerModule = module {
    viewModel { params ->
        PlayerViewModel(
            context = params.get(),
            startItemId = params.get(),
            drives = get(),
            network = get(),
        )
    }
}
