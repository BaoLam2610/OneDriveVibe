package com.lambao.odv.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lambao.odv.R
import com.lambao.odv.core.designsystem.component.ODVTab
import com.lambao.odv.core.designsystem.component.ODVTabs
import com.lambao.odv.core.designsystem.icon.ODVIcon
import com.lambao.odv.core.domain.model.DriveItem
import com.lambao.odv.feature.browser.ODVBrowserScreen
import com.lambao.odv.feature.library.ODVLibraryScreen

private const val TAB_FOLDERS = 0
private const val TAB_LIBRARY = 1

/**
 * Màn Danh sách: Tabs Thư mục / Thư viện (thiet-ke-ui.md mục 4.1, 5.2). Mỗi tab là một màn riêng với AppBar và ViewModel
 * của nó; Tabs do màn này dựng rồi truyền xuống để nằm ngay dưới AppBar của từng tab. Tab đang chọn nhớ qua xoay màn
 * hình, và trạng thái cuộn của tab vừa rời được giữ lại để quay về không mất vị trí.
 *
 * [onOpenFile] chạy khi chạm một tệp ở bất kỳ tab nào; màn xem làm ở Lát 5–7.
 */
@Composable
fun ODVHomeScreen(
    modifier: Modifier = Modifier,
    onOpenFile: (DriveItem) -> Unit = {},
) {
    var selected by rememberSaveable { mutableIntStateOf(TAB_FOLDERS) }
    val stateHolder = rememberSaveableStateHolder()
    val foldersLabel = stringResource(R.string.home_tab_folders)
    val libraryLabel = stringResource(R.string.home_tab_library)
    val tabItems = remember(foldersLabel, libraryLabel) {
        listOf(ODVTab(foldersLabel, ODVIcon.Folder), ODVTab(libraryLabel, ODVIcon.Image))
    }
    // Padding 4/16/0 của Tabs cộng khoảng cách 12 giữa các khối của màn Danh sách (thiet-ke-ui.md mục 5.2).
    val tabs: @Composable () -> Unit = {
        Box(Modifier.padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 12.dp)) {
            ODVTabs(tabs = tabItems, selectedIndex = selected, onSelect = { selected = it })
        }
    }
    stateHolder.SaveableStateProvider(selected) {
        if (selected == TAB_FOLDERS) {
            ODVBrowserScreen(modifier = modifier, onOpenFile = onOpenFile, tabs = tabs)
        } else {
            ODVLibraryScreen(
                modifier = modifier,
                onOpenFile = onOpenFile,
                onShowFolders = { selected = TAB_FOLDERS },
                tabs = tabs,
            )
        }
    }
}
