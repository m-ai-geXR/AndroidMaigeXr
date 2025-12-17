# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

**m{ai}geXR Android** is a revolutionary AI-powered Extended Reality development platform for Android that combines Babylon.js, Together AI, and modern Android development into the ultimate mobile XR development environment with advanced AI assistance capabilities.

> **The Ultimate Mobile XR Development Environment for Android**
> Democratizing 3D and Extended Reality development through conversational AI assistance, professional parameter control, and privacy-first architecture.

---

## 🏗️ Project Architecture

### **MVVM + Clean Architecture**

This project follows modern Android development best practices with a three-layer architecture:

```
┌─────────────────────────────────────┐
│      Presentation Layer              │
│  Compose UI → ViewModels → State    │
│  (Jetpack Compose + Material 3)     │
└──────────────┬──────────────────────┘
               │
┌──────────────┴──────────────────────┐
│         Domain Layer                 │
│  Models → Repositories (Interface)   │
│  (Business logic abstractions)       │
└──────────────┬──────────────────────┘
               │
┌──────────────┴──────────────────────┐
│          Data Layer                  │
│  Repositories (Impl) → Data Sources  │
│  (Room, DataStore, Retrofit)        │
└─────────────────────────────────────┘
```

### **Key Components**

- **`ChatViewModel`** - Main AI integration hub using StateFlow for reactive state
- **`Library3DRepository`** - 3D framework management (Babylon.js, Three.js, A-Frame, R3F, Reactylon)
- **`AIProviderRepository`** - Multi-provider API client (Together.ai, OpenAI, Anthropic)
- **`SettingsRepository`** - Encrypted settings persistence with DataStore
- **`ConversationRepository`** - Chat history management with Room database

---

## 🛠️ Technology Stack

### **Core Android**
- **Kotlin 1.9.20** - Modern language features and coroutines
- **Jetpack Compose 1.5.4** - Declarative UI framework
- **Material Design 3** - Google's latest design system
- **Android Gradle Plugin 8.2.0** - Build tooling
- **Compile SDK 34** (Android 14)
- **Min SDK 26** (Android 8.0)

### **Architecture & DI**
- **Hilt 2.48** - Dependency injection framework
- **ViewModel + StateFlow** - Reactive state management
- **DataStore Preferences 1.0.0** - Type-safe preferences
- **Kotlinx Coroutines** - Asynchronous programming

### **Networking**
- **Retrofit 2.9** - Type-safe HTTP client
- **OkHttp 4.11** - HTTP client with logging interceptor
- **Moshi** - JSON serialization with KSP code generation

### **Local Storage**
- **Room 2.6.0** - SQLite database abstraction
- **Security Crypto** - Encrypted SharedPreferences for API keys

### **UI Components**
- **Compose BOM** - Coordinated Compose library versions
- **Material Icons Extended** - Extended icon set
- **Coil** - Image loading for Compose
- **CommonMark** - Markdown rendering

### **Testing**
- **JUnit 4** - Unit testing framework
- **Mockk** - Mocking library for Kotlin
- **Turbine** - Flow testing utilities
- **Espresso** - UI testing framework
- **Compose UI Test** - Compose-specific testing

---

## 📦 Project Structure

