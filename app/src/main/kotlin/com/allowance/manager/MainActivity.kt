package com.allowance.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.allowance.manager.core.ui.theme.AllowanceManagerTheme
import com.allowance.manager.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AllowanceManagerTheme {
                val navController = rememberNavController()
                AppNavHost(navController = navController)
            }
        }
    }
}
