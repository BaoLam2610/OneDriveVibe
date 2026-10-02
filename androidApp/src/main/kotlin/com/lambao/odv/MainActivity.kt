package com.lambao.odv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.lambao.odv.core.designsystem.theme.ODVTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App()
        }
    }
}

// Màn tạm thay cho App mẫu của template (đã xóa cùng :shared). Thay bằng NavDisplay khi làm feature đầu tiên (ADR-0003).
@Composable
@Preview
fun App() {
    ODVTheme {
        Text("OneDriveVibe")
    }
}
