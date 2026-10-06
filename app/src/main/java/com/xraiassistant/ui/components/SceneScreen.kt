package com.xraiassistant.ui.components

import android.os.Build
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xraiassistant.ui.theme.StatusColors
import com.xraiassistant.R
import com.xraiassistant.data.local.PlaygroundPreferences
import com.xraiassistant.ui.viewmodels.ChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import org.json.JSONObject

/**
 * Scene Screen - WebView-based 3D playground with Monaco editor
 * 
 * Complete iOS parity implementation with:
 * - Monaco editor matching iOS styling exactly
 * - AI code injection functionality
 * - Multiple layout modes (split horizontal/vertical, editor only, preview only)
 * - Real-time 3D rendering with Babylon.js
 * - iOS-style toolbar with layout controls
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SceneScreen(
    chatViewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by chatViewModel.uiState.collectAsStateWithLifecycle()
    val lastGeneratedCode by chatViewModel.lastGeneratedCode.collectAsStateWithLifecycle()
    val currentLibrary by chatViewModel.currentLibrary.collectAsStateWithLifecycle()
    val sandboxUrl by chatViewModel.sandboxUrl.collectAsStateWithLifecycle()
    val isBuildingCode by chatViewModel.isBuildingCode.collectAsStateWithLifecycle()
    val isCodeInjecting = uiState.isInjectingCode

    var webView by remember { mutableStateOf<WebView?>(null) }
    var currentLayout by remember { mutableStateOf(SceneLayout.SPLIT_HORIZONTAL) }
    var showLayoutMenu by remember { mutableStateOf(false) }
    var webViewLoaded by remember { mutableStateOf(false) }
    var monacoReady by remember { mutableStateOf(false) }
    var webViewError by remember { mutableStateOf<String?>(null) }
    var lastInjectedCode by remember { mutableStateOf("") }
    var showExportSuccess by remember { mutableStateOf(false) }
    var pageLoads by remember { mutableIntStateOf(0) }
    val commandLineEnabled by PlaygroundPreferences.commandLineEnabled.collectAsStateWithLifecycle()
    val commandLineScript = remember {
        context.assets.open(COMMAND_LINE_ASSET).bufferedReader().use { it.readText() }
    }
    var exportedFileUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val hasGeneratedCode = lastGeneratedCode.isNotEmpty()
    val coroutineScope = rememberCoroutineScope()

    // AUTO-INJECT CODE WHEN SCREEN BECOMES VISIBLE WITH NEW CODE
    // This matches iOS behavior where switching to Scene tab auto-injects
    LaunchedEffect(lastGeneratedCode, webViewLoaded, monacoReady) {
        if (lastGeneratedCode.isNotEmpty() &&
            lastGeneratedCode != lastInjectedCode &&
            webViewLoaded &&
            webView != null) {

            println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            println("🎯 [SceneScreen] AUTO-INJECTION TRIGGERED")
            println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            println("📏 Code length: ${lastGeneratedCode.length} characters")
            println("🔍 Code preview (first 300 chars):")
            println(lastGeneratedCode.take(300))
            println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            println("🕐 WebView loaded: $webViewLoaded")
            println("🕐 Monaco ready: $monacoReady")
            println("🚀 Starting auto-injection with retry logic...")

            lastInjectedCode = lastGeneratedCode

            // IMPORTANT: Use CodeSandbox for React-based libraries (React Three Fiber, Reactylon), direct injection for others
            if (currentLibrary?.id == "reactThreeFiber" || currentLibrary?.id == "reactylon") {
                println("🏗️ [SceneScreen] ${currentLibrary?.displayName} detected - using CodeSandbox build")
                chatViewModel.buildWithCodeSandbox(lastGeneratedCode, currentLibrary?.id)
            } else {
                println("📝 [SceneScreen] Using direct injection for ${currentLibrary?.displayName}")
                // Start injection with retry - longer initial delay for Monaco to fully initialize
                coroutineScope.launch {
                    // Inject as soon as the editor reports ready, rather than after a
                    // fixed wait sized for the slowest CDN load.
                    injectCodeWhenReady(webView!!, lastGeneratedCode)

                    // Wait 5 seconds after injection, then capture screenshot
                    println("📸 Waiting 5 seconds before capturing screenshot...")
                    delay(5000L)
                    captureAndSaveScreenshot(webView!!, chatViewModel)
                }
            }
        } else {
            if (lastGeneratedCode.isEmpty()) {
                println("⏸️ [SceneScreen] No code to inject (lastGeneratedCode is empty)")
            } else if (lastGeneratedCode == lastInjectedCode) {
                println("⏸️ [SceneScreen] Code already injected, skipping")
            } else if (!webViewLoaded) {
                println("⏸️ [SceneScreen] WebView not loaded yet, waiting...")
            } else if (webView == null) {
                println("⏸️ [SceneScreen] WebView is null, cannot inject")
            }

            // Still try to check Monaco readiness even if not injecting yet
            if (webView != null && webViewLoaded && !monacoReady) {
                coroutineScope.launch {
                    delay(2000)
                    checkMonacoReadiness(webView!!) { ready ->
                        if (ready) {
                            println("✅ Monaco confirmed ready via background check")
                            monacoReady = true
                        }
                    }
                }
            }
        }
    }

    // The scene command line: injected into each playground page as it loads, and
    // shown or hidden when the Settings toggle changes. CodeSandbox previews are a
    // third-party page, so they are left alone.
    LaunchedEffect(pageLoads, commandLineEnabled, sandboxUrl) {
        val view = webView ?: return@LaunchedEffect
        if (pageLoads == 0 || sandboxUrl != null) return@LaunchedEffect
        view.evaluateJavascript(
            "$commandLineScript\n;window.maigeCommandLine && window.maigeCommandLine.setEnabled($commandLineEnabled);",
            null
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        PlaygroundWebView(
            currentLibrary = currentLibrary,
            onWebViewCreated = {
                webView = it
                println("✅ WebView created and stored")
            },
            onWebViewLoaded = {
                webViewLoaded = true
                webViewError = null
                pageLoads++
                println("✅ WebView loaded successfully")

                // Check Monaco readiness after page load
                coroutineScope.launch {
                    // Increased delay for Monaco CDN loading (from 2s to 4s)
                    delay(4000)
                    webView?.let { view ->
                        checkMonacoReadiness(view) { ready ->
                            monacoReady = ready
                            println("🔍 Monaco readiness check after page load: $ready")
                            if (!ready) {
                                println("⏰ Monaco not ready after 4s, will retry on injection attempt")
                            } else {
                                println("✅ Monaco confirmed ready and available for injection")
                            }
                        }
                    }
                }
            },
            onWebViewError = { error ->
                webViewError = error
                webViewLoaded = false
                println("❌ WebView error: $error")
            },
            onSceneSaved = { filename, base64Data ->
                // Handle scene export - save zip file and show share sheet
                coroutineScope.launch(Dispatchers.IO) {
                    val uri = saveSceneZipFile(context, filename, base64Data)
                    withContext(Dispatchers.Main) {
                        if (uri != null) {
                            exportedFileUri = uri
                            showExportSuccess = true
                            // Show Android share sheet
                            shareFile(context, uri, filename)
                        } else {
                            webViewError = "Failed to save scene file"
                        }
                    }
                }
            },
            lastGeneratedCode = lastGeneratedCode,
            sandboxUrl = sandboxUrl,
            modifier = Modifier.fillMaxSize()
        )
        
        // Visual feedback while the playground loads. Hidden when an error is up,
        // so dismissing the error does not leave a spinner that never finishes.
        AnimatedVisibility(
            visible = hasGeneratedCode && !webViewLoaded && webViewError == null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            CodeInjectionOverlay()
        }

        // Loading overlay for CodeSandbox build
        if (isBuildingCode) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .padding(32.dp)
                        .widthIn(max = 400.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Building with CodeSandbox...",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Creating sandbox with React, Three.js, and dependencies...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Error overlay if WebView fails
        webViewError?.let { error ->
            ErrorOverlay(
                error = error,
                onDismiss = { webViewError = null },
                onRetry = {
                    webViewError = null
                    webView?.reload()
                }
            )
        }
    }
}

@Composable
private fun SceneToolbar(
    hasGeneratedCode: Boolean,
    currentLayout: SceneLayout,
    showLayoutMenu: Boolean,
    onShowLayoutMenu: (Boolean) -> Unit,
    onLayoutChange: (SceneLayout) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side - Scene info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Code,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "3D Playground",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Right side - Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status indicator for generated code
                if (hasGeneratedCode) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Code Ready",
                            tint = StatusColors.success,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Code Ready",
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusColors.success
                        )
                    }
                }

                // Layout selector
                Box {
                    IconButton(
                        onClick = { onShowLayoutMenu(true) }
                    ) {
                        Icon(
                            when (currentLayout) {
                                SceneLayout.SPLIT_HORIZONTAL -> Icons.Default.ViewColumn
                                SceneLayout.SPLIT_VERTICAL -> Icons.Default.ViewStream
                                SceneLayout.EDITOR_ONLY -> Icons.Default.Code
                                SceneLayout.PREVIEW_ONLY -> Icons.Default.Visibility
                            },
                            contentDescription = "Layout",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showLayoutMenu,
                        onDismissRequest = { onShowLayoutMenu(false) }
                    ) {
                        SceneLayout.values().forEach { layout ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            when (layout) {
                                                SceneLayout.SPLIT_HORIZONTAL -> Icons.Default.ViewColumn
                                                SceneLayout.SPLIT_VERTICAL -> Icons.Default.ViewStream
                                                SceneLayout.EDITOR_ONLY -> Icons.Default.Code
                                                SceneLayout.PREVIEW_ONLY -> Icons.Default.Visibility
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            when (layout) {
                                                SceneLayout.SPLIT_HORIZONTAL -> "Split Horizontal"
                                                SceneLayout.SPLIT_VERTICAL -> "Split Vertical"
                                                SceneLayout.EDITOR_ONLY -> "Editor Only"
                                                SceneLayout.PREVIEW_ONLY -> "Preview Only"
                                            }
                                        )
                                        if (currentLayout == layout) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    onLayoutChange(layout)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaygroundWebView(
    currentLibrary: com.xraiassistant.domain.models.Library3D?,
    onWebViewCreated: (WebView) -> Unit,
    onWebViewLoaded: () -> Unit,
    onWebViewError: (String) -> Unit,
    onSceneSaved: (filename: String, base64Data: String) -> Unit,
    lastGeneratedCode: String,
    sandboxUrl: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Track if we've already injected this code to prevent duplicate injections
    var lastInjectedCode by remember { mutableStateOf("") }

    // Track last loaded library to detect library changes
    var lastLoadedLibrary by remember { mutableStateOf<String?>(null) }

    AndroidView(
        factory = { context ->
            // CRITICAL: Wrap WebView creation in try-catch to prevent crashes
            try {
                WebView(context).apply {
                    // CRITICAL: Set explicit layout parameters so HTML can inherit height
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            } catch (e: Exception) {
                println("❌ CRITICAL: WebView creation failed: ${e.message}")
                e.printStackTrace()
                // Return a minimal WebView to prevent null pointer exceptions
                WebView(context).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            }.apply {

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        println("✅ WebView page finished loading")
                        // Hardware keys go to the focused View; without this they stay
                        // with Compose and never reach the playground.
                        view?.requestFocus()
                        onWebViewLoaded()
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        errorCode: Int,
                        description: String?,
                        failingUrl: String?
                    ) {
                        super.onReceivedError(view, errorCode, description, failingUrl)
                        println("❌ WebView error: $description")
                        onWebViewError(description ?: "WebView error occurred")
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        return false
                    }

                    // CRITICAL: Handle WebView render process crashes (API 26+)
                    override fun onRenderProcessGone(
                        view: WebView?,
                        detail: RenderProcessGoneDetail?
                    ): Boolean {
                        println("")
                        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                        println("💥 WEBVIEW RENDER PROCESS CRASHED!")
                        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                        println("   Did crash: ${detail?.didCrash()}")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            println("   Renderer priority: ${detail?.rendererPriorityAtExit()}")
                        }
                        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                        // Remove crashed WebView from view hierarchy
                        try {
                            (view?.parent as? ViewGroup)?.removeView(view)
                            println("✅ Crashed WebView removed from hierarchy")
                        } catch (e: Exception) {
                            println("⚠️ Failed to remove WebView: ${e.message}")
                        }

                        // Notify user with recovery option
                        if (detail?.didCrash() == true) {
                            onWebViewError(
                                "The 3D scene crashed due to high complexity. " +
                                "This is a CodeSandbox limitation. Tap retry to reload."
                            )
                        } else {
                            onWebViewError("WebView process terminated. Tap retry to reload.")
                        }

                        // CRITICAL: Return true to prevent app from crashing
                        println("🛡️ Crash isolated - app will continue running")
                        return true
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                        consoleMessage?.let { msg ->
                            val emoji = when (msg.messageLevel()) {
                                android.webkit.ConsoleMessage.MessageLevel.ERROR -> "❌"
                                android.webkit.ConsoleMessage.MessageLevel.WARNING -> "⚠️"
                                android.webkit.ConsoleMessage.MessageLevel.DEBUG -> "🐛"
                                android.webkit.ConsoleMessage.MessageLevel.TIP -> "💡"
                                else -> "📝"
                            }
                            println("$emoji [WebView Console - ${msg.messageLevel()}] ${msg.message()} (${msg.sourceId()}:${msg.lineNumber()})")

                            // Detect critical JavaScript errors that may trigger WebView crash
                            if (msg.messageLevel() == android.webkit.ConsoleMessage.MessageLevel.ERROR) {
                                val message = msg.message() ?: ""
                                val sourceId = msg.sourceId() ?: ""

                                // Detect CodeSandbox-specific failures (TypeScript worker, Monaco editor)
                                val isCodeSandboxError = sourceId.contains("codesandbox.io") &&
                                    (message.contains("readFile") ||
                                     message.contains("tsWorker") ||
                                     message.contains("vs/language/typescript"))

                                if (isCodeSandboxError) {
                                    println("")
                                    println("🚨━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━🚨")
                                    println("🚨 CODESANDBOX IFRAME ERROR!")
                                    println("🚨 CodeSandbox TypeScript worker failed")
                                    println("🚨 This is a CodeSandbox bug, not your app")
                                    println("🚨 Scene may not render properly")
                                    println("🚨 Error: $message")
                                    println("🚨 Recommendation: Try a simpler scene or different library")
                                    println("🚨━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━🚨")
                                } else if (message.contains("is not a function") ||
                                    message.contains("Cannot read properties of null") ||
                                    message.contains("Cannot read properties of undefined") ||
                                    message.contains("out of memory")) {
                                    println("")
                                    println("⚠️━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━⚠️")
                                    println("⚠️ CRITICAL JS ERROR DETECTED!")
                                    println("⚠️ This may trigger a WebView crash")
                                    println("⚠️ Error: $message")
                                    println("⚠️ Source: ${msg.sourceId()}:${msg.lineNumber()}")
                                    println("⚠️━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━⚠️")
                                }
                            }
                        }
                        return true
                    }
                }

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    setSupportZoom(true)
                    builtInZoomControls = false
                    displayZoomControls = false
                    mediaPlaybackRequiresUserGesture = false
                    allowFileAccessFromFileURLs = true
                    allowUniversalAccessFromFileURLs = true
                    databaseEnabled = true

                    // Enable mixed content for CodeSandbox embeds
                    mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                    // Performance optimizations for heavy content like CodeSandbox
                    cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                    @Suppress("DEPRECATION")
                    setRenderPriority(android.webkit.WebSettings.RenderPriority.HIGH)

                    // Enable Web Workers and SharedArrayBuffer for CodeSandbox
                    javaScriptCanOpenWindowsAutomatically = true

                    // CRITICAL: Optimize memory for heavy CodeSandbox iframes
                    // Note: App cache methods removed in API 33+ (deprecated)
                    // Modern WebView uses automatic caching

                    // Reduce memory pressure by disabling unnecessary features
                    setSaveFormData(false) // We don't need form data
                    setGeolocationEnabled(false) // We don't use geolocation

                    // Load images (needed for 3D scenes)
                    loadsImagesAutomatically = true

                    // Allow more storage for DOM and JavaScript
                    databaseEnabled = true
                    domStorageEnabled = true
                }

                // No explicit hardware layer: WebView is already GPU-composited, and an
                // extra layer copies every frame offscreen, which adds touch latency.

                // Take focus on touch so keyboard input follows the user into the scene.
                isFocusable = true
                isFocusableInTouchMode = true
                // No system focus box around the whole scene when it takes focus;
                // the playground manages focus inside the page itself.
                defaultFocusHighlightEnabled = false

                // Add JavaScript interface for bidirectional communication (iOS WKScriptMessageHandler equivalent)
                addJavascriptInterface(
                    PlaygroundMessageHandler(
                        onMessage = { action, data ->
                            println("🔔 [JS → Native] Action: $action, Data: $data")
                            handleWebViewMessage(action, data, onSceneSaved)
                        }
                    ),
                    "AndroidBridge"
                )

                onWebViewCreated(this)

                // Load the correct playground HTML based on selected library
                try {
                    // Get playground template from library, default to BabylonJS
                    val playgroundTemplate = currentLibrary?.playgroundTemplate ?: "playground-babylonjs.html"

                    println("📚 Loading playground template: $playgroundTemplate for library: ${currentLibrary?.displayName}")

                    val playgroundHtml = context.assets.open(playgroundTemplate).bufferedReader().use { it.readText() }
                    println("📄 Loaded HTML from assets: ${playgroundHtml.length} characters")
                    println("🔍 HTML preview (first 200 chars): ${playgroundHtml.take(200)}")

                    // Nova64's studio runner answers us with
                    // postMessage(msg, event.origin). A document with a null base URL
                    // has an opaque origin that serialises to the string "null", which
                    // is not a parseable URL — so that call throws inside the runner's
                    // EXECUTE_CODE handler BEFORE it evaluates the cart, and the scene
                    // just stays blank with no useful error. An https base URL gives the
                    // page a real origin and makes it same-origin with the runner, so the
                    // runner's replies arrive. playground-nova64.html is fully
                    // self-contained for this reason — an https document cannot load
                    // file:///android_asset/ subresources.
                    loadDataWithBaseURL(
                        playgroundBaseUrlFor(playgroundTemplate),
                        playgroundHtml,
                        "text/html",
                        "UTF-8",
                        null
                    )

                    lastLoadedLibrary = currentLibrary?.id
                    println("✅ WebView loaded playground HTML from assets successfully")
                } catch (e: Exception) {
                    println("❌ Failed to load playground HTML from assets: ${e.message}")
                    onWebViewError("Failed to load playground: ${e.message}")
                }
            }
        },
        update = { webView ->
            // PRIORITY 1: Load CodeSandbox URL if available (for React Three Fiber builds)
            if (sandboxUrl != null && lastLoadedLibrary != "codesandbox") {
                println("🔗 Loading CodeSandbox preview: $sandboxUrl")
                println("📦 This is a server-bundled React Three Fiber sandbox")

                // CRITICAL: Load asynchronously with coroutine to prevent ANR (skipped frames)
                coroutineScope.launch(Dispatchers.IO) {
                    // Small delay to let UI thread breathe (prevents "Skipped 41 frames" ANR)
                    delay(100)

                    withContext(Dispatchers.Main) {
                        try {
                            println("🚀 Starting async CodeSandbox iframe load...")
                            webView.loadUrl(sandboxUrl!!)
                            lastLoadedLibrary = "codesandbox"
                            println("✅ CodeSandbox URL load initiated (async)")
                        } catch (e: Exception) {
                            println("❌ Failed to load CodeSandbox URL: ${e.message}")
                            e.printStackTrace()
                            onWebViewError("Failed to load CodeSandbox: ${e.message}")
                        }
                    }
                }
            }
            // PRIORITY 2: Reload WebView when library changes (fallback to local HTML)
            // Also handle switching from CodeSandbox to local libraries
            else if (currentLibrary?.id != lastLoadedLibrary && currentLibrary != null && sandboxUrl == null) {
                println("🔄 Library changed from $lastLoadedLibrary to ${currentLibrary.id}, reloading WebView...")

                // If switching from CodeSandbox, force reload
                val forceReload = lastLoadedLibrary == "codesandbox"
                if (forceReload) {
                    println("🔄 Switching from CodeSandbox to local playground - forcing reload")
                }

                try {
                    val playgroundTemplate = currentLibrary.playgroundTemplate
                    println("📚 Loading new playground template: $playgroundTemplate")

                    val playgroundHtml = context.assets.open(playgroundTemplate).bufferedReader().use { it.readText() }

                    webView.loadDataWithBaseURL(
                        playgroundBaseUrlFor(playgroundTemplate),
                        playgroundHtml,
                        "text/html",
                        "UTF-8",
                        null
                    )

                    lastLoadedLibrary = currentLibrary.id
                    println("✅ WebView reloaded with new library template")
                } catch (e: Exception) {
                    println("❌ Failed to reload WebView with new library: ${e.message}")
                    onWebViewError("Failed to load ${currentLibrary.displayName}: ${e.message}")
                }
            }
        },
        modifier = modifier.fillMaxSize()
        // NOTE: Code injection now handled by LaunchedEffect above
        // This ensures injection happens when user switches to Scene tab
    )
}

/**
 * Base URL a playground document is loaded with. Nova64 needs a real https origin
 * (see the note at the first load); every other playground keeps the null base URL
 * so CDN resources and file:///android_asset/ helpers both load without CORS trouble.
 */
