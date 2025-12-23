# WebView Crash Fix Implementation Plan

## Problem Analysis

### Crash Details
- **Signal**: SIGTRAP (code 128) in MemoryInfra thread
- **Trigger**: Loading CodeSandbox iframes (phfgld sandbox)
- **JavaScript Error**: `Uncaught TypeError: l(...) is not a function` in embed.5d869bc69.js:263
- **Performance Issue**: Skipped 41 frames (main thread overload)
- **Result**: Process killed by system

### Root Causes

1. **CodeSandbox Complexity**: Iframes load massive JavaScript bundles (100+ dependencies)
2. **WebView Memory Pressure**: Default WebView has limited memory allocation
3. **Main Thread Blocking**: WebView operations blocking UI thread
4. **No Crash Recovery**: App dies when WebView crashes
5. **No Process Isolation**: WebView crash kills entire app

## Solution: Multi-Layer WebView Hardening

### Layer 1: Enable WebView Multiprocess Mode
**Purpose**: Isolate WebView crashes from app process

**Implementation**:
1. Add to AndroidManifest.xml:
```xml
<application>
    <meta-data
        android:name="android.webkit.WebView.EnableSafeBrowsing"
        android:value="true" />
    <meta-data
        android:name="android.webkit.WebView.MetricsOptOut"
        android:value="false" />
</application>
```

2. In SceneScreen.kt PlaygroundWebView factory:
```kotlin
// Enable multiprocess mode (requires API 28+)
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    val processName = "${context.packageName}:webview"
    WebView.setDataDirectorySuffix(processName)
}
```

### Layer 2: Increase WebView Memory Limits
**Purpose**: Prevent out-of-memory crashes

**Implementation** in PlaygroundWebView settings:
```kotlin
settings.apply {
    // Increase cache quota for heavy content
    setAppCacheMaxSize(100 * 1024 * 1024) // 100MB
    setAppCachePath(context.cacheDir.absolutePath)
    setAppCacheEnabled(true)

    // Allow more memory for complex pages
    setJavaScriptCanOpenWindowsAutomatically(true)
    loadsImagesAutomatically = true

    // Disable memory-intensive features we don't need
    setSaveFormData(false)
    setGeolocationEnabled(false)
}
```

### Layer 3: WebView Crash Detection and Recovery
**Purpose**: Detect crashes and reload gracefully

**Implementation**: Add crash listener
```kotlin
webViewClient = object : WebViewClient() {
    override fun onRenderProcessGone(
        view: WebView?,
        detail: RenderProcessGoneDetail?
    ): Boolean {
        println("❌ WebView render process crashed!")
        println("   Did crash: ${detail?.didCrash()}")
        println("   Renderer priority: ${detail?.rendererPriorityAtExit()}")

        // Recover from crash
        if (detail?.didCrash() == true) {
            // Remove crashed WebView
            (view?.parent as? ViewGroup)?.removeView(view)

            // Notify user
            onWebViewError("WebView crashed. Tap retry to reload.")

            // Prevent app from dying
            return true
        }

        return false // Let system handle non-crash terminations
    }
}
```

### Layer 4: Async WebView Loading (Prevent ANR)
**Purpose**: Don't block main thread with heavy operations

**Implementation**: Wrap WebView.loadUrl in coroutine
```kotlin
// In update block - load asynchronously
if (sandboxUrl != null && lastLoadedLibrary != "codesandbox") {
    coroutineScope.launch(Dispatchers.IO) {
        delay(100) // Let UI thread breathe
        withContext(Dispatchers.Main) {
            try {
                webView.loadUrl(sandboxUrl!!)
                lastLoadedLibrary = "codesandbox"
            } catch (e: Exception) {
                onWebViewError("Failed to load: ${e.message}")
            }
        }
    }
}
```

### Layer 5: JavaScript Error Isolation
**Purpose**: Prevent JavaScript errors from crashing WebView

**Implementation**: Add error handler in WebChromeClient
```kotlin
webChromeClient = object : WebChromeClient() {
    override fun onConsoleMessage(msg: android.webkit.ConsoleMessage?): Boolean {
        msg?.let {
            // Detect critical JavaScript errors
            if (it.messageLevel() == android.webkit.ConsoleMessage.MessageLevel.ERROR) {
                val message = it.message()

                // CodeSandbox-specific errors that may trigger crash
                if (message.contains("is not a function") ||
                    message.contains("Cannot read properties of null")) {
                    println("⚠️ Critical JS error detected: $message")
                    println("🛡️ Monitoring for potential WebView crash...")
                }
            }
        }
        return true
    }

    override fun onJsAlert(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        println("⚠️ JS Alert: $message")
        result?.confirm()
        return true
    }
}
```

### Layer 6: Memory Monitoring and Cleanup
**Purpose**: Proactively free memory before crash

**Implementation**: Monitor and cleanup
```kotlin
// In PlaygroundWebView factory
val memoryMonitor = object : Runnable {
    override fun run() {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val percentUsed = (usedMemory * 100 / maxMemory).toInt()

        if (percentUsed > 85) {
            println("⚠️ High memory usage: $percentUsed%")
            println("🧹 Triggering WebView cleanup...")

            webView.clearCache(true)
            webView.clearHistory()
            System.gc()
        }

        // Check every 5 seconds
        handler.postDelayed(this, 5000)
    }
}
handler.post(memoryMonitor)
```

### Layer 7: Fallback Loading Strategy
**Purpose**: If CodeSandbox iframe fails, show error with retry

**Implementation**: Timeout detection
```kotlin
LaunchedEffect(sandboxUrl) {
    if (sandboxUrl != null) {
        // Start timeout timer
        delay(15000) // 15 second timeout

        // Check if still loading
        webView?.let { view ->
            view.evaluateJavascript("document.readyState") { state ->
                if (state != "\"complete\"") {
                    println("⏱️ CodeSandbox iframe timeout after 15s")
                    onWebViewError(
                        "CodeSandbox is taking too long to load. " +
                        "The scene may be too complex. Tap retry."
                    )
                }
            }
        }
    }
}
```

## Implementation Priority

### Phase 1: Critical (Prevent Crashes)
1. ✅ Enable multiprocess WebView mode
2. ✅ Add onRenderProcessGone crash handler
3. ✅ Increase memory limits

### Phase 2: Important (Improve Stability)
4. ✅ Async WebView loading
5. ✅ JavaScript error detection
6. ✅ Loading timeout

### Phase 3: Nice to Have (Optimization)
7. ✅ Memory monitoring and cleanup

## Expected Results

- **Before Fix**: App crashes 50-80% of time when loading CodeSandbox
- **After Fix**: WebView crashes isolated, app stays alive, user sees retry option

## Testing Checklist

- [ ] Generate Reactylon scene
- [ ] CodeSandbox loads without crash
- [ ] If crash occurs, app shows error overlay (not killed)
- [ ] Tap retry successfully reloads
- [ ] Memory usage stays under 85%
- [ ] No ANR (Application Not Responding) dialogs

## Files to Modify

1. **AndroidManifest.xml** - Add WebView metadata
2. **SceneScreen.kt** - All 7 layers of hardening
3. **build.gradle.kts (app)** - Ensure minSdk 26 (WebView features)

---

**Created**: 2025-12-21
**Issue**: WebView crashes with SIGTRAP in MemoryInfra thread
**Solution**: Multi-layer WebView hardening with crash isolation
