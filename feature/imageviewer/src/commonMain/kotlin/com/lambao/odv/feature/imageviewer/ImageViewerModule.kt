package com.lambao.odv.feature.imageviewer

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Binding Koin của `:feature:imageviewer`. Ghép ở MainApplication. ViewModel nhận ngữ cảnh mở và id ảnh đầu tiên qua
 * `parametersOf(context, startItemId)` vì chúng nằm trong route điều hướng.
 */
val imageViewerModule = module {
    viewModel { params ->
        ImageViewerViewModel(
            context = params.get(),
            startItemId = params.get(),
            drives = get(),
            originals = get(),
        )
    }
}
