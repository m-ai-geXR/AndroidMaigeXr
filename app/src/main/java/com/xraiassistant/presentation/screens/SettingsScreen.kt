package com.xraiassistant.presentation.screens

import androidx.compose.ui.graphics.luminance
import com.xraiassistant.ui.theme.asColor
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.horizontalScroll
import com.xraiassistant.domain.local.LocalServerConfig
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xraiassistant.R
import com.xraiassistant.data.models.AIEffort
import com.xraiassistant.data.models.AIModel
import com.xraiassistant.domain.models.Library3D
import com.xraiassistant.monetization.BillingEntitlement
import com.xraiassistant.monetization.RemoveAdsViewModel
import com.xraiassistant.ui.theme.*
import com.xraiassistant.ui.viewmodels.ChatViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.xraiassistant.ui.theme.ThemeMode
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.ui.platform.LocalUriHandler
import com.xraiassistant.config.AppConfig
import androidx.compose.material.icons.outlined.Circle
import com.xraiassistant.data.local.PlaygroundPreferences
import com.xraiassistant.ui.theme.AppearanceStore
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.ExperimentalMaterial3Api

/** Stored value meaning no key; shown as an empty field rather than masked dots. */
private const val UNSET_API_KEY = "changeMe"

/**
 * Settings Screen - Exact recreation of iOS ContentView settings implementation
 *
 * Features identical to iOS:
 * - API Configuration Section (Together.ai, OpenAI, Anthropic, Google Gemini, CodeSandbox)
 * - Model & Library Settings Section with provider grouping
 * - Temperature and Top-P sliders with intelligent descriptions
 * - Parameter summary view with current mode display
 * - Sandbox & Deployment settings
 * - System Prompt customization
 * - Save/Cancel with visual feedback
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentVmSelectedModel by viewModel.selectedModelState.collectAsStateWithLifecycle()
    var settingsSaved by remember { mutableStateOf(false) }
    
    // Local state for editing before save
    var togetherApiKey by remember { mutableStateOf("") }
    var openaiApiKey by remember { mutableStateOf("") }
    var anthropicApiKey by remember { mutableStateOf("") }
    var googleApiKey by remember { mutableStateOf("") }
    var xaiApiKey by remember { mutableStateOf("") }
    var codesandboxApiKey by remember { mutableStateOf("") }
    var localServerUrl by remember { mutableStateOf("") }
    var localModelName by remember { mutableStateOf("") }
    var localApiKey by remember { mutableStateOf("") }
    var selectedModel by remember { mutableStateOf("") }
    var selectedLibrary by remember { mutableStateOf("") }
    var temperature by remember { mutableFloatStateOf(0.7f) }
    var topP by remember { mutableFloatStateOf(0.9f) }
    var systemPrompt by remember { mutableStateOf("") }
    var ragEnabled by remember { mutableStateOf(true) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var historyCleared by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    // Initialize local state with current values (everything except selectedModel)
    LaunchedEffect(Unit) {
        println("🔧 SettingsScreen: Loading current settings from ViewModel...")

        // Load current settings into local state
        // CRITICAL FIX: Use getRawAPIKey() for editing, not getAPIKey() which returns masked version
        togetherApiKey = viewModel.getRawAPIKey("Together.ai").let { if (it == UNSET_API_KEY) "" else it }
        openaiApiKey = viewModel.getRawAPIKey("OpenAI").let { if (it == UNSET_API_KEY) "" else it }
        anthropicApiKey = viewModel.getRawAPIKey("Anthropic").let { if (it == UNSET_API_KEY) "" else it }
        googleApiKey = viewModel.getRawAPIKey("Google AI").let { if (it == UNSET_API_KEY) "" else it }
        xaiApiKey = viewModel.getRawAPIKey("xAI").let { if (it == UNSET_API_KEY) "" else it }
        codesandboxApiKey = viewModel.getRawAPIKey("CodeSandbox").let { if (it == UNSET_API_KEY) "" else it }
        localServerUrl = viewModel.localServer.baseUrl
        localModelName = viewModel.localServer.modelName
        localApiKey = viewModel.getRawAPIKey(LocalServerConfig.PROVIDER).let { if (it == UNSET_API_KEY) "" else it }
        selectedLibrary = viewModel.currentLibraryId
        temperature = viewModel.temperature
        topP = viewModel.topP
        systemPrompt = viewModel.systemPrompt

        println("✅ SettingsScreen: Settings loaded")
        println("   Selected Library: $selectedLibrary")
        println("   Temperature: $temperature")
        println("   Top-P: $topP")
        println("   System Prompt Length: ${systemPrompt.length} characters")
        if (systemPrompt.isNotEmpty()) {
            println("   System Prompt Preview: ${systemPrompt.take(100)}...")
        } else {
            println("   ⚠️ System Prompt is EMPTY!")
        }
    }

    // Reactive sync: fires when DataStore load completes or user saves
    // Fixes the race condition where the stale default was captured before DataStore finished reading
    LaunchedEffect(currentVmSelectedModel) {
        selectedModel = currentVmSelectedModel
        println("🔄 SettingsScreen: selectedModel synced to $currentVmSelectedModel")
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                // Plain title, like the iOS settings sheet.
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            // Save all settings in a coroutine to ensure proper sequencing
                            coroutineScope.launch {
                                // Save API keys first (await completion)
                                viewModel.setAPIKey("Together.ai", togetherApiKey)
                                viewModel.setAPIKey("OpenAI", openaiApiKey)
                                viewModel.setAPIKey("Anthropic", anthropicApiKey)
                                viewModel.setAPIKey("Google AI", googleApiKey)
                                viewModel.setAPIKey("xAI", xaiApiKey)
                                viewModel.setAPIKey("CodeSandbox", codesandboxApiKey)
                                viewModel.localServer.baseUrl = localServerUrl
                                viewModel.localServer.modelName = localModelName
                                viewModel.setAPIKey(LocalServerConfig.PROVIDER, localApiKey)

                                // Apply and save model settings in one step, so the
                                // library switch cannot reset the prompt typed here.
                                viewModel.applySettings(
                                    model = selectedModel,
                                    libraryId = selectedLibrary,
                                    temperature = temperature,
                                    topP = topP,
                                    systemPrompt = systemPrompt
                                )

                                // Show success message
                                settingsSaved = true

                                // Auto-dismiss after showing confirmation
                                delay(1500L)
                                onNavigateBack()
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .height(36.dp)
                    ) {
                        Text(
                            "Save",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // API Configuration Section
            ApiConfigurationSection(
                togetherApiKey = togetherApiKey,
                onTogetherApiKeyChange = { togetherApiKey = it },
                openaiApiKey = openaiApiKey,
                onOpenaiApiKeyChange = { openaiApiKey = it },
                anthropicApiKey = anthropicApiKey,
                onAnthropicApiKeyChange = { anthropicApiKey = it },
                googleApiKey = googleApiKey,
                onGoogleApiKeyChange = { googleApiKey = it },
                xaiApiKey = xaiApiKey,
                onXaiApiKeyChange = { xaiApiKey = it },
                codesandboxApiKey = codesandboxApiKey,
                onCodesandboxApiKeyChange = { codesandboxApiKey = it },
                viewModel = viewModel
            )

            LocalModelSection(
                url = localServerUrl,
                onUrlChange = { localServerUrl = it },
                model = localModelName,
                onModelChange = { localModelName = it },
                apiKey = localApiKey,
                onApiKeyChange = { localApiKey = it }
            )
            
            // Appearance Section
            AppearanceSection()

            PlaygroundSection()

            // Ads: the Remove Ads purchase, Restore, and privacy options
            RemoveAdsSection()

            AboutSection()

            // Model & Library Settings Section
            ModelSettingsSection(
                selectedModel = selectedModel,
                onModelChange = { selectedModel = it },
                selectedLibrary = selectedLibrary,
                onLibraryChange = { selectedLibrary = it },
                temperature = temperature,
                onTemperatureChange = { temperature = it },
                topP = topP,
                onTopPChange = { topP = it },
                viewModel = viewModel
            )
            
            // Sandbox & Deployment Section
            SandboxSettingsSection()
            
            // System Prompt Section
            SystemPromptSection(
                systemPrompt = systemPrompt,
                onSystemPromptChange = { systemPrompt = it }
            )

            // Data & Privacy Section
            DataPrivacySection(
                onClearHistoryClick = { showClearAllDialog = true },
                historyCleared = historyCleared
            )

            // Save Settings Section
            SaveSettingsSection(
                settingsSaved = settingsSaved
            )
        }
    }

    // Clear all history confirmation dialog
    if (showClearAllDialog) {
        ClearAllHistoryDialog(
            onConfirm = {
                coroutineScope.launch {
                    viewModel.clearAllConversations()
                    historyCleared = true
                    showClearAllDialog = false

                    // Reset the success message after 3 seconds
                    delay(3000L)
                    historyCleared = false
                }
            },
            onDismiss = { showClearAllDialog = false }
        )
    }
}

@Composable
private fun ApiConfigurationSection(
    togetherApiKey: String,
    onTogetherApiKeyChange: (String) -> Unit,
    openaiApiKey: String,
    onOpenaiApiKeyChange: (String) -> Unit,
    anthropicApiKey: String,
    onAnthropicApiKeyChange: (String) -> Unit,
    googleApiKey: String,
    onGoogleApiKeyChange: (String) -> Unit,
    xaiApiKey: String,
    onXaiApiKeyChange: (String) -> Unit,
    codesandboxApiKey: String,
    onCodesandboxApiKeyChange: (String) -> Unit,
    viewModel: ChatViewModel
) {
    SettingsSection(
        title = "AI Provider API Keys",
        icon = Icons.Default.Key
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(
                "Keys stay on this device and are sent only to the provider they belong to.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Together.ai API Key
            ProviderAPIKeyView(
                provider = "Together.ai",
                description = "Get your API key from together.ai",
                apiKey = togetherApiKey,
                onApiKeyChange = onTogetherApiKeyChange,
                isConfigured = viewModel.isProviderConfigured("Together.ai")
            )

            // OpenAI API Key
            ProviderAPIKeyView(
                provider = "OpenAI",
                description = "Get your API key from platform.openai.com",
                apiKey = openaiApiKey,
                onApiKeyChange = onOpenaiApiKeyChange,
                isConfigured = viewModel.isProviderConfigured("OpenAI")
            )

            // Anthropic API Key
            ProviderAPIKeyView(
                provider = "Anthropic",
                description = "Get your API key from console.anthropic.com",
                apiKey = anthropicApiKey,
                onApiKeyChange = onAnthropicApiKeyChange,
                isConfigured = viewModel.isProviderConfigured("Anthropic")
            )

            // Google Gemini API Key
            ProviderAPIKeyView(
                provider = "Google AI",
                description = "Get your API key from aistudio.google.com/apikey",
                apiKey = googleApiKey,
                onApiKeyChange = onGoogleApiKeyChange,
                isConfigured = viewModel.isProviderConfigured("Google AI")
            )

            // xAI (Grok) API Key
            ProviderAPIKeyView(
                provider = "xAI",
                description = "Get your API key from console.x.ai",
                apiKey = xaiApiKey,
                onApiKeyChange = onXaiApiKeyChange,
                isConfigured = viewModel.isProviderConfigured("xAI")
            )

            // CodeSandbox API Key (Optional)
            CodeSandboxAPIKeyView(
                apiKey = codesandboxApiKey,
                onApiKeyChange = onCodesandboxApiKeyChange
            )
        }
    }
}

/**
 * One provider: name and status, the key field, where to get a key. Plain rows
 * like the iOS grouped list; no per-provider colours or glows.
 */