private fun playgroundBaseUrlFor(playgroundTemplate: String): String? =
    if (playgroundTemplate.contains("nova64")) "https://nova64.io/maigexr-playground/" else null

/**
 * Check if Monaco editor is ready
 */
private fun checkMonacoReadiness(webView: WebView, callback: (Boolean) -> Unit) {
    val checkJS = """
        (function() {
            const ready = window.editor &&
                         typeof window.editor.setValue === 'function' &&
                         window.editorReady === true;
            return ready ? "READY" : "NOT_READY";
        })();
    """.trimIndent()

    webView.evaluateJavascript(checkJS) { result ->
        val isReady = result?.replace("\"", "") == "READY"
        callback(isReady)
    }
}

/**
 * Handle messages from JavaScript (iOS WKScriptMessageHandler equivalent)
 */
private fun handleWebViewMessage(
    action: String,
    data: Map<String, Any>,
    onSceneSaved: ((String, String) -> Unit)? = null
) {
    when (action) {
        "initializationComplete" -> {
            println("✅ Monaco editor initialization complete")
            println("   Editor ready: ${data["editorReady"]}")
            println("   Engine ready: ${data["engineReady"]}")
            // Note: We now track Monaco readiness separately via checkMonacoReadiness
        }
        "codeChanged" -> {
            println("📝 Code changed in editor")
        }
        "sceneCreated" -> {
            println("✅ Scene created successfully")
        }
        "sceneError" -> {
            println("❌ Scene error: ${data["error"]}")
        }
        "codeRun" -> {
            println("✅ Code execution completed")
        }
        "codeInserted" -> {
            println("✅ Code inserted: ${data["code"]?.toString()?.take(100)}...")
        }
        "codeFormatted" -> {
            println("✅ Code formatted")
        }
        "sceneSaved" -> {
            println("💾 Scene save received from JavaScript")
            val filename = data["filename"] as? String ?: "maigeXR_scene.zip"
            val base64Data = data["data"] as? String
            val success = data["success"] as? Boolean ?: false

            if (success && base64Data != null) {
                println("✅ Scene save successful, filename: $filename, data length: ${base64Data.length}")
                onSceneSaved?.invoke(filename, base64Data)
            } else {
                println("❌ Scene save failed: ${data["error"]}")
            }
        }
        "sceneExported" -> {
            println("📦 Scene export received from JavaScript")
            val format = data["format"] as? String ?: "unknown"
            val filename = data["filename"] as? String ?: "maigeXR_scene_export"
            val base64Data = data["data"] as? String
            val success = data["success"] as? Boolean ?: false

            if (success && base64Data != null) {
                println("✅ Scene export successful ($format), filename: $filename, data length: ${base64Data.length}")
                onSceneSaved?.invoke(filename, base64Data) // Reuse sceneSaved callback for exports
            } else {
                println("❌ Scene export failed: ${data["error"]}")
            }
        }
        else -> {
            println("⚠️ Unknown action: $action")
        }
    }
}

