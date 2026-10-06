package com.xraiassistant.presentation.screens

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
import com.xraiassistant.data.local.PlaygroundPreferences
import com.xraiassistant.ui.theme.AppearanceStore
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.ExperimentalMaterial3Api

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
    var selectedModel by remember { mutableStateOf("") }
    var selectedLibrary by remember { mutableStateOf("") }
    var temperature by remember { mutableFloatStateOf(0.7f) }
    var topP by remember { mutableFloatStateOf(0.9f) }
    var systemPrompt by remember { mutableStateOf("") }
    var useSandpackForR3F by remember { mutableStateOf(true) }
    var ragEnabled by remember { mutableStateOf(true) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var historyCleared by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    // Initialize local state with current values (everything except selectedModel)
    LaunchedEffect(Unit) {
        println("🔧 SettingsScreen: Loading current settings from ViewModel...")

        // Load current settings into local state
        // CRITICAL FIX: Use getRawAPIKey() for editing, not getAPIKey() which returns masked version
        togetherApiKey = viewModel.getRawAPIKey("Together.ai")
        openaiApiKey = viewModel.getRawAPIKey("OpenAI")
        anthropicApiKey = viewModel.getRawAPIKey("Anthropic")
        googleApiKey = viewModel.getRawAPIKey("Google AI")
        xaiApiKey = viewModel.getRawAPIKey("xAI")
        codesandboxApiKey = viewModel.getRawAPIKey("CodeSandbox")
        selectedLibrary = viewModel.currentLibraryId
        temperature = viewModel.temperature
        topP = viewModel.topP
        systemPrompt = viewModel.systemPrompt
        useSandpackForR3F = true // Default value

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
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "m{ai}geXR Settings",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
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
                    TextButton(
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
                        modifier = Modifier.neonButtonGlow(MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            "Save",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
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
            
            // Appearance Section
            AppearanceSection()

            PlaygroundSection()

            // Ads: the Remove Ads purchase, Restore, and privacy options
            RemoveAdsSection()

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
            SandboxSettingsSection(
                useSandpackForR3F = useSandpackForR3F,
                onUseSandpackChange = { useSandpackForR3F = it }
            )
            
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
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Together.ai API Key
            ProviderAPIKeyView(
                provider = "Together.ai",
                description = "Get your API key from together.ai",
                color = MaterialTheme.colorScheme.primary, // Blue
                apiKey = togetherApiKey,
                onApiKeyChange = onTogetherApiKeyChange,
                isConfigured = viewModel.isProviderConfigured("Together.ai")
            )

            // OpenAI API Key
            ProviderAPIKeyView(
                provider = "OpenAI",
                description = "Get your API key from platform.openai.com",
                color = StatusColors.success, // Green
                apiKey = openaiApiKey,
                onApiKeyChange = onOpenaiApiKeyChange,
                isConfigured = viewModel.isProviderConfigured("OpenAI")
            )

            // Anthropic API Key
            ProviderAPIKeyView(
                provider = "Anthropic",
                description = "Get your API key from console.anthropic.com",
                color = Color(0xFF9C27B0), // Purple
                apiKey = anthropicApiKey,
                onApiKeyChange = onAnthropicApiKeyChange,
                isConfigured = viewModel.isProviderConfigured("Anthropic")
            )

            // Google Gemini API Key
            ProviderAPIKeyView(
                provider = "Google AI",
                description = "Get your API key from aistudio.google.com/apikey",
                color = Color(0xFFEA4335), // Google Red
                apiKey = googleApiKey,
                onApiKeyChange = onGoogleApiKeyChange,
                isConfigured = viewModel.isProviderConfigured("Google AI")
            )

            // xAI (Grok) API Key
            ProviderAPIKeyView(
                provider = "xAI",
                description = "Get your API key from console.x.ai",
                color = Color(0xFFFF6B35), // xAI Orange
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

@Composable
private fun ProviderAPIKeyView(
    provider: String,
    description: String,
    color: Color,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    isConfigured: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .neonCardGlow(color),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "$provider API Key",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            val keyboardController = LocalSoftwareKeyboardController.current

            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKeyChange,
                placeholder = { Text("Enter your $provider API key") },
                modifier = Modifier
                    .fillMaxWidth()
                    .neonInputGlow(color),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { keyboardController?.hide() }
                ),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = color,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    cursorColor = color
                )
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isConfigured) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Configured",
                            tint = color,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Configured",
                            style = MaterialTheme.typography.bodySmall,
                            color = color
                        )
                    } else {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "API key required",
                            tint = StatusColors.warning, // Orange
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "API key required",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusColors.warning
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeSandboxAPIKeyView(
    apiKey: String,
    onApiKeyChange: (String) -> Unit
) {
    val cardColor = StatusColors.warning // Orange
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .neonCardGlow(cardColor),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, cardColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "CodeSandbox API Key (Optional)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            val keyboardController = LocalSoftwareKeyboardController.current

            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKeyChange,
                placeholder = { Text("Enter your CodeSandbox API key") },
                modifier = Modifier
                    .fillMaxWidth()
                    .neonInputGlow(cardColor),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { keyboardController?.hide() }
                ),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = cardColor,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    cursorColor = cardColor
                )
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Enables advanced CodeSandbox features and deployment",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (apiKey.isNotEmpty()) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Configured",
                            tint = StatusColors.warning,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Configured",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusColors.warning
                        )
                    } else {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Optional",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Optional - basic features work without API key",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
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
private fun SandboxSettingsSection(
    useSandpackForR3F: Boolean,
    onUseSandpackChange: (Boolean) -> Unit
) {
    SettingsSection(
        title = "Sandbox & Deployment",
        icon = Icons.Default.Cloud
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Language,
                    contentDescription = "React Three Fiber Rendering",
                    tint = StatusColors.warning,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "React Three Fiber Rendering",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Use CodeSandbox Live",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        if (useSandpackForR3F) {
                            "Real CodeSandbox projects with sharing & npm packages"
                        } else {
                            "Local playground with offline support"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = useSandpackForR3F,
                    onCheckedChange = onUseSandpackChange,
                    modifier = if (useSandpackForR3F) Modifier.neonGlow(MaterialTheme.colorScheme.primary, blurRadius = 6.dp) else Modifier,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
            
            // Description based on current setting
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    if (useSandpackForR3F) Icons.Default.CloudCircle else Icons.Default.Computer,
                    contentDescription = if (useSandpackForR3F) "Online" else "Offline",
                    tint = if (useSandpackForR3F) MaterialTheme.colorScheme.primary else StatusColors.success,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    if (useSandpackForR3F) {
                        "Online: Real CodeSandbox environment with full npm ecosystem"
                    } else {
                        "Offline: Fast local rendering, no network required"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (useSandpackForR3F) MaterialTheme.colorScheme.primary else StatusColors.success
                )
            }
            
            // Benefits info
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Benefits:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (useSandpackForR3F) {
                        Text("• Instant deployment to CodeSandbox", style = MaterialTheme.typography.bodySmall)
                        Text("• Social sharing with direct links", style = MaterialTheme.typography.bodySmall)
                        Text("• Live collaboration and embedding", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text("• Works completely offline", style = MaterialTheme.typography.bodySmall)
                        Text("• Faster local rendering", style = MaterialTheme.typography.bodySmall)
                        Text("• No external dependencies", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
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
                icon?.let {
                    Icon(
                        it,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
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
        }
    }
}