@Composable
private fun ProviderAPIKeyView(
    provider: String,
    description: String,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    isConfigured: Boolean,
    optional: Boolean = false
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                provider,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (optional) {
                Spacer(modifier = Modifier.width(6.dp))
                Text("Optional", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                if (isConfigured) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                contentDescription = null,
                tint = if (isConfigured) StatusColors.success else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                if (isConfigured) "Configured" else "Not set",
                style = MaterialTheme.typography.labelMedium,
                color = if (isConfigured) StatusColors.success else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        TextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            placeholder = { Text("Paste your $provider key") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.background,
                unfocusedContainerColor = MaterialTheme.colorScheme.background,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )

        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * The user's own model server (Ollama, LM Studio, any OpenAI-compatible server).
 * Once an address and model are saved, the model appears under Local in the
 * model picker. The key is optional.
 */
@Composable
private fun LocalModelSection(
    url: String,
    onUrlChange: (String) -> Unit,
    model: String,
    onModelChange: (String) -> Unit,
    apiKey: String,
    onApiKeyChange: (String) -> Unit
) {
    val ready = LocalServerConfig.chatCompletionsUrl(url) != null && model.isNotBlank()
    SettingsSection(title = "Local Model", icon = Icons.Default.Computer) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Local server", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    if (ready) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = null,
                    tint = if (ready) StatusColors.success else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (ready) "Configured" else "Not set", style = MaterialTheme.typography.labelMedium,
                    color = if (ready) StatusColors.success else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LocalField(url, onUrlChange, "Server address, e.g. http://192.168.1.20:11434", KeyboardType.Uri)
            LocalField(model, onModelChange, "Model name, e.g. qwen2.5-coder:7b", KeyboardType.Ascii)
            LocalField(apiKey, onApiKeyChange, "API key (optional)", KeyboardType.Password, secret = true)
            Text(
                "Use a model running on your own computer or network, such as Ollama or LM Studio. " +
                    "Any server with an OpenAI-compatible API works. Save, then pick it under Local in the model menu.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LocalField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    secret: Boolean = false
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done, autoCorrect = false),
        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.background,
            unfocusedContainerColor = MaterialTheme.colorScheme.background,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
private fun CodeSandboxAPIKeyView(
    apiKey: String,
    onApiKeyChange: (String) -> Unit
) {
    ProviderAPIKeyView(
        provider = "CodeSandbox",
        description = "React Three Fiber and Reactylon always use CodeSandbox. A key is optional and saves sandboxes to your account.",
        apiKey = apiKey,
        onApiKeyChange = onApiKeyChange,
        isConfigured = apiKey.isNotBlank(),
        optional = true
    )
}

@Composable
private fun ModelSettingsSection(
    selectedModel: String,
    onModelChange: (String) -> Unit,
    selectedLibrary: String,
    onLibraryChange: (String) -> Unit,
    temperature: Float,
    onTemperatureChange: (Float) -> Unit,
    topP: Float,
    onTopPChange: (Float) -> Unit,
    viewModel: ChatViewModel
) {
    SettingsSection(
        title = "Model & Library Settings",
        icon = Icons.Default.Tune
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Model Selection
            ModelSelectionView(
                selectedModel = selectedModel,
                onModelChange = onModelChange,
                viewModel = viewModel
            )
            
            // Library Selection
            LibrarySelectionView(
                selectedLibrary = selectedLibrary,
                onLibraryChange = onLibraryChange,
                viewModel = viewModel
            )
            
            val usesEffort by viewModel.usesEffortControl.collectAsStateWithLifecycle()
            val effort by viewModel.effortFlow.collectAsStateWithLifecycle()

            if (usesEffort) {
                // Claude 5 series and GPT-5.6/GPT-6 reject temperature and top_p.
                EffortPickerView(
                    effort = effort,
                    onEffortChange = { viewModel.effort = it }
                )
            } else {
                TemperatureSliderView(
                    temperature = temperature,
                    onTemperatureChange = onTemperatureChange
                )

                TopPSliderView(
                    topP = topP,
                    onTopPChange = onTopPChange
                )
            }

            // Parameter Summary
            if (usesEffort) {
                EffortSummaryView(effort = effort)
            } else {
                ParameterSummaryView(
                    temperature = temperature,
                    topP = topP
                )
            }
        }
    }
}

@Composable
private fun ModelSelectionView(
    selectedModel: String,
    onModelChange: (String) -> Unit,
    viewModel: ChatViewModel
) {
    var showDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "AI Model",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Button to open model selection dialog
        OutlinedButton(
            onClick = { showDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = viewModel.getModelDisplayName(selectedModel),
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 16.sp
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select model")
            }
        }

        // Dialog for model selection - Simplified compact design
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = {
                    Text(
                        "Select AI Model",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 500.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        viewModel.modelsByProvider.forEach { (provider, models) ->
                            // Compact provider header
                            Text(
                                text = provider,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )

                            models.forEach { model ->
                                // Compact single-line model selection
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        onModelChange(model.id)
                                        showDialog = false
                                    },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (model.id == selectedModel)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Model name with selected indicator
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            if (model.id == selectedModel) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = "Selected",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Text(
                                                text = model.displayName,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = if (model.id == selectedModel) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1
                                            )
                                        }

                                        // Compact pricing tag
                                        if (model.pricing.isNotEmpty()) {
                                            Text(
                                                text = model.pricing,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }

                            // Subtle divider between providers
                            if (provider != viewModel.modelsByProvider.keys.last()) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
        
        // Show current provider info
        val currentModel = viewModel.allAvailableModels.find { it.id == selectedModel }
        currentModel?.let { model ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Business,
                        contentDescription = "Provider",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "Provider: ${model.provider}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                if (viewModel.isProviderConfigured(model.provider)) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Configured",
                        tint = StatusColors.success,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Not configured",
                        tint = StatusColors.warning,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LibrarySelectionView(
    selectedLibrary: String,
    onLibraryChange: (String) -> Unit,
    viewModel: ChatViewModel
) {
    var showDialog by remember { mutableStateOf(false) }
    val availableLibraries = viewModel.getAvailableLibraries()
    val currentLibrary = viewModel.getCurrentLibrary()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "3D Library",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Button to open library selection dialog
        OutlinedButton(
            onClick = { showDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentLibrary.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 16.sp
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select library")
            }
        }

        // Dialog for library selection - Simplified compact design
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = {
                    Text(
                        "Select 3D Library",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        availableLibraries.forEach { library ->
                            // Compact single-line library selection
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    onLibraryChange(library.id)
                                    showDialog = false
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (library.id == selectedLibrary)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Library name with selected indicator
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (library.id == selectedLibrary) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Text(
                                            text = library.displayName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (library.id == selectedLibrary) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1
                                        )
                                    }

                                    // Compact version tag
                                    Text(
                                        text = library.version,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
        
        // Show current library info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.ViewInAr,
                    contentDescription = "Library",
                    tint = StatusColors.success,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    "Library: ${currentLibrary.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusColors.success
                )
            }
            
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = "Playground: ${currentLibrary.codeLanguage}",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EffortPickerView(
    effort: AIEffort,
    onEffortChange: (AIEffort) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Reasoning Effort",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
            ) {
                Text(
                    text = effort.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            AIEffort.entries.forEachIndexed { index, level ->
                SegmentedButton(
                    selected = level == effort,
                    onClick = { onEffortChange(level) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = AIEffort.entries.size
                    )
                ) {
                    Text(
                        text = level.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }
            }
        }

        Text(
            text = effort.summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "This model sets reasoning depth instead of temperature and top-p.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EffortSummaryView(effort: AIEffort) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Current Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
            )
        ) {
            Text(
                text = "${effort.displayName} Reasoning - ${effort.summary}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun TemperatureSliderView(
    temperature: Float,
    onTemperatureChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Temperature",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
            ) {
                Text(
                    text = String.format("%.1f", temperature),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
        
        Slider(
            value = temperature,
            onValueChange = onTemperatureChange,
            valueRange = 0.0f..2.0f,
            steps = 19, // 20 steps total (0.1 increments)
            modifier = Modifier.neonGlow(MaterialTheme.colorScheme.primary, blurRadius = 6.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outline
            )
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "0.0 - Focused",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "2.0 - Creative",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TopPSliderView(
    topP: Float,
    onTopPChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Top-p (Nucleus Sampling)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = StatusColors.success.copy(alpha = 0.1f)
                )
            ) {
                Text(
                    text = String.format("%.1f", topP),
                    style = MaterialTheme.typography.bodyMedium,
                    color = StatusColors.success,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
        
        Slider(
            value = topP,
            onValueChange = onTopPChange,
            valueRange = 0.1f..1.0f,
            steps = 8, // 9 steps total (0.1 increments)
            modifier = Modifier.neonGlow(StatusColors.success, blurRadius = 6.dp),
            colors = SliderDefaults.colors(
                thumbColor = StatusColors.success,
                activeTrackColor = StatusColors.success,
                inactiveTrackColor = MaterialTheme.colorScheme.outline
            )
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "0.1 - Precise",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "1.0 - Diverse",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            "Controls vocabulary diversity. Lower values focus on most likely words.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ParameterSummaryView(
    temperature: Float,
    topP: Float
) {
    val parameterDescription = getParameterDescription(temperature, topP)
    
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Default.Speed,
                contentDescription = "Current Mode",
                tint = Color(0xFF9C27B0),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Current Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        Card(
            modifier = Modifier.neonCardGlow(MaterialTheme.colorScheme.secondary),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
        ) {
            Text(
                text = parameterDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
private fun SandboxSettingsSection() {
    SettingsSection(
        title = "Sandbox & Deployment",
        icon = Icons.Default.Cloud
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "React Three Fiber and Reactylon run on CodeSandbox",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "These frameworks need an npm build, so their scenes are always built and shown on codesandbox.io. An internet connection is required. Babylon.js, Three.js, A-Frame and Nova64 run in the app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SystemPromptSection(
    systemPrompt: String,
    onSystemPromptChange: (String) -> Unit
) {
    SettingsSection(
        title = "System Prompt",
        icon = Icons.Default.Code
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Custom Instructions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            OutlinedTextField(
                value = systemPrompt,
                onValueChange = onSystemPromptChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .neonInputGlow(MaterialTheme.colorScheme.secondary),
                placeholder = { Text("Enter custom instructions for the AI assistant...") },
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    cursorColor = MaterialTheme.colorScheme.secondary
                )
            )
            
            Text(
                "Customize how the AI assistant behaves and responds to your requests",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SaveSettingsSection(
    settingsSaved: Boolean
) {
    SettingsSection(
        title = "",
        icon = null
    ) {
        if (settingsSaved) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Settings saved",
                    tint = StatusColors.success
                )
                Text(
                    "Settings saved successfully!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StatusColors.success
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Save Your Settings",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Save Your Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(
                    "Tap 'Save' to persist your API key, system prompt, model selection, and AI parameters. Your settings will be remembered when you return to the app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DataPrivacySection(
    onClearHistoryClick: () -> Unit,
    historyCleared: Boolean
) {
    SettingsSection(
        title = "Data & Privacy",
        icon = Icons.Default.Security
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Clear Chat History Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .neonCardGlow(MaterialTheme.colorScheme.error),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "Chat History",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Chat History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        "Clear all saved conversations and messages. This action cannot be undone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onClearHistoryClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .neonButtonGlow(MaterialTheme.colorScheme.error),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Default.DeleteForever,
                            contentDescription = "Clear All",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear All Chat History")
                    }

                    // Success message
                    if (historyCleared) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Cleared",
                                tint = StatusColors.success,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "All chat history cleared successfully",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusColors.success
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Clear All History Confirmation Dialog
 */
@Composable
private fun ClearAllHistoryDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                "Clear All Chat History?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "This will permanently delete all saved conversations and messages.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Clear All")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * The one purchase this app sells. Restore stays visible after purchase so a user
 * on a second device can find it. States the whole deal plainly: every feature
 * stays available on the free tier, so this must not imply otherwise.
 */
@Composable
private fun RemoveAdsSection(viewModel: RemoveAdsViewModel = hiltViewModel()) {
    val activity = LocalContext.current as? android.app.Activity
    val isEntitled by viewModel.isEntitled.collectAsStateWithLifecycle()
    val product by viewModel.product.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val privacyOptionsRequired by viewModel.privacyOptionsRequired.collectAsStateWithLifecycle()

    val busy = state == BillingEntitlement.State.Purchasing ||
        state == BillingEntitlement.State.Restoring ||
        state == BillingEntitlement.State.LoadingProduct

    SettingsSection(title = "Ads", icon = Icons.Default.Block) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isEntitled) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("Ads removed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "Thank you for supporting m{ai}geXR.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Text("Remove ads", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "A one-off purchase that removes banner and full-screen ads. Everything else in the app is unchanged. Nothing is locked behind it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { activity?.let(viewModel::buy) },
                        enabled = !busy && product != null && activity != null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            when {
                                state == BillingEntitlement.State.Purchasing -> "Purchasing…"
                                viewModel.displayPrice != null -> "Remove ads — ${viewModel.displayPrice}"
                                state == BillingEntitlement.State.LoadingProduct -> "Loading…"
                                else -> "Unavailable"
                            }
                        )
                    }
                }

                TextButton(onClick = viewModel::restore, enabled = !busy) {
                    Text(if (state == BillingEntitlement.State.Restoring) "Restoring…" else "Restore Purchases")
                }

                when (val current = state) {
                    is BillingEntitlement.State.Failed -> Text(
                        current.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    BillingEntitlement.State.Pending -> Text(
                        "Your purchase is waiting for approval. Ads will switch off automatically once it completes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    else -> Unit
                }

                // A standing control, not a one-time prompt: when UMP requires
                // privacy options, the user must be able to change their mind.
                if (privacyOptionsRequired && activity != null) {
                    TextButton(onClick = { viewModel.presentPrivacyOptions(activity) }) {
                        Text("Privacy options")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector?,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (title.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Quiet group header, like the iOS grouped list. The icon is
                // kept in the signature but no longer drawn.
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        content()
    }
}

private fun getParameterDescription(temperature: Float, topP: Float): String {
    return when {
        temperature in 0.0f..0.3f && topP in 0.1f..0.5f -> "Precise & Focused - Perfect for debugging"
        temperature in 0.4f..0.8f && topP in 0.6f..0.9f -> "Balanced Creativity - Ideal for most scenes"
        temperature in 0.9f..2.0f && topP in 0.9f..1.0f -> "Experimental Mode - Maximum innovation"
        else -> "Custom Configuration"
    }
}

/** Legal links the stores require to be reachable from inside the app. */
@Composable
private fun AboutSection() {
    val uriHandler = LocalUriHandler.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClickLabel = "Open the privacy policy in your browser",
                    role = Role.Button
                ) { uriHandler.openUri(AppConfig.PRIVACY_POLICY_URL) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Privacy policy",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Playground options. The command line is the one-line JavaScript console at the
 * bottom of the scene.
 */
@Composable
private fun PlaygroundSection() {
    val context = LocalContext.current
    val commandLineEnabled by PlaygroundPreferences.commandLineEnabled.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = commandLineEnabled,
                    role = Role.Switch,
                    onValueChange = { PlaygroundPreferences.setCommandLineEnabled(context, it) }
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Scene command line",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "A one-line JavaScript console at the bottom of the scene. " +
                        "Run code against the live scene, type globals() to see what it exposes, " +
                        "Tab to complete, Esc to return to the scene.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            // The whole row toggles, so the switch itself takes no separate click.
            Switch(checked = commandLineEnabled, onCheckedChange = null)
        }
    }
}

/**
 * Theme choice. Matches the desktop client's Appearance setting; the native
 * clients previously had no way to override the system.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceSection() {
    val context = LocalContext.current
    val mode by AppearanceStore.mode.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Appearance",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = mode == option,
                        onClick = { AppearanceStore.set(context, option) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ThemeMode.entries.size
                        )
                    ) {
                        Text(option.displayName)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "System follows your device setting. The splash screen is always dark.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Chat style", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            val chatTheme by AppearanceStore.chatTheme.collectAsState()
            // Same presets as iOS; each chip shows the preset's colours.
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                com.xraiassistant.ui.theme.ChatTheme.entries.forEach { theme ->
                    FilterChip(
                        selected = chatTheme == theme,
                        onClick = { AppearanceStore.setChatTheme(context, theme) },
                        label = { Text(theme.displayName) },
                        leadingIcon = {
                            Row(modifier = Modifier.clip(RoundedCornerShape(4.dp))) {
                                // Swatch in the current appearance: backdrop, then sent bubble.
                                val p = theme.palette(dark = MaterialTheme.colorScheme.background.luminance() < 0.5f)
                                listOf(p.backdropTop, p.userBubble).forEach { color ->
                                    Box(Modifier.size(width = 8.dp, height = 16.dp).background(color.asColor()))
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
