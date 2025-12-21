package com.xraiassistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xraiassistant.R
import com.xraiassistant.ui.components.ChatScreen
import com.xraiassistant.ui.components.SceneScreen
import com.xraiassistant.presentation.screens.ConversationHistoryScreen
import com.xraiassistant.presentation.screens.SettingsScreen
import com.xraiassistant.ui.theme.*
import com.xraiassistant.ui.viewmodels.ChatViewModel

/**
 * App views for navigation
 */
enum class AppView {
    CHAT,
    SCENE,
    SETTINGS,
    EXAMPLES,
    HISTORY
}

/**
 * MainScreen - Primary UI container
 * 
 * Equivalent to ContentView.swift in iOS
 * Provides dual-pane interface with bottom navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    chatViewModel: ChatViewModel
) {
    val uiState by chatViewModel.uiState.collectAsStateWithLifecycle()
    val lastGeneratedCode by chatViewModel.lastGeneratedCode.collectAsStateWithLifecycle()

    // Settings bottom sheet
    val settingsBottomSheetState = rememberModalBottomSheetState()

    // Setup code injection callbacks (matching iOS ContentView)
    LaunchedEffect(Unit) {
        println("🔗 Setting up ChatViewModel callbacks...")

        // Callback for code insertion
        chatViewModel.onInsertCode = { code ->
            println("=== CALLBACK: AI generated code received ===")
            println("Code length: ${code.length} characters")
            println("Code preview: ${code.take(200)}...")
            println("✅ Code will be injected when user switches to Scene tab")
        }

        // Callback for run scene command
        chatViewModel.onRunScene = {
            println("=== CALLBACK: Run scene command received ===")
            // User must manually tap Scene tab to execute
        }

        // Callback for scene description
        chatViewModel.onDescribeScene = { description ->
            println("Scene description: $description")
        }

        // Enhanced callback for build system
        chatViewModel.onInsertCodeWithBuild = { code, library ->
            println("=== ENHANCED CALLBACK: AI code with build support ===")
            println("Library: ${library.displayName}")
            println("Code length: ${code.length} characters")
            println("✅ Code will be processed when user switches to Scene tab")
        }

        println("✅ Callbacks wired successfully")
    }


    Scaffold(
        containerColor = CyberpunkBlack,
        bottomBar = {
            MainBottomNavigation(
                currentView = uiState.currentView,
                hasGeneratedCode = lastGeneratedCode.isNotEmpty(),
                onViewChange = { view ->
                    chatViewModel.updateCurrentView(view)
                },
                onSettingsClick = {
                    chatViewModel.showSettings()
                }
            )
        }
    ) { paddingValues ->
        when (uiState.currentView) {
            AppView.CHAT -> {
                ChatScreen(
                    chatViewModel = chatViewModel,
                    onNavigateToScene = {
                        // Switch to Scene tab when "Run Scene" button is clicked
                        chatViewModel.updateCurrentView(AppView.SCENE)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            AppView.SCENE -> {
                SceneScreen(
                    chatViewModel = chatViewModel,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            AppView.EXAMPLES -> {
                // Placeholder for Examples screen
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Examples Screen\n(Coming Soon)",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
            AppView.HISTORY -> {
                ConversationHistoryScreen(
                    onConversationSelected = { conversationId ->
                        // Load the selected conversation
                        chatViewModel.loadConversation(conversationId)
                        // Switch back to Chat view to show the conversation
                        chatViewModel.updateCurrentView(AppView.CHAT)
                    },
                    onNavigateBack = {
                        // Go back to previous view (Chat)
                        chatViewModel.updateCurrentView(AppView.CHAT)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            AppView.SETTINGS -> {
                // Settings is handled as a modal, not a screen
                ChatScreen(
                    chatViewModel = chatViewModel,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
        }
    }
    
    // Settings modal
    if (uiState.showSettings) {
        ModalBottomSheet(
            onDismissRequest = { chatViewModel.hideSettings() },
            sheetState = settingsBottomSheetState,
            modifier = Modifier.fillMaxHeight(0.95f)
        ) {
            SettingsScreen(
                onNavigateBack = { chatViewModel.hideSettings() }
            )
        }
    }
}

/**
 * Bottom Navigation Bar
 */
@Composable
private fun MainBottomNavigation(
    currentView: AppView,
    hasGeneratedCode: Boolean,
    onViewChange: (AppView) -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Neon accent line separator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .neonAccentLine(NeonCyan, thickness = 2.dp)
        )

        NavigationBar(
            containerColor = CyberpunkDarkGray,
            contentColor = CyberpunkGray
        ) {
        // Code Tab (Chat)
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Outlined.Code,
                    contentDescription = stringResource(R.string.nav_chat),
                    tint = if (currentView == AppView.CHAT) NeonCyan else CyberpunkGray
                )
            },
            label = {
                Text(
                    stringResource(R.string.nav_chat),
                    color = if (currentView == AppView.CHAT) NeonCyan else CyberpunkGray
                )
            },
            selected = currentView == AppView.CHAT,
            onClick = { onViewChange(AppView.CHAT) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonCyan,
                selectedTextColor = NeonCyan,
                indicatorColor = NeonCyanGlow,
                unselectedIconColor = CyberpunkGray,
                unselectedTextColor = CyberpunkGray
            )
        )
        
        // Run Scene Tab
        NavigationBarItem(
            icon = {
                Box {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = stringResource(R.string.nav_scene),
                        tint = if (currentView == AppView.SCENE) NeonPink else CyberpunkGray
                    )

                    // Notification dot for generated code with neon glow
                    if (hasGeneratedCode && currentView != AppView.SCENE) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    NeonPink,
                                    CircleShape
                                )
                                .offset(x = 8.dp, y = (-8).dp)
                        )
                    }
                }
            },
            label = {
                Text(
                    stringResource(R.string.nav_scene),
                    color = if (currentView == AppView.SCENE) NeonPink else CyberpunkGray
                )
            },
            selected = currentView == AppView.SCENE,
            onClick = { onViewChange(AppView.SCENE) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonPink,
                selectedTextColor = NeonPink,
                indicatorColor = NeonPinkGlow,
                unselectedIconColor = CyberpunkGray,
                unselectedTextColor = CyberpunkGray
            )
        )

        // History Tab
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Filled.History,
                    contentDescription = "History",
                    tint = if (currentView == AppView.HISTORY) NeonBlue else CyberpunkGray
                )
            },
            label = {
                Text(
                    "History",
                    color = if (currentView == AppView.HISTORY) NeonBlue else CyberpunkGray
                )
            },
            selected = currentView == AppView.HISTORY,
            onClick = { onViewChange(AppView.HISTORY) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonBlue,
                selectedTextColor = NeonBlue,
                indicatorColor = NeonBlueGlow,
                unselectedIconColor = CyberpunkGray,
                unselectedTextColor = CyberpunkGray
            )
        )

        // Settings Tab
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.nav_settings),
                    tint = CyberpunkGray
                )
            },
            label = {
                Text(
                    stringResource(R.string.nav_settings),
                    color = CyberpunkGray
                )
            },
            selected = false, // Settings is a modal, not a view
            onClick = onSettingsClick,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonPurple,
                selectedTextColor = NeonPurple,
                indicatorColor = NeonPurpleGlow,
                unselectedIconColor = CyberpunkGray,
                unselectedTextColor = CyberpunkGray
            )
        )
        }
    }
}

/**
 * Code injection loading overlay
 */
@Composable
private fun CodeInjectionOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.code_injection),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}