@Composable
private fun CodeInjectionOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            shadowElevation = 4.dp,
            modifier = Modifier
                .padding(32.dp)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp
                )
                Column {
                    Text(
                        text = "Loading scene",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Your code will run as soon as the playground is ready",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorOverlay(
    error: String,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "WebView Error",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Dismiss")
                    }
                    Button(onClick = onRetry) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

// NOTE: HTML is now loaded from assets/playground-babylonjs.html (same file as iOS)
// This ensures 100% compatibility with the iOS version


/**
 * JavaScript Interface for bidirectional communication between WebView and Native Android
 * Equivalent to iOS WKScriptMessageHandler
 */
class PlaygroundMessageHandler(
    private val onMessage: (String, Map<String, Any>) -> Unit
) {
    @JavascriptInterface
    fun postMessage(jsonString: String) {
        try {
            val json = JSONObject(jsonString)
            val action = json.optString("action", "")
            val dataJson = json.optJSONObject("data")

            val data = mutableMapOf<String, Any>()
            dataJson?.let { obj ->
                obj.keys().forEach { key ->
                    data[key] = obj.get(key)
                }
            }

            onMessage(action, data)
        } catch (e: Exception) {
            println("❌ Error parsing message from JavaScript: ${e.message}")
        }
    }
}

/** Polling interval while waiting for the playground editor to come up. */
private const val READINESS_POLL_MILLIS = 250L

/** Longest wait for the editor before injecting anyway (the old retry schedule's total). */
private const val READINESS_TIMEOUT_MILLIS = 15_000L

private val PLAYGROUND_READINESS_JS = """
    (function() {
        const monacoReady = window.editor &&
                           typeof window.editor.setValue === 'function' &&
                           typeof window.editor.getValue === 'function' &&
                           typeof window.editor.layout === 'function';
        const editorFlagReady = window.editorReady === true;
        const domReady = document.readyState === 'complete';
        return (monacoReady && editorFlagReady && domReady) ? "READY" : "NOT_READY";
    })();
""".trimIndent()

private suspend fun isPlaygroundReady(webView: WebView): Boolean =
    suspendCancellableCoroutine { continuation ->
        webView.evaluateJavascript(PLAYGROUND_READINESS_JS) { result ->
            if (continuation.isActive) continuation.resume(result?.trim('"') == "READY")
        }
    }

/**
 * Inject code once the playground editor is ready, polling briefly instead of
 * sleeping a fixed worst-case delay. Cancelled with the screen, and after the
 * timeout it injects anyway, as the retry schedule it replaces did.
 */
private suspend fun injectCodeWhenReady(webView: WebView, code: String) = withContext(Dispatchers.Main) {
    val started = System.currentTimeMillis()
    val ready = withTimeoutOrNull(READINESS_TIMEOUT_MILLIS) {
        while (!isPlaygroundReady(webView)) delay(READINESS_POLL_MILLIS)
        true
    } ?: false

    val waited = System.currentTimeMillis() - started
    if (ready) {
        println("✅ Playground ready after ${waited}ms, injecting")
    } else {
        println("⏰ Playground not ready after ${waited}ms, attempting injection anyway")
    }
    insertCodeInWebView(webView, code)
}

/**
 * Insert code into WebView Monaco editor - iOS parity implementation
 * Equivalent to iOS insertCodeInWebView in ContentView.swift
 */
private fun insertCodeInWebView(webView: WebView, code: String) {
    println("")
    println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    println("📝 INSERTING CODE INTO WEBVIEW")
    println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    println("📏 Original code length: ${code.length} characters")
    println("🔍 Code preview (first 200 chars):")
    println(code.take(200))
    println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

    // Escape the code properly for JavaScript (matching iOS implementation)
    val escapedCode = code
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")

    println("📏 Escaped code length: ${escapedCode.length} characters")

    val jsCode = """
        console.log("=== CALLING ENHANCED setFullEditorContent ===");

        (function() {
            const codeToInject = "$escapedCode";
            console.log("📋 Code length to inject:", codeToInject.length);

            // Try method 1: Enhanced setFullEditorContent function
            if (typeof setFullEditorContent === 'function') {
                console.log("📋 Method 1: Using setFullEditorContent function");
                try {
                    const success = setFullEditorContent(codeToInject);
                    console.log("setFullEditorContent result:", success ? "SUCCESS" : "FAILED");
                    if (success) {
                        // Auto-run the code after injection
                        if (typeof runCode === 'function') {
                            setTimeout(() => {
                                console.log("🚀 Auto-running code after injection...");
                                runCode();
                            }, 500);
                        }
                        return "SUCCESS_METHOD_1";
                    }
                } catch (error) {
                    console.error("❌ setFullEditorContent error:", error);
                    console.error("❌ Error details:", error.message, error.stack);
                }
            }

            // Try method 2: Direct Monaco setValue with checks
            if (window.editor && typeof window.editor.setValue === 'function') {
                console.log("📋 Method 2: Direct Monaco setValue");
                try {
                    window.editor.setValue(codeToInject);
                    if (typeof window.editor.layout === 'function') {
                        window.editor.layout();
                    }
                    // Deliberately no editor.focus(): the editor is hidden behind the
                    // scene, and focusing it would swallow the scene's keyboard input.

                    // Try to trigger auto-run if available
                    if (typeof runCode === 'function') {
                        setTimeout(() => {
                            console.log("🚀 Auto-running code after injection...");
                            runCode();
                        }, 500);
                    }

                    console.log("✅ Direct injection completed");
                    return "SUCCESS_METHOD_2";
                } catch (error) {
                    console.error("❌ Direct injection error:", error);
                    console.error("❌ Error details:", error.message, error.stack);
                }
            }

            // Try method 3: Emergency text area injection (last resort)
            console.log("📋 Method 3: Emergency injection attempt");
            try {
                const editorElement = document.getElementById('monaco-editor');
                if (editorElement) {
                    console.log("Found editor element, attempting emergency injection");
                    // This is a fallback that at least puts the code somewhere visible
                    return "EMERGENCY_FALLBACK";
                }
            } catch (error) {
                console.error("❌ Emergency injection error:", error);
            }

            return "ALL_METHODS_FAILED";
        })();
    """.trimIndent()

    println("🚀 Executing ENHANCED JavaScript code injection...")
    webView.evaluateJavascript(jsCode) { result ->
        println("")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("✅ JAVASCRIPT EXECUTION COMPLETE")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("📋 Result: ${result?.replace("\"", "")}")

        when (result?.replace("\"", "")) {
            "SUCCESS_METHOD_1" -> {
                println("🎉 SUCCESS: setFullEditorContent method")
                println("✅ Code injected and auto-run triggered")
            }
            "SUCCESS_METHOD_2" -> {
                println("🎉 SUCCESS: Direct Monaco API")
                println("✅ Code injected and auto-run triggered")
            }
            "EMERGENCY_FALLBACK" -> {
                println("⚠️ WARNING: Emergency fallback used")
                println("⏳ Code injection may take up to 2 seconds")
            }
            "ALL_METHODS_FAILED" -> {
                println("❌ FAILURE: All injection methods failed")
                println("🔍 Check WebView console for errors")
            }
            else -> {
                println("✅ Injection completed")
                println("🔍 Raw result: $result")
            }
        }
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }
}

