package com.xraiassistant.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xraiassistant.data.models.AIEffort
import com.xraiassistant.data.models.AIModel
import com.xraiassistant.data.models.AIModelControl
import com.xraiassistant.data.models.AIModels
import com.xraiassistant.data.models.ChatMessage
import com.xraiassistant.data.models.CodeSandboxDefineRequest
import com.xraiassistant.data.models.CodeSandboxTemplates
import com.xraiassistant.data.remote.CodeSandboxService
import com.xraiassistant.data.repositories.AIProviderRepository
import com.xraiassistant.data.repositories.ConversationRepository
import com.xraiassistant.data.repositories.FavoriteRepository
import com.xraiassistant.data.repositories.Library3DRepository
import com.xraiassistant.data.repositories.RAGRepository
import com.xraiassistant.data.repositories.SettingsRepository
import com.xraiassistant.domain.models.Library3D
import com.xraiassistant.ui.screens.AppView
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.xraiassistant.domain.errors.AIErrorClassifier
import com.xraiassistant.domain.errors.InterruptedRequestQueue
import com.xraiassistant.domain.errors.NetworkInterruption

/**
 * UI State for Chat screen
 */
data class ChatUiState(
    val showSettings: Boolean = false,
    val settingsSaved: Boolean = false,
    val currentView: AppView = AppView.CHAT,
    val isInjectingCode: Boolean = false,
    val webViewReady: Boolean = false
)

