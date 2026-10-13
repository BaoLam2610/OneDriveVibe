package com.lambao.odv.feature.pdfviewer

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Binding Koin của `:feature:pdfviewer`. Ghép ở MainApplication. ViewModel nhận id tệp qua `parametersOf(itemId)` vì nó nằm trong route điều hướng. */
val pdfViewerModule = module {
    viewModel { params ->
        PdfViewerViewModel(
            itemId = params.get(),
            getPdfFile = get(),
            getReadingProgress = get(),
            saveReadingProgress = get(),
            openPdf = get(),
            discardCachedPdf = get(),
            getFolderPath = get(),
            settings = get(),
        )
    }
}