// Legacy injection function (kept for backwards compatibility)
private fun injectCodeIntoWebView(webView: WebView?, code: String) {
    if (webView == null) return
    
    try {
        // Escape the code properly for JavaScript (matching iOS implementation)
        val escapedCode = code
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        
        val jsCode = """
            console.log("=== CALLING ENHANCED setFullEditorContent ===");
            
            (function() {
                const codeToInject = "$escapedCode";
                console.log("📋 Code length to inject:", codeToInject.length);
                
                // Try method 1: Enhanced setFullEditorContent function
                if (typeof setFullEditorContent === 'function') {
                    console.log("📋 Method 1: Using setFullEditorContent function");
                    try {
                        const success = setFullEditorContent(codeToInject);
                        console.log("setFullEditorContent result:", success ? "SUCCESS" : "FAILED");
                        if (success) {
                            return "SUCCESS_METHOD_1";
                        }
                    } catch (error) {
                        console.error("❌ setFullEditorContent error:", error);
                    }
                }
                
                // Try method 2: Direct Monaco setValue with checks
                if (window.editor && typeof window.editor.setValue === 'function') {
                    console.log("📋 Method 2: Using direct Monaco setValue");
                    try {
                        window.editor.setValue(codeToInject);
                        if (typeof window.editor.layout === 'function') {
                            window.editor.layout();
                        }
                        if (typeof window.editor.focus === 'function') {
                            window.editor.focus();
                        }
                        console.log("✅ Direct Monaco setValue successful");
                        return "SUCCESS_METHOD_2";
                    } catch (error) {
                        console.error("❌ Direct Monaco error:", error);
                    }
                }
                
                // Emergency fallback: Try with delay
                console.log("📋 Emergency fallback: Delayed retry");
                setTimeout(function() {
                    if (window.editor && typeof window.editor.setValue === 'function') {
                        window.editor.setValue(codeToInject);
                        console.log("✅ Emergency fallback successful");
                    }
                }, 2000);
                
                return "EMERGENCY_FALLBACK";
            })();
        """.trimIndent()
        
        webView.evaluateJavascript(jsCode) { result ->
            println("✅ Code injection result: $result")
            when (result?.replace("\"", "")) {
                "SUCCESS_METHOD_1" -> println("🎉 Code injection successful via setFullEditorContent")
                "SUCCESS_METHOD_2" -> println("🎉 Code injection successful via direct Monaco API")
                "EMERGENCY_FALLBACK" -> println("⚠️ Used emergency fallback injection method")
                else -> println("❌ Code injection failed: $result")
            }
        }
        
    } catch (e: Exception) {
        println("❌ Code injection error: " + e.message)
    }
}

