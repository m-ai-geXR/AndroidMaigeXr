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
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.xraiassistant.ui.components.MaigeXRWordmark
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
import com.xraiassistant.presentation.screens.ExamplesScreen
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

    // A reply cut off while the app was in the background is sent again on return.
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_START -> chatViewModel.onAppForegroundChanged(true)
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> chatViewModel.onAppForegroundChanged(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Settings bottom sheet
    // Opens fully expanded: half height left Settings cramped on tablets.
    val settingsBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Ads: restraint signals and the one interstitial trigger.
    val activity = LocalContext.current as? android.app.Activity
    val isGenerating by chatViewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by chatViewModel.errorMessage.collectAsStateWithLifecycle()
    LaunchedEffect(isGenerating) { adManager.setGenerationInFlight(isGenerating) }
    LaunchedEffect(errorMessage) { adManager.setErrorVisible(errorMessage != null) }

    val codeSandboxNotice by chatViewModel.codeSandboxNotice.collectAsStateWithLifecycle()
    codeSandboxNotice?.let { notice ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { chatViewModel.dismissCodeSandboxNotice() },
            title = { androidx.compose.material3.Text("Runs on CodeSandbox") },
            text = { androidx.compose.material3.Text(notice) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { chatViewModel.dismissCodeSandboxNotice() }) {
                    androidx.compose.material3.Text("OK")
                }
            }
        )
    }

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
                    onOpenHistory = { chatViewModel.updateCurrentView(AppView.HISTORY) },
                    onOpenFavorites = { chatViewModel.updateCurrentView(AppView.FAVORITES) },
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
                val library by chatViewModel.currentLibrary.collectAsStateWithLifecycle()
                ExamplesScreen(
                    library = library ?: chatViewModel.getCurrentLibrary(),
                    onExampleSelected = { example ->
                        // Same path as Run Scene on a message: load the code, show the scene.
                        chatViewModel.runCodeFromMessage(example.code, (library ?: chatViewModel.getCurrentLibrary()).id)
                        chatViewModel.updateCurrentView(AppView.SCENE)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
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

        Hairline()

        // Compact: 56dp instead of Material's 80dp, still above the 48dp touch
        // minimum. The system inset is applied outside so the height is exact.
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp,
            windowInsets = WindowInsets(0, 0, 0, 0),
            modifier = Modifier
                .navigationBarsPadding()
                .height(56.dp)
        ) {
            // Same four tabs as iOS. History and Favorites live in the chat
            // header, so the chat tab stays selected while they are open.
            val chatSelected = currentView == AppView.CHAT ||
                currentView == AppView.HISTORY || currentView == AppView.FAVORITES

            NavigationBarItem(
                selected = chatSelected,
                onClick = { onViewChange(AppView.CHAT) },
                icon = { Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null) },
                label = {
                    MaigeXRWordmark(
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        muted = !chatSelected
                    )
                },
                modifier = Modifier.semantics { contentDescription = "Chat" },
                colors = navItemColors()
            )
            NavigationBarItem(
                selected = currentView == AppView.SCENE,
                onClick = { onViewChange(AppView.SCENE) },
                icon = {
                    BadgedBox(badge = {
                        // New code waiting in the scene.
                        if (hasGeneratedCode && currentView != AppView.SCENE) Badge()
                    }) {
                        Icon(Icons.Outlined.PlayCircle, contentDescription = null)
                    }
                },
                label = { Text(stringResource(R.string.nav_scene)) },
                colors = navItemColors()
            )
            NavigationBarItem(
                selected = currentView == AppView.EXAMPLES,
                onClick = { onViewChange(AppView.EXAMPLES) },
                icon = { Icon(Icons.Outlined.AutoStories, contentDescription = null) },
                label = { Text("Examples") },
                colors = navItemColors()
            )
            NavigationBarItem(
                selected = false, // Settings opens as a sheet, not a view
                onClick = onSettingsClick,
                icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                label = { Text(stringResource(R.string.nav_settings)) },
                colors = navItemColors()
            )
        }
    }
}

@Composable
private fun navItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.primary,
    selectedTextColor = MaterialTheme.colorScheme.primary,
    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
)

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