```
app/src/main/java/com/xraiassistant/
├── MainActivity.kt                     # App entry point
├── XRAiAssistantApplication.kt        # Hilt application class
│
├── data/                               # Data Layer
│   ├── local/
│   │   ├── AppDatabase.kt             # Room database
│   │   ├── SettingsDataStore.kt       # DataStore preferences
│   │   ├── dao/
│   │   │   └── ConversationDao.kt     # Room DAO
│   │   └── entities/
│   │       ├── ConversationEntity.kt   # DB entities
│   │       └── MessageEntity.kt
│   ├── models/
│   │   ├── ChatMessage.kt             # Data models
│   │   ├── AIModel.kt
│   │   └── AIRequest.kt               # API request/response models
│   ├── remote/
│   │   ├── AIProviderService.kt       # Service interface
│   │   ├── RealAIProviderService.kt   # Implementation
│   │   ├── TogetherAIService.kt       # Together.ai HTTP client
│   │   ├── OpenAIService.kt           # OpenAI HTTP client
│   │   └── AnthropicService.kt        # Anthropic HTTP client
│   └── repositories/
│       ├── AIProviderRepository.kt    # AI provider management
│       ├── Library3DRepository.kt     # 3D library management
│       ├── SettingsRepository.kt      # Settings persistence
│       └── ConversationRepository.kt  # Chat history
│
├── domain/                             # Domain Layer
│   └── models/
│       ├── Library3D.kt               # 3D library interface
│       ├── AFrameLibrary.kt           # A-Frame implementation
│       ├── ReactylonLibrary.kt        # Reactylon implementation
│       └── CodeExample.kt             # Example models
│
├── presentation/                       # Presentation Layer
│   └── screens/
│       ├── ConversationHistoryScreen.kt
│       └── SettingsScreen.kt
│
├── ui/                                 # UI Components
│   ├── components/
│   │   ├── ChatScreen.kt              # Main chat interface
│   │   ├── SceneScreen.kt             # 3D playground WebView
│   │   └── ChatMessageCard.kt         # Message display
│   ├── screens/
│   │   └── MainScreen.kt              # Bottom navigation container
│   ├── theme/
│   │   ├── Color.kt                   # Material color scheme
│   │   ├── Theme.kt                   # Compose theme
│   │   └── Type.kt                    # Typography definitions
│   └── viewmodels/
│       └── ChatViewModel.kt           # Core business logic
│
└── di/                                 # Dependency Injection
    ├── AppModule.kt                    # App-level dependencies
    └── NetworkModule.kt                # Network dependencies
```

---

## 🎯 Core Features

### **Multi-Provider AI Integration**

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val aiProviderRepository: AIProviderRepository,
    private val library3DRepository: Library3DRepository,
    private val settingsRepository: SettingsRepository,
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    // Reactive state management with StateFlow
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _temperature = MutableStateFlow(0.7)
    val temperature: StateFlow<Double> = _temperature.asStateFlow()

    private val _topP = MutableStateFlow(0.9)
    val topP: StateFlow<Double> = _topP.asStateFlow()

    // Multi-provider AI generation with streaming
    suspend fun sendMessage(content: String) {
        val library = library3DRepository.getCurrentLibrary()
        val systemPrompt = library.systemPrompt

        val response = aiProviderRepository.generateResponse(
            prompt = content,
            model = selectedModel.value,
            temperature = temperature.value,
            topP = topP.value,
            systemPrompt = systemPrompt
        )

        // Process streaming response
        response.collect { chunk ->
            updateMessageContent(chunk)
        }
    }
}
```

### **Supported 3D Libraries**

1. **Babylon.js v8.22.3** - Full-featured WebGL engine
2. **Three.js r171** - Popular lightweight 3D library
3. **A-Frame v1.7.0** - WebXR VR/AR framework
4. **React Three Fiber 8.17.10** - Declarative React + Three.js
5. **Reactylon 3.2.1** - Declarative React + Babylon.js

Each library provides:
- Custom system prompts optimized for that framework
- Code examples and templates
- Automatic API error correction
- Framework-specific build pipelines

### **Secure Settings Management**

```kotlin
class SettingsRepository @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val encryptedPrefs: EncryptedSharedPreferences
) {
    // Encrypted API key storage
    suspend fun saveApiKey(provider: AIProvider, key: String) {
        encryptedPrefs.edit()
            .putString("${provider.name}_api_key", key)
            .apply()
    }

    // Type-safe DataStore preferences
    suspend fun saveTemperature(value: Double) {
        settingsDataStore.updateTemperature(value)
    }

    // Reactive settings observation
    val settingsFlow: Flow<AppSettings> = settingsDataStore.settingsFlow
}
```

---

## 🚀 Development Guidelines

### **Code Style**

1. **Follow Kotlin Coding Conventions**
   - Use camelCase for function and variable names
   - Use PascalCase for class names
   - Use UPPER_SNAKE_CASE for constants
   - Prefer expression bodies for single-expression functions

2. **Compose Best Practices**
   - Use `remember` and `rememberSaveable` appropriately
   - Hoist state to the lowest common ancestor
   - Use `derivedStateOf` for computed state
   - Prefer stateless composables when possible
   - Use `LaunchedEffect` for side effects

3. **Dependency Injection**
   - Use constructor injection with Hilt
   - Annotate ViewModels with `@HiltViewModel`
   - Provide dependencies in appropriate Hilt modules
   - Use `@Singleton` for app-wide single instances

4. **Coroutines & Flow**
   - Use `viewModelScope` for ViewModel coroutines
   - Prefer `StateFlow` over `LiveData` for Compose
   - Use `flow { }` builder for cold flows
   - Handle errors with `catch` operator

### **Architecture Patterns**

```kotlin
// ✅ CORRECT: Clean separation of concerns
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository  // Interface, not implementation
) : ViewModel() {

    fun sendMessage(content: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.sendMessage(content)  // Repository handles details
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
}

// ❌ WRONG: ViewModel directly accessing network
class ChatViewModel {
    fun sendMessage(content: String) {
        // Don't do HTTP calls directly in ViewModel!
        retrofit.create(ApiService::class.java).sendMessage(content)
    }
}
```

### **State Management**

```kotlin
// ✅ CORRECT: StateFlow for reactive state
@HiltViewModel
class ChatViewModel @Inject constructor() : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    // Update state immutably
    fun addMessage(message: ChatMessage) {
        _messages.update { currentMessages ->
            currentMessages + message
        }
    }
}

