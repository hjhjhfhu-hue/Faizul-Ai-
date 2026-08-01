package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.ChatViewModel
import com.example.ui.components.BentoBottomNavigation
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.KnowledgeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.FaizulAITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val chatViewModel: ChatViewModel = viewModel()
            val isDarkMode by chatViewModel.isDarkMode.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = isDarkMode || systemDark

            FaizulAITheme(darkTheme = useDarkTheme) {
                val navController = rememberNavController()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = { BentoBottomNavigation(navController = navController) }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "chat",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("chat") {
                            ChatScreen(
                                viewModel = chatViewModel,
                                onNavigateToHistory = { navController.navigate("history") },
                                onNavigateToSettings = { navController.navigate("settings") },
                                onNavigateToProfile = { navController.navigate("profile") },
                                onNavigateToKnowledge = { navController.navigate("knowledge") }
                            )
                        }
                        composable("history") {
                            HistoryScreen(
                                viewModel = chatViewModel,
                                onSelectSession = { sessionId ->
                                    chatViewModel.selectSession(sessionId)
                                    navController.navigate("chat") {
                                        popUpTo("chat") { inclusive = true }
                                    }
                                },
                                onNewChat = {
                                    chatViewModel.createNewChat()
                                    navController.navigate("chat") {
                                        popUpTo("chat") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("knowledge") {
                            KnowledgeScreen(
                                viewModel = chatViewModel,
                                onSelectQuery = { query ->
                                    chatViewModel.sendMessage(query)
                                    navController.navigate("chat") {
                                        popUpTo("chat") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("profile") {
                            ProfileScreen(
                                viewModel = chatViewModel,
                                onNavigateToSettings = { navController.navigate("settings") }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = chatViewModel,
                                onNavigateToAbout = { navController.navigate("about") }
                            )
                        }
                        composable("about") {
                            AboutScreen(
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
