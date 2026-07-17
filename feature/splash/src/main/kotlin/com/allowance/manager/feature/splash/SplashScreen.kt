package com.allowance.manager.feature.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToIntro: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val destination by viewModel.destination.collectAsState()

    LaunchedEffect(destination) {
        val dest = destination ?: return@LaunchedEffect
        delay(1000L)
        when (dest) {
            SplashDestination.INTRO -> onNavigateToIntro()
            SplashDestination.ONBOARDING -> onNavigateToOnboarding()
            SplashDestination.HOME -> onNavigateToHome()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "Allowance Manager")
    }
}