// In Composable:
@Composable
fun ChatScreen(viewModel: ChatViewModel = hiltViewModel()) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()

    LazyColumn {
        items(messages) { message ->
            MessageCard(message)
        }
    }
}
```

### **Error Handling**

```kotlin
// ✅ CORRECT: Comprehensive error handling
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

class ChatRepository {
    suspend fun sendMessage(content: String): Result<ChatMessage> {
        return try {
            val response = apiService.sendMessage(content)
            Result.Success(response.toDomain())
        } catch (e: IOException) {
            Result.Error(NetworkException("Network error", e))
        } catch (e: HttpException) {
            Result.Error(ApiException("API error: ${e.code()}", e))
        } catch (e: Exception) {
            Result.Error(UnknownException("Unknown error", e))
        }
    }
}
```

---

## ✅ Implementation Checklist

### **Phase 1: Foundation** (COMPLETE ✅)
- [x] Project structure and package organization
- [x] Gradle configuration with version catalog
- [x] Hilt dependency injection setup
- [x] Domain models (ChatMessage, AIModel, Library3D)
- [x] Navigation with bottom tabs
- [x] Material 3 theming (light/dark mode)
- [x] Application class with Hilt

### **Phase 2: AI Integration** (IN PROGRESS 🚧)
- [x] Together.ai HTTP service with Retrofit
- [x] OpenAI HTTP service
- [x] Anthropic HTTP service
- [x] AIProviderRepository with multi-provider support
- [x] ChatViewModel with streaming responses
- [x] Settings persistence with DataStore
- [x] Chat UI with message list
- [ ] Markdown rendering in messages
- [ ] Code syntax highlighting

### **Phase 3: 3D Library System** (IN PROGRESS 🚧)
- [x] Library3D interface and implementations
- [x] BabylonJS, ThreeJS, AFrame libraries
- [x] React Three Fiber and Reactylon libraries
- [x] Code examples for each library
- [ ] WebView integration for 3D rendering
- [ ] JavaScript bridge for code injection
- [ ] Build system for React frameworks

### **Phase 4: Data Persistence** (IN PROGRESS 🚧)
- [x] Room database setup
- [x] ConversationEntity and MessageEntity
- [x] ConversationDao with queries
- [x] ConversationRepository implementation
- [ ] Conversation history UI
- [ ] Search and filter conversations
- [ ] Export/import conversations

### **Phase 5-10: Advanced Features** (PLANNED 📋)
- [ ] CodeSandbox API integration
- [ ] WebView with Monaco editor
- [ ] Local RAG system with SQLite
- [ ] Performance analytics
- [ ] Offline mode capabilities
- [ ] Advanced WebXR features

---

## 🔧 Build & Testing

### **Build Commands**

```bash
# Debug build
./gradlew assembleDebug