/**
 * Capture canvas screenshot and save to conversation
 * Waits 5 seconds after code injection for scene to render
 */
private suspend fun captureAndSaveScreenshot(
    webView: WebView,
    chatViewModel: ChatViewModel
) = withContext(Dispatchers.Main) {
    println("")
    println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    println("📸 CAPTURING CANVAS SCREENSHOT")
    println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

    val captureJS = """
        (function() {
            if (typeof captureCanvasScreenshot === 'function') {
                console.log("📸 Calling captureCanvasScreenshot...");
                return captureCanvasScreenshot();
            } else {
                console.error("❌ captureCanvasScreenshot function not found");
                return null;
            }
        })();
    """.trimIndent()

    webView.evaluateJavascript(captureJS) { result ->
        println("📸 Screenshot capture result received")
        if (result != null && result != "null" && result.length > 100) {
            // Remove quotes from JSON string result
            val base64Data = result.trim('"')
            println("✅ Screenshot captured successfully (${base64Data.length} characters)")
            println("💾 Saving screenshot to conversation...")
            chatViewModel.saveConversationScreenshot(base64Data)
        } else {
            println("❌ Screenshot capture failed or returned null")
            println("🔍 Result: $result")
        }
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }
}

/**
 * Save scene zip file from base64 data to user's Downloads folder
 * Returns URI for sharing the file
 *
 * Android 10+ (API 29+): Uses MediaStore to save to public Downloads (no permission needed)
 * Android 9 and below (API 26-28): Falls back to app-private storage
 */