/**
 * ChatViewModel - Core AI integration hub for XRAiAssistant
 * 
 * Kotlin/Android port of iOS ChatViewModel.swift
 * Manages AI conversations, 3D library selection, and code generation
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val aiProviderRepository: AIProviderRepository,
    private val library3DRepository: Library3DRepository,
    private val settingsRepository: SettingsRepository,
    private val conversationRepository: ConversationRepository,  // For chat history
    private val codeSandboxService: CodeSandboxService,  // For building React Three Fiber code
    private val ragRepository: RAGRepository,  // For RAG-enhanced responses
    private val favoriteRepository: FavoriteRepository,  // For favorites (code bookmarks)
    private val backgroundReplies: com.xraiassistant.data.background.BackgroundReplies
) : ViewModel() {

    // MARK: - UI State
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // MARK: - Messages
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    // MARK: - Loading State
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // MARK: - Error State
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // MARK: - Status Message (for retry attempts, progress, etc.)
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // MARK: - AI Configuration
    private val _togetherModels = MutableStateFlow(aiProviderRepository.togetherModels())
    /** Together models for the key; updates when the live list is fetched. */
    val togetherModels: StateFlow<List<AIModel>> = _togetherModels.asStateFlow()
    private var togetherRefresh: kotlinx.coroutines.Job? = null

    private val _selectedModel = MutableStateFlow(com.xraiassistant.data.models.ModelMigrations.DEFAULT_MODEL)
    val selectedModelState: StateFlow<String> = _selectedModel.asStateFlow()
    var selectedModel: String
        get() = _selectedModel.value
        set(value) {
            if (_selectedModel.value == value) return
            _selectedModel.value = value
            // Picks from the chat header are the user's choice too; keep them
            // across launches instead of only when Settings is saved.
            persistSettings()
        }

    /** True once saved settings are loaded; nothing is written back before then. */
    private var settingsLoaded = false

    private val _temperature = MutableStateFlow(0.7f)
    var temperature: Float
        get() = _temperature.value
        set(value) { _temperature.value = value.coerceIn(0.0f, 2.0f) }

    private val _topP = MutableStateFlow(0.9f)
    var topP: Float
        get() = _topP.value
        set(value) { _topP.value = value.coerceIn(0.1f, 1.0f) }

    /** Reasoning depth for models that take effort instead of temperature/top-p. */
    private val _effort = MutableStateFlow(AIEffort.HIGH)
    val effortFlow: StateFlow<AIEffort> = _effort.asStateFlow()
    var effort: AIEffort
        get() = _effort.value
        set(value) { _effort.value = value }

    /**
     * True when the selected model takes a reasoning-effort level rather than
     * temperature/top-p, so the settings UI can show the right control.
     */
    val usesEffortControl: StateFlow<Boolean> = _selectedModel
        .map { id ->
            AIModels.ALL_MODELS.firstOrNull { it.id == id }?.control == AIModelControl.EFFORT
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _systemPrompt = MutableStateFlow("")
    var systemPrompt: String
        get() = _systemPrompt.value
        set(value) { _systemPrompt.value = value }

    // MARK: - 3D Library
    private val _currentLibrary = MutableStateFlow<Library3D?>(null)
    val currentLibrary: StateFlow<Library3D?> = _currentLibrary.asStateFlow()
    
    val currentLibraryId: String
        get() = _currentLibrary.value?.id ?: "babylonjs"

    // MARK: - Generated Code
    private val _lastGeneratedCode = MutableStateFlow("")

    /** The example the welcome message names; Run demo and an empty Run Scene play it. */
    private var welcomeExample: com.xraiassistant.domain.models.CodeExample? = null

    /** Library the code in the scene was written for, to spot leftovers after a switch. */
    private var lastCodeLibraryId: String? = null
    val lastGeneratedCode: StateFlow<String> = _lastGeneratedCode.asStateFlow()

    // The "AI code ready" notice under the chat. Raised when a response yields new
    // code and cleared once the user opens the scene or dismisses it, so it does
    // not linger for the rest of the session.
    private val _codeReadyNotice = MutableStateFlow(false)
    val codeReadyNotice: StateFlow<Boolean> = _codeReadyNotice.asStateFlow()

    // MARK: - CodeSandbox URL (for React Three Fiber builds)
    private val _sandboxUrl = MutableStateFlow<String?>(null)
    val sandboxUrl: StateFlow<String?> = _sandboxUrl.asStateFlow()

    private val _isBuildingCode = MutableStateFlow(false)
    val isBuildingCode: StateFlow<Boolean> = _isBuildingCode.asStateFlow()

    // Track if first AI response has been shown (for loading random demo)
    private var hasShownFirstResponse = false

    // MARK: - Threading Support
    private val _expandedThreads = MutableStateFlow<Set<String>>(emptySet())
    val expandedThreads: StateFlow<Set<String>> = _expandedThreads.asStateFlow()

    private val _replyToMessageId = MutableStateFlow<String?>(null)
    val replyToMessageId: StateFlow<String?> = _replyToMessageId.asStateFlow()

    // MARK: - Multimodal Support (Images)
    private val _selectedImages = MutableStateFlow<List<com.xraiassistant.data.models.AIImageContent>>(emptyList())
    val selectedImages: StateFlow<List<com.xraiassistant.data.models.AIImageContent>> = _selectedImages.asStateFlow()

    // MARK: - RAG (Retrieval-Augmented Generation) Support
    private val _ragEnabled = MutableStateFlow(true)  // Always on by default
    val ragEnabled: StateFlow<Boolean> = _ragEnabled.asStateFlow()

    // MARK: - Chat History / Conversation Tracking
    // Track current conversation ID (null for new unsaved conversation)
    private var currentConversationId: String? = null

    // MARK: - Callbacks (equivalent to iOS closures)
    var onInsertCode: ((String) -> Unit)? = null
    var onRunScene: (() -> Unit)? = null
    var onDescribeScene: ((String) -> Unit)? = null
    var onInsertCodeWithBuild: ((String, Library3D) -> Unit)? = null

    init {
        println("🚀 ChatViewModel initialization starting...")

        // Initialize UI state
        _uiState.value = ChatUiState()

        // The models this Together key can use, refreshed once a day.
        refreshTogetherModels()

        // Setup in correct order (matching iOS):
        // 1. Setup initial welcome message
        // 2. Setup default system prompt from library
        // 3. Load saved settings (which may override system prompt)
        setupInitialMessage()

        // Replies finished by a background job: while the chat is open, and any
        // that arrived after the app was closed.
        viewModelScope.launch {
            com.xraiassistant.data.background.BackgroundReplyStore.finished.collect { collectBackgroundReply(it) }
        }
        collectOrphanedReplies()
        setupDefaultSystemPrompt()
        loadSettings()
        loadFavoritedMessages()

        println("✅ ChatViewModel initialization complete")
    }

    /**
     * Setup initial welcome message
     * Equivalent to iOS setupInitialMessage()
     */
    private fun setupInitialMessage() {
        val defaultLibrary = library3DRepository.getDefaultLibrary()
        _currentLibrary.value = defaultLibrary

        welcomeExample = defaultLibrary.examples.randomOrNull()
        var welcomeContent = defaultLibrary.getWelcomeMessage(welcomeExample)

        // Check if API key is configured
        val currentAPIKey = aiProviderRepository.getAPIKey("Together.ai")
        if (currentAPIKey == "changeMe" || currentAPIKey.length < 10) {
            welcomeContent += "\n\n⚠️ **Setup Required**: Please configure your Together.ai API key in Settings (gear icon) to start chatting. Get your free API key at https://api.together.ai/settings/api-keys"
        }

        val welcomeMessage = ChatMessage(
            id = java.util.UUID.randomUUID().toString(),
            content = welcomeContent,
            isUser = false,
            timestamp = java.util.Date(),
            libraryId = defaultLibrary.id,  // Track which library this welcome message is for
            isWelcomeMessage = true  // Mark as welcome message to show "Run Demo" button
        )
        println("📨 Created welcome message: isWelcomeMessage=${welcomeMessage.isWelcomeMessage}, libraryId=${welcomeMessage.libraryId}")
        _messages.value = listOf(welcomeMessage)
    }

    /**
     * Setup default system prompt from current library
     * Equivalent to iOS setupDefaultSystemPrompt()
     */
    private fun setupDefaultSystemPrompt() {
        val defaultLibrary = library3DRepository.getDefaultLibrary()
        _systemPrompt.value = defaultLibrary.systemPrompt
        println("📝 Default system prompt set from ${defaultLibrary.displayName} (${_systemPrompt.value.length} characters)")
    }

    // MARK: - Core Chat Functions

    /**
     * Send message to AI and handle response
     * Equivalent to sendMessage in iOS
     */
    /**
     * Send message with streaming response (NEW - iOS parity)
     *
     * Provides real-time feedback as AI generates response, matching iOS behavior.
     */
    /** A request cut off by backgrounding or sleep, sent again when the app is back. */
    private val interruptedRequests = InterruptedRequestQueue()
    private var appInForeground = true

    /** Called from the UI lifecycle: send any interrupted request once the app is in front. */
    fun onAppForegroundChanged(inForeground: Boolean) {
        appInForeground = inForeground
        if (inForeground) {
            interruptedRequests.resume()
            checkReplyOnReturn()
        } else {
            handOffActiveReply()
        }
    }

    // MARK: - Replies that survive leaving the app

    /**
     * The reply in flight. Its id makes sure it is shown exactly once, whether it
     * arrives by the live stream or from the background job.
     */
    private data class ActiveReply(
        val id: String,
        val content: String,
        val currentCode: String,
        val threadParentId: String?,
        val prompt: String,
        val systemPrompt: String,
        val model: String,
        val temperature: Double,
        val topP: Double,
        val effort: String,
        val placeholderId: String,
        val library: Library3D?,
        val messagesBefore: List<ChatMessage>,
        val streamJob: kotlinx.coroutines.Job?,
        val restarted: Boolean,
        var handedOff: Boolean = false,
        var leftApp: Boolean = false,
        /** Last time any text arrived, for spotting a stream that went quiet. */
        var lastProgress: Long = System.currentTimeMillis()
    )

    private var activeReply: ActiveReply? = null
    private val deliveredByBackground = mutableSetOf<String>()

    /** Leaving the app mid-reply: let a WorkManager job finish it. */
    private fun handOffActiveReply() {
        val reply = activeReply ?: return
        reply.leftApp = true
        if (reply.handedOff) return
        backgroundReplies.start(
            com.xraiassistant.data.background.BackgroundReplyStore.Job(
                id = reply.id, prompt = reply.prompt, systemPrompt = reply.systemPrompt, model = reply.model,
                temperature = reply.temperature, topP = reply.topP, effort = reply.effort
            )
        )
        reply.handedOff = true
        Log.d("ChatViewModel", "📨 Reply handed to a background job")
    }

    /** A background job finished. Show its reply if the live stream has not. */
    private fun collectBackgroundReply(jobId: String) {
        val store = backgroundReplies.store
        val outcome = store.outcome(jobId) ?: return
        store.remove(jobId)
        val reply = activeReply
        if (reply == null || reply.id != jobId) {
            // The app was closed when it finished: keep the reply rather than lose it.
            outcome.text?.let { text ->
                _messages.value = _messages.value + ChatMessage.aiMessage(
                    content = text,
                    model = getModelDisplayName(outcome.model ?: _selectedModel.value),
                    libraryId = _currentLibrary.value?.id
                )
                processAIResponse(text, _currentLibrary.value)
            }
            return
        }
        val text = outcome.text
        if (text == null) {
            Log.w("ChatViewModel", "Background job failed: ${outcome.error}")
            reply.handedOff = false
            restartIfStalled(reply.id, 0)
            return
        }
        Log.d("ChatViewModel", "📬 Reply delivered by the background job")
        deliveredByBackground += reply.id
        activeReply = null
        val updated = _messages.value.toMutableList()
        val index = updated.indexOfFirst { it.id == reply.placeholderId }
        if (index >= 0) {
            updated[index] = updated[index].copy(content = text, isStreaming = false)
        } else {
            updated += ChatMessage.aiMessage(content = text, model = getModelDisplayName(reply.model), libraryId = reply.library?.id)
        }
        _messages.value = updated
        reply.streamJob?.cancel()
        processAIResponse(text, reply.library)
        autoSaveConversation()
        _errorMessage.value = null
        _isLoading.value = false
    }

    /** Back in the app: collect a finished reply, and never let a stalled one spin forever. */
    private fun checkReplyOnReturn() {
        val reply = activeReply ?: return
        if (!reply.leftApp) return
        if (backgroundReplies.store.outcome(reply.id) != null) {
            collectBackgroundReply(reply.id)
            return
        }
        restartIfStalled(reply.id, if (reply.handedOff) BACKGROUND_TIMEOUT_MS else STALL_TIMEOUT_MS)
    }

    /** Sends the request again if reply [id] still has not arrived after [delayMs]. */
    /** Set for one request after a GLM reply came back with no answer. */
    private var effortOverride: AIEffort? = null
    private var lowEffortRetryUsed = false

    /**
     * Ends a reply with an error shown in the chat, where the empty reply bubble
     * was. The user's message stays so they can see what failed.
     */
    private fun failReply(placeholderId: String?, text: String) {
        _messages.value = _messages.value.filterNot { it.id == placeholderId } +
            ChatMessage.aiMessage(content = text, model = "Error", libraryId = _currentLibrary.value?.id)
        _isLoading.value = false
        _errorMessage.value = text
    }

    private fun restartIfStalled(id: String, delayMs: Long) {
        viewModelScope.launch {
            if (delayMs > 0) kotlinx.coroutines.delay(delayMs)
            val reply = activeReply ?: return@launch
            if (reply.id != id || !appInForeground) return@launch
            reply.streamJob?.cancel()
            if (reply.handedOff) backgroundReplies.cancel(reply.id)
            activeReply = null
            if (reply.restarted) {
                failReply(reply.placeholderId, "The reply did not arrive. Try again, or pick another model in Settings.")
                return@launch
            }
            _messages.value = reply.messagesBefore
            Log.d("ChatViewModel", "🔁 Reply stalled; sending it again")
            sendMessage(reply.content, reply.currentCode, reply.threadParentId, isRetry = true)
        }
    }

    /** Replies that finished after the app was closed, shown at the next launch. */
    private fun collectOrphanedReplies() {
        backgroundReplies.store.uncollectedOutcomes().forEach { collectBackgroundReply(it.jobId) }
    }

    /** Stops the reply in progress (the Stop button). */
    fun stopReply() {
        val reply = activeReply ?: return
        reply.streamJob?.cancel()
        if (reply.handedOff) backgroundReplies.cancel(reply.id)
        activeReply = null
        // Keep the question; drop the unfinished answer.
        _messages.value = _messages.value.filterNot { it.id == reply.placeholderId }
        _isLoading.value = false
        Log.d("ChatViewModel", "⏹️ Reply stopped by the user")
    }

    /**
     * While the app is open, a reply that receives nothing for too long is sent
     * again once, then reported, so the spinner never runs on and on.
     */
    private fun monitorStall(id: String, model: String) {
        viewModelScope.launch {
            val limit = if (AIModels.ALL_MODELS.firstOrNull { it.id == model }?.control == com.xraiassistant.data.models.AIModelControl.EFFORT)
                SILENT_THINKING_TIMEOUT_MS else IN_APP_STALL_TIMEOUT_MS
            val startedAt = System.currentTimeMillis()
            while (true) {
                kotlinx.coroutines.delay(5_000)
                val reply = activeReply ?: return@launch
                if (reply.id != id) return@launch
                // Away from the app the background job and checkReplyOnReturn take over.
                if (reply.leftApp || !appInForeground) continue
                if (System.currentTimeMillis() - reply.lastProgress > limit) {
                    restartIfStalled(id, 0)
                    return@launch
                }
                // GLM can think for an hour on a huge request without writing a word.
                // Past the thinking limit, ask once more at low effort; if that also
                // only thinks, stop and suggest a faster model.
                val stillThinking = _messages.value.firstOrNull { it.id == reply.placeholderId }?.content.isNullOrBlank()
                if (stillThinking && thinksBeforeAnswering(model) &&
                    System.currentTimeMillis() - startedAt > GLM_THINKING_LIMIT_MS
                ) {
                    reply.streamJob?.cancel()
                    if (reply.handedOff) backgroundReplies.cancel(reply.id)
                    activeReply = null
                    if (!lowEffortRetryUsed) {
                        Log.w("ChatViewModel", "$model still thinking; asking again with low effort")
                        lowEffortRetryUsed = true
                        effortOverride = AIEffort.LOW
                        _messages.value = reply.messagesBefore
                        sendMessage(reply.content, reply.currentCode, reply.threadParentId, isRetry = true)
                    } else {
                        lowEffortRetryUsed = false
                        failReply(reply.placeholderId, STILL_THINKING_MESSAGE)
                    }
                    return@launch
                }
                // A model that keeps streaming (a reasoning loop, say) is never silent,
                // so it also gets an overall cap. Not retried: it would likely loop again.
                if (hasRunTooLong(startedAt, System.currentTimeMillis())) {
                    reply.streamJob?.cancel()
                    if (reply.handedOff) backgroundReplies.cancel(reply.id)
                    activeReply = null
                    failReply(reply.placeholderId, "Reply took too long. The model kept going without finishing, so it was stopped. Try again, or pick a faster model in Settings.")
                    return@launch
                }
            }
        }
    }

    private companion object {
        const val STALL_TIMEOUT_MS = 20_000L
        const val BACKGROUND_TIMEOUT_MS = 180_000L
        const val IN_APP_STALL_TIMEOUT_MS = 45_000L
        const val SILENT_THINKING_TIMEOUT_MS = 150_000L
    }

    fun sendMessage(content: String, currentCode: String = "", threadParentId: String? = null) {
        // A new message starts with a clean slate for the GLM low-effort retry.
        lowEffortRetryUsed = false
        effortOverride = null
        sendMessage(content, currentCode, threadParentId, isRetry = false)
    }

    private fun sendMessage(content: String, currentCode: String, threadParentId: String?, isRetry: Boolean) {
        if (content.isBlank()) return
        val messagesBefore = _messages.value
        var replyId: String? = null
        var waitingForBackground = false
        // A one-off low-effort retry uses its override once.
        val requestEffort = effortOverride ?: _effort.value
        effortOverride = null

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                // Determine parent ID: use provided threadParentId or current replyToMessageId
                val parentId = threadParentId ?: _replyToMessageId.value

                // Add user message with threading support
                val userMessage = ChatMessage.userMessage(
                    content = content,
                    threadParentId = parentId
                )
                _messages.value = _messages.value + userMessage

                // Clear reply state after sending
                if (parentId != null) {
                    clearReplyTo()
                }

                // Get images from state FIRST (before RAG context)
                val imagesToSend = _selectedImages.value

                // Build RAG context from previous conversations ONLY if no images are present
                // When images are present, they are the primary context, not past conversations
                val ragContext = if (imagesToSend.isEmpty()) {
                    getRAGContext(content)
                } else {
                    Log.d("ChatViewModel", "⚠️ Skipping RAG context - multimodal message with ${imagesToSend.size} image(s)")
                    ""
                }

                // Get current library for context
                val library = _currentLibrary.value
                val enhancedPrompt = buildPrompt(content, currentCode, library, parentId)

                // Create placeholder AI message that will be updated with streaming chunks
                val placeholderMessage = ChatMessage.aiMessage(
                    content = "",
                    model = getModelDisplayName(_selectedModel.value),
                    libraryId = _currentLibrary.value?.id,  // Track which library this message is for
                    threadParentId = parentId,  // AI reply goes in same thread
                    isStreaming = true  // Suppress Run Scene until the response lands
                )
                _messages.value = _messages.value + placeholderMessage
                val messageIndex = _messages.value.lastIndex

                // Collect streaming response
                val fullResponse = StringBuilder()

                // Enhance system prompt with RAG context if available (and no images)
                val enhancedSystemPrompt = if (ragContext.isNotEmpty()) {
                    """
                    ${_systemPrompt.value}

                    $ragContext

                    **Instructions**: Use the context above to inform your responses when relevant. Reference specific examples from previous conversations when applicable. If the context doesn't help answer the question, rely on your general knowledge.
                    """.trimIndent()
                } else {
                    _systemPrompt.value
                }

                // Track the reply so leaving the app hands it to a background job.
                if (imagesToSend.isEmpty()) {
                    val id = java.util.UUID.randomUUID().toString()
                    replyId = id
                    activeReply = ActiveReply(
                        id = id, content = content, currentCode = currentCode, threadParentId = threadParentId,
                        prompt = enhancedPrompt, systemPrompt = enhancedSystemPrompt, model = _selectedModel.value,
                        temperature = _temperature.value.toDouble(), topP = _topP.value.toDouble(),
                        effort = requestEffort.apiValue, placeholderId = placeholderMessage.id,
                        library = library, messagesBefore = messagesBefore,
                        streamJob = coroutineContext[kotlinx.coroutines.Job], restarted = isRetry
                    )
                    monitorStall(id, _selectedModel.value)
                }

                aiProviderRepository.generateResponseStream(
                    prompt = enhancedPrompt,
                    model = _selectedModel.value,
                    temperature = _temperature.value.toDouble(),
                    topP = _topP.value.toDouble(),
                    systemPrompt = enhancedSystemPrompt,
                    effort = requestEffort,
                    images = imagesToSend
                ).collect { chunk ->
                    // Append chunk to full response
                    fullResponse.append(chunk)
                    replyId?.let { id -> if (activeReply?.id == id) activeReply?.lastProgress = System.currentTimeMillis() }

                    // Update the message in real-time. Copy the placeholder rather
                    // than building a new message so the id stays stable: the chat
                    // list keys rows by id, and a fresh id per chunk would rebuild
                    // the row on every chunk.
                    val updatedMessages = _messages.value.toMutableList()
                    // Reasoning (<think>) is not shown; "Thinking…" covers it.
                    updatedMessages[messageIndex] = placeholderMessage.copy(
                        content = com.xraiassistant.domain.text.ReplyText.visible(fullResponse.toString()).text
                    )
                    _messages.value = updatedMessages
                }

                // Already shown by the background job: nothing more to do.
                if (replyId != null && replyId in deliveredByBackground) return@launch
                replyId?.let { id ->
                    if (activeReply?.id == id) {
                        if (activeReply?.handedOff == true) backgroundReplies.cancel(id)
                        activeReply = null
                    }
                }

                // Finished without any answer text (for example a reasoning model
                // that spent its whole budget thinking): say so instead of leaving
                // an empty bubble.
                if (isEmptyReply(fullResponse.toString())) {
                    // GLM thinks and answers from one budget, so on a very large
                    // request it can spend it all thinking: ask once more at low
                    // effort, which keeps the thinking short.
                    if (shouldRetryWithLowEffort(_selectedModel.value, requestEffort, lowEffortRetryUsed)) {
                        Log.w("ChatViewModel", "${_selectedModel.value} used its budget thinking; asking again with low effort")
                        lowEffortRetryUsed = true
                        effortOverride = AIEffort.LOW
                        replyId?.let { id -> if (activeReply?.id == id) activeReply = null }
                        _messages.value = messagesBefore
                        sendMessage(content, currentCode, threadParentId, isRetry = true)
                        return@launch
                    }
                    lowEffortRetryUsed = false
                    failReply(placeholderMessage.id, EMPTY_REPLY_MESSAGE)
                    return@launch
                }
                lowEffortRetryUsed = false

                // Streaming finished: settle the message so Run Scene can appear.
                _messages.value = _messages.value.toMutableList().also { settled ->
                    settled.getOrNull(messageIndex)?.let { streamed ->
                        settled[messageIndex] = streamed.copy(isStreaming = false)
                    }
                }

                // Process complete response for code extraction
                processAIResponse(fullResponse.toString(), library)

                // Auto-save conversation after AI response (matching iOS)
                autoSaveConversation()

                // Index messages for RAG (fire-and-forget, iOS approach)
                // User message had images if imagesToSend was not empty
                val hadImages = imagesToSend.isNotEmpty()
                indexMessageForRAG(userMessage, hadImages)
                val finalAiMessage = _messages.value.getOrNull(messageIndex)
                if (finalAiMessage != null) {
                    // AI message didn't have images (only user messages can have images)
                    indexMessageForRAG(finalAiMessage, hadImages = false)
                }

                // Clear selected images after sending (prevent them from sticking to next message)
                if (imagesToSend.isNotEmpty()) {
                    clearImages()
                    Log.d("ChatViewModel", "✅ Cleared ${imagesToSend.size} image(s) after sending")
                }

            } catch (e: Exception) {
                val id = replyId
                // Delivered by the background job, or superseded by a restart.
                if (id != null && (id in deliveredByBackground || activeReply?.id != id)) return@launch
                if (e is kotlinx.coroutines.CancellationException) return@launch
                if (id != null && activeReply?.handedOff == true && NetworkInterruption.isInterruption(e)) {
                    // The live stream dropped; the background job is still finishing it.
                    Log.w("ChatViewModel", "Stream interrupted; waiting for the background job")
                    waitingForBackground = true
                    return@launch
                }
                if (id != null) activeReply = null
                if (!isRetry && e !is kotlinx.coroutines.CancellationException && NetworkInterruption.isInterruption(e)) {
                    // The connection dropped (app backgrounded or device asleep).
                    // Take back the half-made exchange and send it again once the
                    // app is in front, instead of showing an error.
                    Log.w("ChatViewModel", "Request interrupted (${e.message}); will send again")
                    _messages.value = messagesBefore
                    interruptedRequests.hold {
                        sendMessage(content, currentCode, threadParentId, isRetry = true)
                    }
                    if (appInForeground) interruptedRequests.resume()
                    return@launch
                }
                println("❌ ChatViewModel: Error in sendMessage")
                println("   Error type: ${e.javaClass.simpleName}")
                println("   Error message: ${e.message}")
                e.printStackTrace()

                // Classified rather than matched ad hoc, so the user gets a
                // cause and a next step instead of exception text. Shared
                // taxonomy with the web and iOS clients.
                val info = AIErrorClassifier.classify(e, currentProviderName())
                val errorMsg = info.asMessage()
                _errorMessage.value = errorMsg

                // Also add error message to chat for visibility
                val errorChatMessage = ChatMessage.aiMessage(
                    content = errorMsg,
                    model = "Error",
                    libraryId = _currentLibrary.value?.id
                )
                _messages.value = _messages.value + errorChatMessage
            } finally {
                if (!waitingForBackground) _isLoading.value = false
            }
        }
    }

    /**
     * Send message without streaming (fallback for compatibility)
     */
    fun sendMessageNonStreaming(content: String, currentCode: String = "") {
        if (content.isBlank()) return

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                // Add user message
                val userMessage = ChatMessage.userMessage(content)
                _messages.value = _messages.value + userMessage

                // Build RAG context from previous conversations
                val ragContext = getRAGContext(content)

                // Get current library for context
                val library = _currentLibrary.value
                val enhancedPrompt = buildPrompt(content, currentCode, library)

                // Enhance system prompt with RAG context if available
                val enhancedSystemPrompt = if (ragContext.isNotEmpty()) {
                    """
                    ${_systemPrompt.value}

                    $ragContext

                    **Instructions**: Use the context above to inform your responses when relevant. Reference specific examples from previous conversations when applicable. If the context doesn't help answer the question, rely on your general knowledge.
                    """.trimIndent()
                } else {
                    _systemPrompt.value
                }

                // Call AI service (non-streaming)
                val response = aiProviderRepository.generateResponse(
                    prompt = enhancedPrompt,
                    model = _selectedModel.value,
                    temperature = _temperature.value.toDouble(),
                    topP = _topP.value.toDouble(),
                    systemPrompt = enhancedSystemPrompt,
                    effort = _effort.value
                )

                // Add AI response
                val aiMessage = ChatMessage.aiMessage(
                    content = response,
                    model = getModelDisplayName(_selectedModel.value),
                    libraryId = _currentLibrary.value?.id
                )
                _messages.value = _messages.value + aiMessage

                // Load random demo on first AI response (like iOS)
                if (!hasShownFirstResponse) {
                    hasShownFirstResponse = true
                    loadRandomDemoExample()
                }

                // Process response for code extraction
                processAIResponse(response, library)

                // Auto-save conversation after AI response (matching iOS)
                autoSaveConversation()

                // Index messages for RAG (fire-and-forget)
                // Non-streaming doesn't support images, so hadImages is always false
                indexMessageForRAG(userMessage, hadImages = false)
                indexMessageForRAG(aiMessage, hadImages = false)

            } catch (e: Exception) {
                val errorMsg = when {
                    e.message?.contains("401") == true ->
                        "⚠️ Invalid API Key: Please verify your API key in Settings"
                    e.message?.contains("API key not configured") == true ->
                        "⚠️ API Key Required: Please configure your API key in Settings"
                    else ->
                        "Failed to get response: ${e.message ?: "Unknown error"}"
                }
                _errorMessage.value = errorMsg
            } finally {
                _isLoading.value = false
            }
        }
    }

    // MARK: - AI Response Processing

    /**
     * Process AI response and extract code if present
     * Equivalent to iOS code extraction logic
     */
    private fun processAIResponse(response: String, library: Library3D?) {
        println("🔍 Processing AI response for code extraction...")
        println("📏 Response length: ${response.length} characters")
        println("🔍 Response preview (first 200 chars): ${response.take(200)}")

        // CRITICAL FIX: Strip DeepSeek R1 reasoning tags before code extraction
        // DeepSeek R1 uses <think>...</think> tags for chain-of-thought reasoning
        var cleanedResponse = response.replace("<think>[\\s\\S]*?</think>".toRegex(), "").trim()

        if (cleanedResponse != response) {
            println("✅ Stripped <think> reasoning tags from AI response")
            println("📏 Cleaned response length: ${cleanedResponse.length} characters")
        }

        // CRITICAL FIX: Decode unicode escapes (Gemini returns \u003e instead of >)
        val beforeUnicode = cleanedResponse
        cleanedResponse = cleanedResponse
            .replace("\\u003e", ">")
            .replace("\\u003c", "<")
            .replace("\\u0026", "&")
            .replace("\\u0027", "'")
            .replace("\\u0022", "\"")

        if (beforeUnicode != cleanedResponse) {
            println("✅ Decoded unicode escapes in response")
            println("🔍 First 300 chars after decoding: ${cleanedResponse.take(300)}")
        }

        // Multiple code extraction patterns (in order of preference)
        // CRITICAL FIX: Gemini returns INSERT_CODE with ```/INSERT_CODE closing marker

        // Pattern 0: INSERT_CODE```...```/INSERT_CODE (Gemini actual format with /INSERT_CODE closing)
        val geminiWithSlashPattern = "INSERT_CODE```(?:javascript|typescript|html)?\\s*([\\s\\S]*?)```/INSERT_CODE".toRegex()
        var codeMatch = geminiWithSlashPattern.find(cleanedResponse)

        if (codeMatch != null) {
            val extractedCode = codeMatch.groupValues[1].trim()
            println("✅ Code extracted via GEMINI-SLASH pattern (${extractedCode.length} chars)")
            injectCode(extractedCode, library)
            return
        }

        // Pattern 1: INSERT_CODE```...``` (Gemini format with closing backticks but no /INSERT_CODE yet)
        val geminiNoBracketsPattern = "INSERT_CODE```(?:javascript|typescript|html)?\\s*([\\s\\S]*?)```(?!/INSERT_CODE)".toRegex()
        codeMatch = geminiNoBracketsPattern.find(cleanedResponse)

        if (codeMatch != null) {
            val extractedCode = codeMatch.groupValues[1].trim()
            println("✅ Code extracted via GEMINI-NO-BRACKETS pattern (${extractedCode.length} chars)")
            if (extractedCode.length > 100) {  // Higher threshold to avoid incomplete code
                injectCode(extractedCode, library)
                return
            } else {
                println("⚠️ Code too short (${extractedCode.length} chars), trying next pattern")
            }
        }

        // Pattern 2: INSERT_CODE```... (Gemini streams code mixed with explanation)
        // CRITICAL FIX: Extract everything, then clean up explanation text manually
        if (cleanedResponse.contains("INSERT_CODE```")) {
            println("🔍 Found INSERT_CODE marker, attempting extraction...")

            // Find the start of the code block
            val codeStartIndex = cleanedResponse.indexOf("INSERT_CODE```")
            if (codeStartIndex != -1) {
                // Extract from INSERT_CODE to end
                val afterMarker = cleanedResponse.substring(codeStartIndex + "INSERT_CODE```".length)

                // Skip the language identifier (javascript, typescript, html)
                val codeStart = if (afterMarker.startsWith("javascript") ||
                                   afterMarker.startsWith("typescript") ||
                                   afterMarker.startsWith("html")) {
                    afterMarker.indexOf("\n") + 1
                } else {
                    0
                }

                val potentialCode = afterMarker.substring(codeStart)
                println("🔍 Potential code length: ${potentialCode.length} chars")
                println("🔍 Potential code sample: ${potentialCode.take(300)}")
                println("🔍 Potential code END sample: ...${potentialCode.takeLast(300)}")

                // Debug: Check what markers exist in the response
                val hasBacktickClose = potentialCode.contains("```/INSERT_CODE")
                val hasSlashClose = potentialCode.contains("/INSERT_CODE")
                val hasRunScene = potentialCode.contains("[RUN_SCENE]")
                val hasMarkdownHeader = potentialCode.contains("\n###") || potentialCode.contains("### ")

                println("🔍 Markers present: ```/INSERT=$hasBacktickClose, /INSERT=$hasSlashClose, RUN_SCENE=$hasRunScene, ###=$hasMarkdownHeader")

                // CRITICAL FIX: Search for closing markers more carefully
                var endIndex = -1

                // PRIORITY 1: Look for ```/INSERT_CODE (proper closing with backticks)
                val marker1 = potentialCode.indexOf("```/INSERT_CODE")
                println("🔍 Searching for ```/INSERT_CODE: $marker1")
                if (marker1 >= 0) {
                    endIndex = marker1
                    println("✅ Found ```/INSERT_CODE at position $marker1 (USING THIS)")
                }

                // PRIORITY 2: Look for /INSERT_CODE without backticks
                if (endIndex == -1) {
                    val marker2 = potentialCode.indexOf("/INSERT_CODE")
                    println("🔍 Searching for /INSERT_CODE: $marker2")
                    if (marker2 >= 0) {
                        endIndex = marker2
                        println("✅ Found /INSERT_CODE at position $marker2 (USING THIS)")
                    }
                }

                // PRIORITY 3: Look for [RUN_SCENE] command
                if (endIndex == -1) {
                    val marker3 = potentialCode.indexOf("[RUN_SCENE]")
                    println("🔍 Searching for [RUN_SCENE]: $marker3")
                    if (marker3 >= 0) {
                        endIndex = marker3
                        println("✅ Found [RUN_SCENE] at position $marker3 (USING THIS)")
                    }
                }

                // PRIORITY 4: Look for ### headers (try both with and without newline)
                if (endIndex == -1) {
                    val marker4a = potentialCode.indexOf("\n###")
                    val marker4b = potentialCode.indexOf("### ")
                    val marker4 = when {
                        marker4a >= 0 && marker4b >= 0 -> minOf(marker4a, marker4b)
                        marker4a >= 0 -> marker4a
                        marker4b >= 0 -> marker4b
                        else -> -1
                    }
                    println("🔍 Searching for ### header: $marker4")
                    if (marker4 >= 0) {
                        endIndex = marker4
                        println("✅ Found ### header at position $marker4 (USING THIS)")
                    }
                }

                // PRIORITY 5 (LAST RESORT): Look for numbered markdown lists
                if (endIndex == -1) {
                    val explanationPattern = "\\n\\d+\\.\\s+\\*\\*".toRegex()
                    val explanationMatch = explanationPattern.find(potentialCode)
                    println("🔍 Searching for numbered list pattern: ${explanationMatch?.range?.first}")
                    if (explanationMatch != null) {
                        endIndex = explanationMatch.range.first
                        println("⚠️ Found numbered list at position ${explanationMatch.range.first} (LAST RESORT)")
                    }
                }

                // If still no marker found, use entire potential code
                if (endIndex == -1) {
                    endIndex = potentialCode.length
                    println("⚠️ No end markers found, using entire potential code (${endIndex} chars)")
                }

                val extractedCode = potentialCode.substring(0, endIndex).trim()

                println("✅ Code extracted via GEMINI manual parsing (${extractedCode.length} chars)")
                println("📝 Code preview (first 200 chars): ${extractedCode.take(200)}")
                println("📝 Code preview (last 200 chars): ...${extractedCode.takeLast(200)}")

                if (extractedCode.length > 100) {
                    injectCode(extractedCode, library)
                    return
                } else {
                    println("⚠️ Code too short (${extractedCode.length} chars), trying next pattern")
                }
            }
        }

        // Pattern 2: [INSERT_CODE]```...```[/INSERT_CODE] (with brackets and closing tag)
        val primaryPattern = "\\[INSERT_CODE\\]```(?:javascript|typescript|html)?\\s*([\\s\\S]*?)```\\[/INSERT_CODE\\]".toRegex()
        codeMatch = primaryPattern.find(cleanedResponse)

        if (codeMatch != null) {
            val extractedCode = codeMatch.groupValues[1].trim()
            println("✅ Code extracted via PRIMARY pattern (${extractedCode.length} chars)")
            injectCode(extractedCode, library)
            return
        }

        // Pattern 3: [INSERT_CODE]```...``` (with brackets, Gemini format with closing backticks)
        val geminiWithBracketsPattern = "\\[INSERT_CODE\\]```(?:javascript|typescript|html)?\\s*([\\s\\S]*?)```".toRegex()
        codeMatch = geminiWithBracketsPattern.find(cleanedResponse)

        if (codeMatch != null) {
            val extractedCode = codeMatch.groupValues[1].trim()
            println("✅ Code extracted via GEMINI-BRACKETS pattern (${extractedCode.length} chars)")
            if (extractedCode.length > 20) {
                injectCode(extractedCode, library)
                return
            } else {
                println("⚠️ Code too short (${extractedCode.length} chars), trying next pattern")
            }
        }

        // Pattern 4: [INSERT_CODE]```... (with brackets, no closing ``` yet - incomplete stream)
        val geminiIncompletePattern = "\\[INSERT_CODE\\]```(?:javascript|typescript|html)?\\s*([\\s\\S]+?)(?:```|$)".toRegex()
        codeMatch = geminiIncompletePattern.find(cleanedResponse)

        if (codeMatch != null) {
            val extractedCode = codeMatch.groupValues[1].trim()
            println("✅ Code extracted via GEMINI-BRACKETS-INCOMPLETE pattern (${extractedCode.length} chars)")
            if (extractedCode.length > 20) {
                injectCode(extractedCode, library)
                return
            } else {
                println("⚠️ Code too short (${extractedCode.length} chars), waiting for more content")
            }
        }

        // Pattern 2: ```javascript or ```typescript or ```html blocks
        val languagePattern = "```(?:javascript|typescript|html|js|ts)\\s*([\\s\\S]*?)```".toRegex()
        codeMatch = languagePattern.find(cleanedResponse)

        if (codeMatch != null) {
            val extractedCode = codeMatch.groupValues[1].trim()
            println("✅ Code extracted via LANGUAGE pattern (${extractedCode.length} chars)")

            if (extractedCode.length > 50) {
                injectCode(extractedCode, library)
                return
            } else {
                println("⚠️ Code too short (${extractedCode.length} chars), trying next pattern")
            }
        }

        // Pattern 3: Any ``` code block (fallback)
        val anyCodePattern = "```\\s*([\\s\\S]*?)```".toRegex()
        codeMatch = anyCodePattern.find(cleanedResponse)

        if (codeMatch != null) {
            val extractedCode = codeMatch.groupValues[1].trim()
            println("✅ Code extracted via GENERIC pattern (${extractedCode.length} chars)")

            if (extractedCode.length > 50) {
                injectCode(extractedCode, library)
                return
            } else {
                println("⚠️ Code too short (${extractedCode.length} chars), skipping injection")
            }
        }

        // If we get here, no code was found
        println("⚠️ No code blocks found in AI response using any pattern")
        println("🔍 Searched for patterns:")
        println("   0. INSERT_CODE```...```/INSERT_CODE (Gemini with /INSERT_CODE)")
        println("   1. INSERT_CODE```...``` (Gemini - no /INSERT_CODE)")
        println("   2. INSERT_CODE```... (Gemini - incomplete)")
        println("   3. [INSERT_CODE]```...```[/INSERT_CODE] (with brackets)")
        println("   4. [INSERT_CODE]```...``` (with brackets)")
        println("   5. ```javascript|typescript|html...```")
        println("   6. ```...```")
        println("🔍 Cleaned response preview (first 500 chars):")
        println(cleanedResponse.take(500))

        // Check for commands even if no code found
        if (cleanedResponse.contains("[RUN_SCENE]")) {
            println("✅ Found [RUN_SCENE] command")
            onRunScene?.invoke()
        }

        if (cleanedResponse.contains("[DESCRIBE_SCENE]")) {
            println("✅ Found [DESCRIBE_SCENE] command")
            onDescribeScene?.invoke(cleanedResponse)
        }
    }

    /**
     * Helper function to inject extracted code
     */
    private fun injectCode(code: String, library: Library3D?) {
        _lastGeneratedCode.value = code
        lastCodeLibraryId = library?.id
        _codeReadyNotice.value = true

        if (library?.requiresBuild == true) {
            println("🏗️ Library requires build, calling onInsertCodeWithBuild")
            onInsertCodeWithBuild?.invoke(code, library)
        } else {
            println("📝 Direct injection, calling onInsertCode")
            onInsertCode?.invoke(code)
        }

        // Check for run scene command
        if (_lastGeneratedCode.value.contains("[RUN_SCENE]")) {
            println("✅ Found [RUN_SCENE] command")
            onRunScene?.invoke()
        }
    }

    /**
     * Run code from a chat message
     * Public method for "Run Scene" button functionality
     * Equivalent to iOS onRun callback in ThreadedMessageView
     *
     * UPDATED: Uses CodeSandbox API for React Three Fiber, Babel for other libraries
     */
    fun runCodeFromMessage(code: String, libraryId: String?) {
        println("🎯 Running code from message (${code.length} chars) with library: ${libraryId ?: "current"}")

        // If libraryId is specified, switch to that library first
        val targetLibrary = if (libraryId != null) {
            val library = library3DRepository.getLibraryById(libraryId)
            if (library != null && library.id != _currentLibrary.value?.id) {
                // Switch to the target library
                println("🔄 Switching from ${_currentLibrary.value?.displayName} to ${library.displayName}")
                _currentLibrary.value = library

                // Clear CodeSandbox URL when switching away from React-based libraries
                if (library.id != "reactThreeFiber" && library.id != "reactylon") {
                    _sandboxUrl.value = null
                    println("🧹 Cleared CodeSandbox URL when switching to ${library.displayName}")
                }
            }
            library
        } else {
            _currentLibrary.value
        }

        // IMPORTANT: Use CodeSandbox for React-based libraries (React Three Fiber, Reactylon), regular injection for others
        if (targetLibrary?.id == "reactThreeFiber" || targetLibrary?.id == "reactylon") {
            println("🏗️ ${targetLibrary.displayName} detected - using CodeSandbox build")
            buildWithCodeSandbox(code, libraryId)
        } else {
            // For other libraries (BabylonJS, Three.js, A-Frame), use direct injection
            println("📝 Using direct injection for ${targetLibrary?.displayName}")
            injectCode(code, targetLibrary)
            // Automatically trigger run scene
            println("✅ Code injected, triggering scene run")
            onRunScene?.invoke()
        }
    }

    /**
     * Build code with CodeSandbox API
     *
     * Creates a CodeSandbox sandbox for React-based libraries (React Three Fiber, Reactylon) and returns the preview URL.
     * This replaces client-side Babel transpilation with server-side bundling.
     *
     * @param code The React component code (React Three Fiber or Reactylon)
     * @param libraryId The library ID (defaults to current library)
     * @return CodeSandbox preview URL (e.g., https://codesandbox.io/s/abc123)
     */
    fun buildWithCodeSandbox(code: String, libraryId: String? = null) {
        viewModelScope.launch {
            try {
                _isBuildingCode.value = true
                _statusMessage.value = "🏗️ Building with CodeSandbox..."
                println("🏗️ Building code with CodeSandbox (${code.length} chars)")

                // Determine which library to use
                val library = libraryId?.let { library3DRepository.getLibraryById(it) }
                    ?: _currentLibrary.value

                // Create sandbox files based on library type
                val files = when (library?.id) {
                    "reactThreeFiber" -> CodeSandboxTemplates.createReactThreeFiberSandbox(code)
                    "reactylon" -> CodeSandboxTemplates.createReactylonSandbox(code)
                    "threejs" -> CodeSandboxTemplates.createThreeJSSandbox(code)
                    "aframe" -> CodeSandboxTemplates.createAFrameSandbox(code)
                    else -> {
                        println("⚠️ Library ${library?.id} not supported for CodeSandbox, defaulting to React Three Fiber")
                        CodeSandboxTemplates.createReactThreeFiberSandbox(code)
                    }
                }

                // Create the request
                val request = CodeSandboxDefineRequest(files)

                // Call CodeSandbox API
                println("📡 Calling CodeSandbox Define API...")
                val response = codeSandboxService.createSandbox(request = request)

                // Construct preview URL - use /embed/ for lighter WebView-compatible version
                val sandboxId = response.sandboxId ?: response.id
                if (sandboxId != null) {
                    // Use embed URL which is optimized for iframes and WebViews
                    val previewUrl = "https://codesandbox.io/embed/$sandboxId?view=preview&hidenavigation=1"
                    _sandboxUrl.value = previewUrl
                    _lastGeneratedCode.value = code

                    println("✅ CodeSandbox created successfully!")
                    println("🔗 Sandbox URL: $previewUrl")
                    _statusMessage.value = "✅ Build complete! Loading preview..."

                    // Keep loading indicator visible for a few more seconds while WebView loads
                    // This prevents ANR by showing user that something is happening
                    viewModelScope.launch {
                        delay(3000) // Give WebView time to start loading
                        _isBuildingCode.value = false
                        _statusMessage.value = null
                    }
                } else {
                    throw Exception("CodeSandbox API returned no sandbox ID")
                }

            } catch (e: Exception) {
                println("❌ CodeSandbox build failed: ${e.message}")
                e.printStackTrace()
                _errorMessage.value = "Build failed: ${e.message}"
                _statusMessage.value = null
                _isBuildingCode.value = false
            }
        }
    }

    /**
     * Load a random demo example
     * Public method for "Run Demo" button functionality
     * Equivalent to iOS welcome message with demo code
     */
    fun loadRandomDemoExample(libraryId: String? = null) {
        // If libraryId is specified, switch to that library first
        val targetLibrary = if (libraryId != null) {
            val library = library3DRepository.getLibraryById(libraryId)
            if (library != null && library.id != _currentLibrary.value?.id) {
                // Switch to the target library
                println("🔄 Switching to ${library.displayName} for demo")

                // Clear CodeSandbox URL when switching away from React Three Fiber
                if (library.id != "reactThreeFiber" && _sandboxUrl.value != null) {
                    _sandboxUrl.value = null
                    println("🧹 [loadRandomDemoExample] Cleared CodeSandbox URL when switching to ${library.displayName}")
                }

                _currentLibrary.value = library
            }
            library
        } else {
            _currentLibrary.value
        }

        if (targetLibrary == null) return
        println("🎲 Loading random demo for library: ${targetLibrary.displayName}")

        // Get a random example from the current library
        val examples = targetLibrary.examples
        if (examples.isNotEmpty()) {
            val randomExample = welcomeExample?.takeIf { it in examples } ?: examples.random()
            println("✨ Selected random example: ${randomExample.title}")

            // Inject the example code
            injectCode(randomExample.code, targetLibrary)

            // Auto-run the scene to show the demo
            println("▶️ Auto-running random demo example")
            onRunScene?.invoke()
        } else {
            println("⚠️ No examples available for ${targetLibrary.displayName}")
        }
    }

    // MARK: - AI Parameter Management

    /**
     * Get parameter description based on current settings
     * Equivalent to iOS getParameterDescription()
     */
    fun getParameterDescription(): String {
        val temp = _temperature.value.toDouble()
        val topP = _topP.value.toDouble()

        return when {
            temp in 0.0..0.3 && topP in 0.1..0.5 -> "Precise & Focused - Perfect for debugging"
            temp in 0.4..0.8 && topP in 0.6..0.9 -> "Balanced Creativity - Ideal for most scenes"
            temp in 0.9..2.0 && topP in 0.9..1.0 -> "Experimental Mode - Maximum innovation"
            else -> "Custom Configuration"
        }
    }

    /**
     * Update AI parameters
     */
    fun updateTemperature(value: Double) {
        _temperature.value = value.toFloat().coerceIn(0.0f, 2.0f)
    }

    fun updateTopP(value: Double) {
        _topP.value = value.toFloat().coerceIn(0.1f, 1.0f)
    }

    fun updateSystemPrompt(prompt: String) {
        _systemPrompt.value = prompt
    }

    fun updateSelectedModel(modelId: String) {
        selectedModel = modelId
    }

    // MARK: - 3D Library Management

    /**
     * Select 3D library and update system prompt
     * Matches iOS selectLibrary() behavior
     */
    // React Three Fiber and Reactylon have no in-app playground; say so when picked.
    private val _codeSandboxNotice = MutableStateFlow<String?>(null)
    val codeSandboxNotice: StateFlow<String?> = _codeSandboxNotice.asStateFlow()

    fun dismissCodeSandboxNotice() { _codeSandboxNotice.value = null }

    fun selectLibrary(libraryId: String) {
        if (requiresCodeSandbox(libraryId) && libraryId != currentLibraryId) {
            _codeSandboxNotice.value = codeSandboxNoticeFor(libraryId)
        }
        applyLibrary(libraryId, resetSystemPrompt = true)
        persistSettings()
    }

    /**
     * Switches library without touching storage. Synchronous on purpose: callers
     * that restore a saved system prompt afterwards must not race a coroutine that
     * resets it to the library default, which is how saved prompts were lost.
     */
    private fun applyLibrary(libraryId: String, resetSystemPrompt: Boolean) {
        val library = library3DRepository.getLibraryById(libraryId)

        // Clear CodeSandbox URL when switching away from React Three Fiber
        if (libraryId != "reactThreeFiber" && _sandboxUrl.value != null) {
            _sandboxUrl.value = null
            println("🧹 [selectLibrary] Cleared CodeSandbox URL when switching to ${library?.displayName}")
        }

        _currentLibrary.value = library

        library?.let {
            welcomeExample = it.examples.randomOrNull()

            // Update system prompt with library's default
            if (resetSystemPrompt) _systemPrompt.value = it.systemPrompt

            // Update welcome message if it exists
            if (_messages.value.isNotEmpty()) {
                val updatedMessages = _messages.value.toMutableList()
                updatedMessages[0] = ChatMessage(
                    id = updatedMessages[0].id,
                    content = it.getWelcomeMessage(welcomeExample),
                    isUser = false,
                    timestamp = updatedMessages[0].timestamp,
                    libraryId = it.id,  // FIXED: Preserve library ID
                    isWelcomeMessage = true  // FIXED: Mark as welcome message
                )
                _messages.value = updatedMessages
                println("📨 Updated welcome message: isWelcomeMessage=true, libraryId=${it.id}")
            }

            println("🎯 Switched to ${it.displayName}")
            println("📊 System prompt updated (${_systemPrompt.value.length} characters)")
        }
    }

    fun getCurrentLibrary(): Library3D {
        return _currentLibrary.value ?: library3DRepository.getDefaultLibrary()
    }

    fun getAvailableLibraries(): List<Library3D> = library3DRepository.getAllLibraries()
    
    /**
     * Get model description for legacy compatibility
     */
    fun getModelDescription(modelId: String): String {
        return AIModels.ALL_MODELS.find { it.id == modelId }?.description ?: "AI Model"
    }

    // MARK: - Settings Management

    /**
     * Save all settings to persistent storage
     * Equivalent to iOS saveSettings()
     */
    /**
     * Applies the values from the Settings screen in one step and saves them. The
     * library is applied without resetting the system prompt, so the prompt the
     * user typed is the one that is kept.
     */
    suspend fun applySettings(
        model: String,
        libraryId: String,
        temperature: Float,
        topP: Float,
        systemPrompt: String
    ) {
        _selectedModel.value = model
        if (libraryId.isNotEmpty() && libraryId != _currentLibrary.value?.id) {
            applyLibrary(libraryId, resetSystemPrompt = false)
        }
        this.temperature = temperature
        this.topP = topP
        _systemPrompt.value = systemPrompt
        saveSettings()
    }

    /** Saves current settings in the background, once saved ones have been loaded. */
    private fun persistSettings() {
        if (!settingsLoaded) return
        viewModelScope.launch { saveSettings() }
    }

    suspend fun saveSettings() {
        settingsRepository.saveSettings(
            selectedModel = _selectedModel.value,
            temperature = _temperature.value.toDouble(),
            topP = _topP.value.toDouble(),
            systemPrompt = _systemPrompt.value,
            selectedLibraryId = _currentLibrary.value?.id,
            effort = _effort.value.apiValue
        )
    }

    /**
     * Load settings from persistent storage
     * Matches iOS loadSettings() behavior
     */
    private fun loadSettings() {
        viewModelScope.launch {
            val settings = settingsRepository.getSettings()
            _selectedModel.value = com.xraiassistant.data.models.ModelMigrations.resolve(settings.selectedModel)
            _temperature.value = settings.temperature.toFloat()
            _topP.value = settings.topP.toFloat()
            _effort.value = AIEffort.fromApiValue(settings.effort)

            // Library first, so restoring it cannot overwrite the saved prompt below.
            settings.selectedLibraryId?.let { libraryId ->
                applyLibrary(libraryId, resetSystemPrompt = true)
            }

            // Only override system prompt if a custom one was saved (matching iOS)
            if (settings.systemPrompt.isNotEmpty()) {
                _systemPrompt.value = settings.systemPrompt
                println("📝 Loaded custom system prompt (${settings.systemPrompt.length} characters)")
            } else {
                println("📝 No custom system prompt saved, using library default (${_systemPrompt.value.length} characters)")
            }

            settingsLoaded = true
            println("✅ Settings loaded successfully")
        }
    }

    private fun loadDefaultLibrary() {
        viewModelScope.launch {
            val defaultLibrary = library3DRepository.getDefaultLibrary()
            _currentLibrary.value = defaultLibrary
            _systemPrompt.value = defaultLibrary.systemPrompt
        }
    }

    // MARK: - Provider Management

    /**
     * Check if a provider is configured with API key
     */
    fun isProviderConfigured(provider: String): Boolean {
        return aiProviderRepository.isProviderConfigured(provider)
    }

    /**
     * Set API key for provider
     * Made suspend to ensure caller can await completion
     */
    suspend fun setAPIKey(provider: String, key: String) {
        aiProviderRepository.setAPIKey(provider, key.trim())
        if (provider == "Together.ai") refreshTogetherModels(force = true)
    }

    /**
     * Get API key for provider (masked for display)
     */
    fun getAPIKey(provider: String): String {
        return aiProviderRepository.getAPIKey(provider)
    }

    /**
     * Get raw API key for provider (for editing in settings)
     */
    fun getRawAPIKey(provider: String): String {
        return aiProviderRepository.getRawAPIKey(provider)
    }

    // MARK: - Chat History Management

    /**
     * Auto-save conversation after AI response
     * Equivalent to iOS autoSaveConversation() in EnhancedChatView.swift
     */
    private fun autoSaveConversation() {
        // Only save if we have messages
        if (_messages.value.isEmpty()) return

        viewModelScope.launch {
            try {
                val messages = _messages.value
                val libraryId = _currentLibrary.value?.id
                val modelUsed = getModelDisplayName(_selectedModel.value)

                if (currentConversationId == null) {
                    // New conversation - create it
                    val conversationId = conversationRepository.saveConversation(
                        messages = messages,
                        libraryId = libraryId,
                        modelUsed = modelUsed
                    )
                    currentConversationId = conversationId
                    println("💾 Auto-saved new conversation: $conversationId")
                } else {
                    // Update existing conversation
                    conversationRepository.updateConversation(
                        conversationId = currentConversationId!!,
                        messages = messages,
                        libraryId = libraryId,
                        modelUsed = modelUsed
                    )
                    println("💾 Auto-updated conversation: $currentConversationId")
                }
            } catch (e: Exception) {
                println("⚠️ Failed to auto-save conversation: ${e.message}")
            }
        }
    }

    /**
     * Load conversation from history
     * Equivalent to iOS loadConversation() in EnhancedChatView.swift
     */
    fun loadConversation(conversationId: String) {
        viewModelScope.launch {
            try {
                val (conversation, messageEntities) = conversationRepository.getConversationWithMessages(conversationId)

                if (conversation != null && messageEntities.isNotEmpty()) {
                    // Convert entities to ChatMessage
                    val messages = messageEntities.map { entity ->
                        ChatMessage(
                            id = entity.id,
                            content = entity.content,
                            isUser = entity.isUser,
                            timestamp = java.util.Date(entity.timestamp),
                            model = entity.model,
                            libraryId = entity.libraryId,
                            isWelcomeMessage = false
                        )
                    }

                    // Update current state
                    _messages.value = messages
                    currentConversationId = conversationId

                    // Switch to the library used in conversation if different
                    conversation.library3DID?.let { libraryId ->
                        if (libraryId != _currentLibrary.value?.id) {
                            selectLibrary(libraryId)
                        }
                    }

                    println("📂 Loaded conversation: ${conversation.title}")
                }
            } catch (e: Exception) {
                println("⚠️ Failed to load conversation: ${e.message}")
            }
        }
    }

    /**
     * Start new conversation
     * Saves current conversation and clears state
     * Equivalent to iOS "New Conversation" in EnhancedChatView.swift
     */
    fun newConversation() {
        viewModelScope.launch {
            // Save current conversation if it has messages
            if (_messages.value.isNotEmpty()) {
                autoSaveConversation()
            }

            // Clear state
            _messages.value = emptyList()
            currentConversationId = null
            hasShownFirstResponse = false
            _lastGeneratedCode.value = ""
            _codeReadyNotice.value = false

            // Show welcome message for current library
            setupInitialMessage()

            println("🆕 Started new conversation")
        }
    }

    /**
     * Clear all saved conversations
     * Equivalent to iOS "Clear All History" in Settings
     */
    fun clearAllConversations() {
        viewModelScope.launch {
            try {
                conversationRepository.deleteAllConversations()
                println("🗑️ Cleared all conversations from Settings")
            } catch (e: Exception) {
                println("⚠️ Failed to clear all conversations: ${e.message}")
            }
        }
    }

    /**
     * Save screenshot to current conversation
     * Called after canvas screenshot is captured in SceneScreen (5 seconds after code injection)
     */
    fun saveConversationScreenshot(screenshotBase64: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val conversationId = currentConversationId ?: run {
                    println("⚠️ Cannot save screenshot - no active conversation")
                    return@launch
                }

                conversationRepository.updateConversationScreenshot(
                    conversationId = conversationId,
                    screenshotBase64 = screenshotBase64
                )

                println("✅ Screenshot saved to conversation: $conversationId (${screenshotBase64.length} characters)")
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to save screenshot: ${e.message}", e)
            }
        }
    }

    // MARK: - Helper Functions

    /**
     * Get display name for model ID
     */
    /** Provider behind the selected model, for error messages. */
    private fun currentProviderName(): String =
        AIModels.ALL_MODELS.firstOrNull { it.id == _selectedModel.value }?.provider
            ?: "The AI provider"

    /** Provider name for a model id (as used by isProviderConfigured), or null if unknown. */
    fun getModelProvider(modelId: String): String? =
        AIModels.ALL_MODELS.find { it.id == modelId }?.provider
            // The key other Together models are grouped apart but use the Together key.
            ?: _togetherModels.value.find { it.id == modelId }?.let { "Together.ai" }

    fun getModelDisplayName(modelId: String): String {
        if (modelId.startsWith(com.xraiassistant.domain.local.LocalServerConfig.MODEL_PREFIX)) {
            return com.xraiassistant.domain.local.LocalServerConfig.serverModelName(modelId)
        }
        return AIModels.ALL_MODELS.find { it.id == modelId }?.displayName
            ?: _togetherModels.value.find { it.id == modelId }?.displayName
            ?: modelId
    }

    /** Address and model of the user's own server, edited in Settings. */
    val localServer get() = aiProviderRepository.localServer

    /** All available models grouped by provider, plus Local once it is set up. */
    val modelsByProvider: Map<String, List<AIModel>>
        get() {
            // Together comes from the live list for the key (curated picks first,
            // the key other models in their own group); the rest are built in.
            val together = togetherModels.value.groupBy { it.provider }
            val others = AIModels.MODELS_BY_PROVIDER.filterKeys { it != "Together.ai" }
            val base = together + others
            return aiProviderRepository.localModel()?.let { base + (it.provider to listOf(it)) } ?: base
        }

    /**
     * Fetches the chat models this Together key can use, so the picker only offers
     * models that will answer. Runs when the key is saved and once a day at launch.
     */
    fun refreshTogetherModels(force: Boolean = false) {
        togetherRefresh?.cancel()
        togetherRefresh = viewModelScope.launch {
            // The Settings field saves on every keystroke: wait until typing stops.
            if (force) kotlinx.coroutines.delay(1_200)
            if (!aiProviderRepository.refreshTogetherModels(force)) return@launch
            val models = aiProviderRepository.togetherModels()
            _togetherModels.value = models
            // A Together model the key can no longer use: move to one it can.
            val selected = _selectedModel.value
            if (selected.contains("/") && models.none { it.id == selected }) {
                val fallback = models.firstOrNull { it.id == com.xraiassistant.data.models.ModelMigrations.DEFAULT_MODEL } ?: models.firstOrNull()
                if (fallback != null) {
                    Log.w("ChatViewModel", "$selected is not available to this key; using ${fallback.id}")
                    updateSelectedModel(fallback.id)
                }
            }
        }
    }
    
    /**
     * Get all available models (flat list)
     */
    val allAvailableModels: List<AIModel>
        get() = AIModels.ALL_MODELS
    
    /**
     * Get available models (legacy compatibility)
     */
    val availableModels: List<String>
        get() = AIModels.ALL_MODELS.map { it.id }

    /**
     * Build enhanced prompt with library context
     */
    private fun buildPrompt(
        userMessage: String,
        currentCode: String,
        library: Library3D?,
        parentId: String? = null
    ): String {
        val libraryContext = library?.let {
            "Current 3D Library: ${it.displayName} (${it.version})\n" +
            "Language: ${it.codeLanguage}\n"
        } ?: ""

        val codeContext = if (currentCode.isNotBlank()) {
            "Current code in editor:\n```\n$currentCode\n```\n\n"
        } else ""

        // Build thread context if this is a reply
        val threadContext = if (parentId != null) {
            buildThreadContext(parentId)
        } else ""

        return "$libraryContext$codeContext$threadContext$userMessage"
    }

    /**
     * Build thread context for AI - includes parent message and all replies
     * This gives the AI full context of the conversation thread
     */
    private fun buildThreadContext(parentId: String): String {
        val messages = _messages.value
        val parentMessage = messages.firstOrNull { it.id == parentId } ?: return ""

        val threadBuilder = StringBuilder()
        threadBuilder.append("--- Thread Context ---\n")
        threadBuilder.append("Original message:\n")
        threadBuilder.append("${if (parentMessage.isUser) "User" else "Assistant"}: ${parentMessage.content}\n\n")

        // Get all replies in this thread (chronological order)
        val replies = messages.filter { it.threadParentId == parentId }
            .sortedBy { it.timestamp }

        if (replies.isNotEmpty()) {
            threadBuilder.append("Previous replies in this thread:\n")
            replies.forEach { reply ->
                threadBuilder.append("${if (reply.isUser) "User" else "Assistant"}: ${reply.content}\n")
            }
            threadBuilder.append("\n")
        }

        threadBuilder.append("--- End Thread Context ---\n")
        threadBuilder.append("Your reply:\n")

        return threadBuilder.toString()
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    // MARK: - UI State Management
    
    /**
     * Update current view
     */
    fun updateCurrentView(view: AppView) {
        _uiState.value = _uiState.value.copy(currentView = view)
        if (view == AppView.SCENE) {
            _codeReadyNotice.value = false
            // Nothing to show yet, or code left from another library (which that
            // playground cannot run): play the welcome message's demo instead.
            val library = _currentLibrary.value
            if (library != null && (_lastGeneratedCode.value.isBlank() || lastCodeLibraryId != library.id)) {
                loadRandomDemoExample(library.id)
            }
        }
    }

    fun dismissCodeReadyNotice() {
        _codeReadyNotice.value = false
    }
    
    /**
     * Show settings modal
     */
    fun showSettings() {
        _uiState.value = _uiState.value.copy(showSettings = true)
    }
    
    /**
     * Hide settings modal
     */
    fun hideSettings() {
        _uiState.value = _uiState.value.copy(showSettings = false, settingsSaved = false)
    }
    
    /**
     * Set code injection loading state
     */
    fun setCodeInjecting(isInjecting: Boolean) {
        _uiState.value = _uiState.value.copy(isInjectingCode = isInjecting)
    }
    
    /**
     * Set WebView ready state
     */
    fun setWebViewReady(ready: Boolean) {
        _uiState.value = _uiState.value.copy(webViewReady = ready)
    }
    
    /**
     * Show settings saved confirmation
     */
    fun showSettingsSaved() {
        _uiState.value = _uiState.value.copy(settingsSaved = true)
    }

    // MARK: - Threading Methods

    /**
     * Toggle thread expansion for a specific message
     * Equivalent to iOS onToggleThread callback
     */
    fun toggleThread(messageId: String) {
        _expandedThreads.value = if (_expandedThreads.value.contains(messageId)) {
            _expandedThreads.value - messageId
        } else {
            _expandedThreads.value + messageId
        }
    }

    /**
     * Set which message the user is replying to
     * Equivalent to iOS onReply callback
     */
    fun setReplyTo(messageId: String) {
        _replyToMessageId.value = messageId
        // Automatically expand the thread when replying
        if (!_expandedThreads.value.contains(messageId)) {
            _expandedThreads.value = _expandedThreads.value + messageId
        }
    }

    /**
     * Clear the reply target
     */
    fun clearReplyTo() {
        _replyToMessageId.value = null
    }

    /**
     * Check if a thread is expanded
     */
    fun isThreadExpanded(messageId: String): Boolean {
        return _expandedThreads.value.contains(messageId)
    }

    // MARK: - Image Management Methods

    /**
     * Add images to the current selection
     * Equivalent to iOS image selection handling
     */
    fun addImages(images: List<com.xraiassistant.data.models.AIImageContent>) {
        _selectedImages.value = _selectedImages.value + images
    }

    /**
     * Remove an image at the specified index
     */
    fun removeImageAt(index: Int) {
        val currentImages = _selectedImages.value.toMutableList()
        if (index in currentImages.indices) {
            currentImages.removeAt(index)
            _selectedImages.value = currentImages
        }
    }

    /**
     * Clear all selected images
     */
    fun clearImages() {
        _selectedImages.value = emptyList()
    }

    /**
     * Check if the current AI model supports vision
     */
    fun currentModelSupportsVision(): Boolean {
        // Check if selected model supports vision
        val modelId = _selectedModel.value
        return when {
            modelId.contains("gpt-4", ignoreCase = true) && modelId.contains("vision", ignoreCase = true) -> true
            modelId.contains("claude-3", ignoreCase = true) -> true
            modelId.contains("gemini", ignoreCase = true) && modelId.contains("pro-vision", ignoreCase = true) -> true
            modelId.contains("llama", ignoreCase = true) && modelId.contains("vision", ignoreCase = true) -> true
            else -> false
        }
    }

    // MARK: - RAG Support Methods

    /**
     * Index message for RAG in background (iOS approach: fire-and-forget after message)
     *
     * @param message The message to index
     * @param hadImages Whether this message was sent with images (multimodal messages are skipped)
     */
    private fun indexMessageForRAG(message: ChatMessage, hadImages: Boolean = false) {
        if (!_ragEnabled.value) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                ragRepository.indexMessage(message, hadImages)
                Log.d("ChatViewModel", "✅ Indexed message ${message.id.take(8)} for RAG")
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to index message: ${e.message}", e)
            }
        }
    }

    /**
     * Get RAG-enhanced context for a user query
     * Returns empty string if RAG is disabled or fails
     */
    private suspend fun getRAGContext(userQuery: String): String {
        if (!_ragEnabled.value) return ""

        return try {
            ragRepository.buildContextForQuery(
                userQuery = userQuery,
                libraryId = _currentLibrary.value?.id,
                topK = 10
            )
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Failed to build RAG context: ${e.message}", e)
            ""
        }
    }

    /**
     * Get RAG statistics (for debugging/monitoring)
     */
    suspend fun getRAGStatistics(): Map<String, Int> {
        return ragRepository.getStatistics()
    }

    /**
     * Enable or disable RAG
     */
    fun setRAGEnabled(enabled: Boolean) {
        _ragEnabled.value = enabled
        Log.d("ChatViewModel", if (enabled) "✅ RAG enabled" else "⚠️ RAG disabled")
    }

    // MARK: - Favorites Support

    /**
     * Map of message IDs to their favorited status
     */
    private val _favoritedMessages = MutableStateFlow<Set<String>>(emptySet())
    val favoritedMessages: StateFlow<Set<String>> = _favoritedMessages.asStateFlow()

    /**
     * Load all favorited message IDs
     */
    private fun loadFavoritedMessages() {
        viewModelScope.launch {
            try {
                val favorites = favoriteRepository.getAllFavoritesOnce()
                _favoritedMessages.value = favorites.map { it.messageId }.toSet()
                Log.d("ChatViewModel", "Loaded ${favorites.size} favorited messages")
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to load favorites: ${e.message}", e)
            }
        }
    }

    /**
     * Check if a message is favorited
     */
    fun isMessageFavorited(messageId: String): Boolean {
        return _favoritedMessages.value.contains(messageId)
    }

    /**
     * Toggle favorite status for a message
     *
     * @param messageId The ID of the message
     * @param title Title for the favorite
     * @param code The code content to save
     * @param libraryId Optional library ID
     */
    fun toggleFavorite(
        messageId: String,
        title: String,
        code: String,
        libraryId: String?
    ) {
        viewModelScope.launch {
            try {
                val isFavorited = favoriteRepository.toggleFavorite(
                    messageId = messageId,
                    conversationId = currentConversationId ?: "",
                    title = title,
                    codeContent = code,
                    libraryId = libraryId,
                    modelUsed = selectedModel,
                    screenshotBase64 = null  // Screenshot captured separately
                )

                // Update local state
                _favoritedMessages.value = if (isFavorited) {
                    _favoritedMessages.value + messageId
                } else {
                    _favoritedMessages.value - messageId
                }

                Log.d("ChatViewModel", if (isFavorited) "Added to favorites" else "Removed from favorites")
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to toggle favorite: ${e.message}", e)
            }
        }
    }
}

