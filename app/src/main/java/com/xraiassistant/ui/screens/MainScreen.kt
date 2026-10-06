package com.xraiassistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xraiassistant.R
import com.xraiassistant.monetization.AdBannerView
import com.xraiassistant.monetization.AdManager
import com.xraiassistant.ui.components.ChatScreen
import com.xraiassistant.ui.components.SceneScreen
import com.xraiassistant.presentation.screens.ConversationHistoryScreen
import com.xraiassistant.presentation.screens.FavoritesScreen
import com.xraiassistant.presentation.screens.SettingsScreen
import com.xraiassistant.ui.theme.*
import com.xraiassistant.ui.viewmodels.ChatViewModel
import kotlinx.coroutines.delay

/**
 * App views for navigation
 */
enum class AppView {
    CHAT,
    SCENE,
    SETTINGS,
    EXAMPLES,
    HISTORY,
    FAVORITES
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
    chatViewModel: ChatViewModel,
    adManager: AdManager
) {
    val uiState by chatViewModel.uiState.collectAsStateWithLifecycle()
    val lastGeneratedCode by chatViewModel.lastGeneratedCode.collectAsStateWithLifecycle()

    // Settings bottom sheet
    val settingsBottomSheetState = rememberModalBottomSheetState()

    // Ads: restraint signals and the one interstitial trigger.
    val activity = LocalContext.current as? android.app.Activity
    val isGenerating by chatViewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by chatViewModel.errorMessage.collectAsStateWithLifecycle()
    LaunchedEffect(isGenerating) { adManager.setGenerationInFlight(isGenerating) }
    LaunchedEffect(errorMessage) { adManager.setErrorVisible(errorMessage != null) }

    // Interstitials only ever appear on the way out of a scene: the user has
    // already seen the result they asked for. Watching the view state rather
    // than one button catches every route out. The short wait lets the
    // transition finish, and is cancelled if the user goes straight back.
    var previousView by remember { mutableStateOf(uiState.currentView) }
    LaunchedEffect(uiState.currentView) {
        val leftScene = previousView == AppView.SCENE && uiState.currentView != AppView.SCENE
        previousView = uiState.currentView
        if (leftScene) {
            delay(350)
            adManager.onSceneRun(activity)
        }
    }

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
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            MainBottomNavigation(
                currentView = uiState.currentView,
                hasGeneratedCode = lastGeneratedCode.isNotEmpty(),
                adManager = adManager,
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
            AppView.FAVORITES -> {
                FavoritesScreen(
                    onFavoriteSelected = { favorite ->
                        // Load the favorite code into the scene
                        chatViewModel.runCodeFromMessage(favorite.codeContent, favorite.libraryId)
                        // Switch to Scene view
                        chatViewModel.updateCurrentView(AppView.SCENE)
                    },
                    onNavigateBack = {
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
    adManager: AdManager,
    onViewChange: (AppView) -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // AdMob banner — shown above nav bar for non-premium users
        AdBannerView(adManager = adManager)

        // Gradient navigation divider (cyan → pink → purple)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .gradientBackground(
                    colors = NavigationGradient,  // Cyan → Pink → Purple
                    angle = 0f,
                    shape = RoundedCornerShape(0.dp)
                )
        )

        NavigationBar(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,  // Transparent for glass effect
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.glassCard(
                backgroundColor = MaterialTheme.colorScheme.background,  // 25% opacity glass
                blurRadius = 8.dp,
                borderGlow = null,
                shape = RoundedCornerShape(0.dp)
            )
        ) {
        // Code Tab (Chat)
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Outlined.Code,
                    contentDescription = stringResource(R.string.nav_chat),
                    tint = if (currentView == AppView.CHAT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            label = {
                Text(
                    stringResource(R.string.nav_chat),
                    color = if (currentView == AppView.CHAT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            selected = currentView == AppView.CHAT,
            onClick = { onViewChange(AppView.CHAT) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        
        // Run Scene Tab
        NavigationBarItem(
            icon = {
                Box {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = stringResource(R.string.nav_scene),
                        tint = if (currentView == AppView.SCENE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Notification dot for generated code with neon glow
                    if (hasGeneratedCode && currentView != AppView.SCENE) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
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
                    color = if (currentView == AppView.SCENE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            selected = currentView == AppView.SCENE,
            onClick = { onViewChange(AppView.SCENE) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        // History Tab
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Filled.History,
                    contentDescription = "History",
                    tint = if (currentView == AppView.HISTORY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            label = {
                Text(
                    "History",
                    color = if (currentView == AppView.HISTORY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            selected = currentView == AppView.HISTORY,
            onClick = { onViewChange(AppView.HISTORY) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        // Favorites Tab
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = "Favorites",
                    tint = if (currentView == AppView.FAVORITES) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            label = {
                Text(
                    "Favorites",
                    color = if (currentView == AppView.FAVORITES) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            selected = currentView == AppView.FAVORITES,
            onClick = { onViewChange(AppView.FAVORITES) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        // Settings Tab
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.nav_settings),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            label = {
                Text(
                    stringResource(R.string.nav_settings),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            selected = false, // Settings is a modal, not a view
            onClick = onSettingsClick,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.secondary,
                selectedTextColor = MaterialTheme.colorScheme.secondary,
                indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
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