package com.lambao.odv.core.designsystem.theme

import androidx.compose.foundation.lazy.grid.GridCells

/**
 * Bố cục lưới nội dung (board "05 Bố cục"): lề màn hình 16; tab Thư mục lưới 2 cột (khe ngang 12, dọc 16);
 * tab Thư viện lưới 4 cột hướng dọc, khe 2. Dùng cùng `LazyVerticalGrid`:
 * `LazyVerticalGrid(ODVLayout.libraryCells, horizontalArrangement = Arrangement.spacedBy(ODVLayout.libraryGap), ...)`.
 */
object ODVLayout {
    /** Lề màn hình (`space.4`). */
    val screenMargin = ODVSpacing.s4

    /** Khoảng giữa các nhóm nội dung (`space.6`). */
    val sectionGap = ODVSpacing.s6

    /** Thư mục: số cột, khe ngang, khe dọc. */
    const val FOLDER_COLUMNS = 2
    val folderColumnGap = ODVSpacing.s3
    val folderRowGap = ODVSpacing.s4
    val folderCells: GridCells = GridCells.Fixed(FOLDER_COLUMNS)

    /** Thư viện (hướng dọc): số cột và khe (`space.half`). */
    const val LIBRARY_COLUMNS = 4
    val libraryGap = ODVSpacing.half
    val libraryCells: GridCells = GridCells.Fixed(LIBRARY_COLUMNS)
}