/** React Three Fiber and Reactylon need an npm build, so they always run on CodeSandbox. */
fun requiresCodeSandbox(libraryId: String): Boolean =
    libraryId == "reactThreeFiber" || libraryId == "reactylon"

fun codeSandboxNoticeFor(libraryId: String): String {
    val name = if (libraryId == "reactylon") "Reactylon" else "React Three Fiber"
    return "$name scenes are built and run on CodeSandbox (codesandbox.io), so they need an internet connection."
}

/** Longest a single reply attempt may run, even while text keeps arriving. */
const val MAX_REPLY_DURATION_MS = 900_000L

fun hasRunTooLong(startedAtMs: Long, nowMs: Long): Boolean = nowMs - startedAtMs > MAX_REPLY_DURATION_MS

const val EMPTY_REPLY_MESSAGE =
    "No answer. The model finished without writing a reply, often because it spent its whole budget thinking. Try again, or pick another model."

/** True when a finished reply has nothing to show once reasoning is removed. */
fun isEmptyReply(raw: String): Boolean =
    com.xraiassistant.domain.text.ReplyText.visible(raw).text.isBlank()

/**
 * Retry an empty reply once at low effort, for models whose thinking shares the
 * answer budget (GLM), unless it already ran at GLM low effort.
 */
fun shouldRetryWithLowEffort(model: String, effort: AIEffort, alreadyRetried: Boolean): Boolean {
    if (alreadyRetried) return false
    val glmEffort = com.xraiassistant.data.models.TogetherReasoning.effort(model, effort) ?: return false
    return glmEffort != "low"
}

/** How long GLM may think without starting its answer before it is asked again at low effort. */
const val GLM_THINKING_LIMIT_MS = 240_000L

const val STILL_THINKING_MESSAGE =
    "Still thinking. This request is large enough that the model was still planning after several minutes, even at low effort. Try GLM-5.3 Flash or Kimi K3, or split the request into smaller steps."

/** Models whose thinking shares the answer budget and has no hard off switch. */
fun thinksBeforeAnswering(model: String): Boolean =
    com.xraiassistant.data.models.TogetherReasoning.effort(model, AIEffort.HIGH) != null