# Release build (with ProGuard)
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug

# Clean build
./gradlew clean assembleDebug

# Build and run tests
./gradlew build
```

### **Testing Commands**

```bash
# Run unit tests
./gradlew test

# Run specific test class
./gradlew test --tests ChatViewModelTest

# Run instrumentation tests (requires device/emulator)
./gradlew connectedAndroidTest

# Generate test coverage report
./gradlew jacocoTestReport
```

### **Code Quality**

```bash
# Run lint checks
./gradlew lint

# Check for code style issues
./gradlew ktlintCheck

# Auto-format code
./gradlew ktlintFormat
```

---

## ⚠️ Important Implementation Notes

### **API Key Security**

**CRITICAL**: Always store API keys in encrypted storage, never in plain text or version control.

```kotlin
// ✅ CORRECT: Use EncryptedSharedPreferences
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveApiKey(key: String) {
        encryptedPrefs.edit().putString("api_key", key).apply()
    }
}

// ❌ WRONG: Plain SharedPreferences or hardcoded keys
val apiKey = "sk-1234567890"  // NEVER DO THIS!
```

### **Streaming AI Responses**

When implementing streaming responses, use Flow for reactive updates:

```kotlin
suspend fun streamResponse(prompt: String): Flow<String> = flow {
    val request = buildRequest(prompt)

    apiService.streamCompletion(request).collect { chunk ->
        val text = chunk.choices.firstOrNull()?.delta?.content
        if (text != null) {
            emit(text)  // Emit each chunk as it arrives
        }
    }
}.flowOn(Dispatchers.IO)  // Run on IO dispatcher
```

### **WebView Integration**

For 3D scene rendering, configure WebView properly:

```kotlin
@Composable
fun SceneWebView(
    code: String,
    library: Library3D,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true  // Required for 3D libraries
                    domStorageEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                }

                // Add JavaScript bridge
                addJavascriptInterface(
                    WebViewBridge(onCodeUpdate = { /* handle */ }),
                    "AndroidBridge"
                )
            }
        },
        update = { webView ->
            val html = library.generateHTML(code)
            webView.loadDataWithBaseURL(
                "file:///android_asset/",
                html,
                "text/html",
                "UTF-8",
                null
            )
        },
        modifier = modifier
    )
}
```

### **Dependency Injection Best Practices**

```kotlin
// ✅ CORRECT: Provide interfaces, not implementations
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindChatRepository(
        impl: ChatRepositoryImpl
    ): ChatRepository
}