private fun saveSceneZipFile(
    context: android.content.Context,
    filename: String,
    base64Data: String
): android.net.Uri? {
    return try {
        println("💾 Saving scene file: $filename")
        println("📊 Base64 data length: ${base64Data.length} characters")

        // Decode base64 to bytes
        val zipBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
        println("✅ Decoded ${zipBytes.size} bytes")

        // Use MediaStore for Android 10+ to save to public Downloads
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            println("📱 Android 10+ detected - using MediaStore for public Downloads")

            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Downloads.DISPLAY_NAME, filename)
                put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/zip")
                put(android.provider.MediaStore.Downloads.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(zipBytes)
                }

                // Mark file as no longer pending
                contentValues.clear()
                contentValues.put(android.provider.MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)

                println("✅ File saved to public Downloads: $uri")
                println("📁 User can find it in their Downloads folder")

                uri
            } else {
                throw Exception("Failed to create MediaStore entry")
            }
        } else {
            // Android 9 and below - use app-private storage with FileProvider
            // Note: This is accessible via FileProvider but not in public Downloads
            println("📱 Android 9 or below - using app-private storage")

            val downloadsDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
            val file = java.io.File(downloadsDir, filename)

            file.writeBytes(zipBytes)
            println("✅ File saved: ${file.absolutePath}")
            println("ℹ️ File is in app storage, accessible via FileProvider")

            // Create content URI using FileProvider
            val authority = "${context.packageName}.fileprovider"
            val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
            println("✅ Content URI created: $uri")

            uri
        }
    } catch (e: Exception) {
        println("❌ Failed to save scene file: ${e.message}")
        e.printStackTrace()
        null
    }
}

/**
 * Show Android share sheet for file
 */
private fun shareFile(
    context: android.content.Context,
    uri: android.net.Uri,
    filename: String
) {
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "m{ai}geXR Scene Export")
            putExtra(
                android.content.Intent.EXTRA_TEXT,
                "3D scene created with m{ai}geXR\n\nFilename: $filename"
            )
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = android.content.Intent.createChooser(intent, "Save or Share Scene")
        chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)

        println("✅ Share sheet launched")
    } catch (e: Exception) {
        println("❌ Failed to launch share sheet: ${e.message}")
        e.printStackTrace()
    }
}

/** The shared scene command line, injected into every playground page. */
private const val COMMAND_LINE_ASSET = "playground-commandline.js"

enum class SceneLayout {
    SPLIT_HORIZONTAL,
    SPLIT_VERTICAL,
    EDITOR_ONLY,
    PREVIEW_ONLY
}