package com.lambao.odv

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import com.lambao.odv.core.designsystem.theme.ODVTheme
import com.lambao.odv.navigation.ODVNavDisplay

// AppCompatActivity thay ComponentActivity của template: AppCompatDelegate.setApplicationLocales() chỉ áp dụng ngay
// (không cần khởi động lại app) với AppCompatActivity trên Android 12 trở xuống (ADR-0011, CD-10).
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            ODVApp()
        }
    }
}

@Composable
fun ODVApp() {
    ODVTheme {
        ODVNavDisplay()
    }
}