// ✅ CORRECT: Provide network dependencies
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.together.xyz/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }
}
```

---

## 🐛 Known Issues & Fixes

### **Issue: Together.ai Non-Serverless Model Error (400)**

**Status**: FIXED ✅ (December 16, 2025)

**Problem**: Getting 400 errors when using certain Together.ai models:
```
"message": "Unable to access non-serverless model deepseek-ai/DeepSeek-R1-Distill-Llama-70B-free.
Please visit https://api.together.ai/models/... to create and start a new dedicated endpoint"
"code": "model_not_available"
```

**Root Cause**: Together.ai has changed their API structure. Some models are "serverless" (instant access, pay-per-use) while others require dedicated endpoints (which need to be manually started). The app was using non-serverless models with incorrect model IDs.

**Models Requiring Dedicated Endpoints (❌ NOT Serverless)**:
- `deepseek-ai/DeepSeek-R1-Distill-Llama-70B-free` (old default - FIXED)
- `meta-llama/Llama-3.3-70B-Instruct-Turbo-Free` (incorrect suffix - FIXED)
- Any model not explicitly marked as serverless in Together.ai docs

**Solution**: Replaced non-serverless models with correct serverless model IDs:

```kotlin
// BEFORE (BROKEN):
val DEEPSEEK_R1_70B = AIModel(
    id = "deepseek-ai/DeepSeek-R1-Distill-Llama-70B-free",  // ❌ Requires dedicated endpoint
    displayName = "DeepSeek R1 70B"
)

val LLAMA_3_3_70B = AIModel(
    id = "meta-llama/Llama-3.3-70B-Instruct-Turbo-Free",  // ❌ Incorrect suffix
    displayName = "Llama 3.3 70B"
)

// AFTER (FIXED):
val DEEPSEEK_R1_70B = AIModel(
    id = "deepseek-ai/DeepSeek-R1",  // ✅ Serverless
    displayName = "DeepSeek R1"
)

val LLAMA_3_3_70B = AIModel(
    id = "meta-llama/Llama-3.3-70B-Instruct-Turbo",  // ✅ Serverless
    displayName = "Llama 3.3 70B Turbo"
)
```

**Files Modified**:
- `app/src/main/java/com/xraiassistant/data/models/AIModel.kt` - Updated model definitions (lines 29-53)

**Current Serverless Models (✅ Work Without Dedicated Endpoints)**:
- **DeepSeek R1** (`deepseek-ai/DeepSeek-R1`) - Advanced reasoning & coding
- **Llama 3.3 70B Turbo** (`meta-llama/Llama-3.3-70B-Instruct-Turbo`) - Recommended for chat
- **Llama 3 8B Lite** (`meta-llama/Meta-Llama-3-8B-Instruct-Lite`) - Cost-effective
- **Llama 3.1 8B Turbo** (`meta-llama/Meta-Llama-3.1-8B-Instruct-Turbo`) - Good balance
- **Qwen 2.5 7B Turbo** (`Qwen/Qwen2.5-7B-Instruct-Turbo`) - Fast lightweight model

**Impact**:
- ✅ All Together.ai models now work instantly without configuration
- ✅ No more 400 "model_not_available" errors
- ✅ Users don't need to create dedicated endpoints
- ✅ Default model (DeepSeek R1) now uses correct serverless endpoint
- ✅ Llama 3.3 70B Turbo uses correct model ID

**Reference**: [Together.ai Serverless Models Documentation](https://docs.together.ai/docs/serverless-models)

---

### **Issue: Anthropic API Streaming Response Parsing**

**Status**: FIXED ✅ (November 14, 2025)

**Problem**: Streaming responses from Anthropic Claude API were not appearing in UI despite successful HTTP 200 responses.

**Root Cause**: The `AnthropicResponse` data model required the `id` field to be non-null, but Anthropic's streaming events (`content_block_delta`, `content_block_start`) do not include an `id` field, causing Moshi parsing to fail silently.

**Solution**: Made the `id` field nullable in the data model:

```kotlin
// FIXED: data/models/AIRequest.kt
@JsonClass(generateAdapter = true)
data class AnthropicResponse(
    @Json(name = "id") val id: String? = null,  // ✅ Now nullable
    @Json(name = "type") val type: String,
    @Json(name = "delta") val delta: Delta? = null,
    // ... other fields
)
```

**Files Modified**:
- `app/src/main/java/com/xraiassistant/data/models/AIRequest.kt` (Line 127)

**Impact**:
- ✅ Anthropic streaming now works correctly
- ✅ All event types can be parsed
- ✅ UI receives real-time text chunks
- ✅ No breaking changes to other providers

---

## 📚 Resources & Documentation

### **Official Documentation**
- [Android Developers](https://developer.android.com/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Hilt Dependency Injection](https://developer.android.com/training/dependency-injection/hilt-android)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [DataStore](https://developer.android.com/topic/libraries/architecture/datastore)

### **API Documentation**
- [Together.ai API](https://docs.together.ai/)
- [OpenAI API](https://platform.openai.com/docs/)
- [Anthropic API](https://docs.anthropic.com/)

### **3D Libraries**
- [Babylon.js Documentation](https://doc.babylonjs.com/)
- [Three.js Documentation](https://threejs.org/docs/)
- [A-Frame Documentation](https://aframe.io/docs/)
- [React Three Fiber](https://docs.pmnd.rs/react-three-fiber/)
- [Reactylon](https://www.reactylon.com/docs/)

---

## 🎯 Development Priorities

### **Immediate Priorities**
1. Complete WebView integration for 3D rendering
2. Implement JavaScript bridge for code injection
3. Add CodeSandbox API integration for R3F/Reactylon
4. Implement conversation history UI
5. Add markdown rendering in chat messages

### **Medium-Term Goals**
- Local RAG system with SQLite vector search
- Advanced WebXR features
- Performance analytics and monitoring
- Comprehensive testing suite
- CI/CD pipeline setup

### **Long-Term Vision**
- Offline mode with local AI models
- Multi-user collaboration features
- Cloud sync for conversations
- Plugin system for custom 3D libraries
- AR/VR preview capabilities

---

## 🤝 Contributing Guidelines

### **Before Making Changes**
1. Review this CLAUDE.md file for architectural patterns
2. Check existing implementations for consistency
3. Follow Kotlin coding conventions
4. Write tests for new features
5. Update documentation as needed

### **Code Review Checklist**
- [ ] Follows MVVM + Clean Architecture
- [ ] Uses Hilt for dependency injection
- [ ] Implements proper error handling
- [ ] Includes unit tests (80%+ coverage goal)
- [ ] Updates documentation if needed
- [ ] Passes lint and formatting checks
- [ ] No hardcoded API keys or secrets
- [ ] Follows Material Design 3 guidelines

### **Git Commit Messages**
Use conventional commit format:
- `feat: Add conversation history screen`
- `fix: Fix Anthropic streaming response parsing`
- `refactor: Extract code cleaning logic to utility`
- `docs: Update CLAUDE.md with build instructions`
- `test: Add ChatViewModel unit tests`

---

## 🔐 Security Best Practices

1. **API Keys**: Always use EncryptedSharedPreferences
2. **Network Security**: Configure `network_security_config.xml`
3. **ProGuard**: Enable code obfuscation in release builds
4. **Permissions**: Request only necessary permissions
5. **Input Validation**: Sanitize user input before API calls
6. **HTTPS Only**: Never use cleartext traffic
7. **Certificate Pinning**: Consider for production

---

## 🏁 Quick Start for New Developers

1. **Clone and Build**
   ```bash
   git clone <repository-url>
   cd AndroidMaigeXr
   ./gradlew build
   ```

2. **Open in Android Studio**
   - File → Open → Select project folder
   - Wait for Gradle sync
   - Run on device/emulator

3. **Configure API Keys**
   - Launch app
   - Go to Settings tab
   - Add your Together.ai API key (get free at together.ai)
   - Save settings

4. **Test AI Integration**
   - Go to Code tab
   - Ask: "Create a spinning cube"
   - Tap Run Scene when code is generated

5. **Start Contributing**
   - Check GitHub issues for tasks
   - Review CLAUDE.md for patterns
   - Write tests for new features
   - Submit pull request

---

**Built with ❤️ using Kotlin, Jetpack Compose, and AI**
