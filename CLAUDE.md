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
2. **Three.js (super-three v0.173.0)** - Modern ES6 modules with postprocessing
3. **A-Frame v1.7.0** - WebXR VR/AR framework
4. **React Three Fiber 8.15.0** - Declarative React + Three.js with postprocessing
5. ~~**Reactylon 3.2.1**~~ - DISABLED (CodeSandbox TypeScript worker unstable - see Known Issues)

Each library provides:
- Custom system prompts optimized for that framework
- Code examples and templates
- Automatic API error correction
- Framework-specific build pipelines
- **Scene export as standalone .zip packages** (NEW ✨)
- **Advanced postprocessing effects** (Three.js, R3F) (NEW ✨)

**Note**: Reactylon is disabled due to CodeSandbox infrastructure bugs (TypeScript worker crashes). The template remains in the codebase for future re-enablement when CodeSandbox resolves their issues.

### **Scene Export System** 💾

All 3D playgrounds now support exporting complete, standalone scene packages:

#### **Export Formats by Framework**

**Babylon.js** → `maigeXR_babylonjs_scene.zip`
- `index.html` - Standalone HTML with embedded code
- `scene.js` - User's Babylon.js code
- `README.txt` - Setup and usage instructions
- **Opens directly in any browser** (no build required)

**Three.js** → `maigeXR_threejs_scene.zip`
- `index.html` - ES6 module-based standalone scene
- `scene.js` - User's Three.js code
- `README.txt` - Setup and troubleshooting
- **Uses ES6 import maps** for modern browsers

**A-Frame** → `maigeXR_aframe_scene.zip`
- `index.html` - WebXR-ready standalone scene
- `scene.html` - User's A-Frame markup
- `README.txt` - VR/AR setup instructions
- **WebXR compatible** for VR headsets

**React Three Fiber** → `maigeXR_r3f_project.zip`
- **Complete npm project structure**:
  - `package.json` - All dependencies (React, R3F, Drei, Three.js)
  - `public/index.html` - React app shell
  - `src/App.js` - User's R3F scene component
  - `src/index.js`, `src/index.css` - React entry point
  - `.gitignore` - Standard React gitignore
  - `README.txt` - npm setup instructions
- **Production-ready**: Run `npm install && npm start`

#### **Export Architecture**

```javascript
// JavaScript side (cross-platform)
async function saveScene() {
    const zip = new JSZip();

    // Add files to package
    zip.file('index.html', generateStandaloneHTML(userCode));
    zip.file('scene.js', userCode);
    zip.file('README.txt', generateReadme());

    // Generate compressed zip
    const zipBlob = await zip.generateAsync({
        type: 'blob',
        compression: 'DEFLATE',
        compressionOptions: { level: 9 }
    });

    // Convert to base64 for native transfer
    const base64 = btoa(arrayBufferToString(zipBlob));

    // Send to native platform (iOS or Android)
    notifySwift('sceneSaved', {
        filename: 'maigeXR_scene.zip',
        data: base64,
        format: 'zip',
        size: bytes.length
    });
}
```

```kotlin
// Android side (SceneScreen.kt)
onSceneSaved = { filename, base64Data ->
    coroutineScope.launch(Dispatchers.IO) {
        // Decode base64 to bytes
        val zipBytes = Base64.decode(base64Data, Base64.DEFAULT)

        // Save to Downloads folder
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), filename)
        file.writeBytes(zipBytes)

        // Create FileProvider URI for sharing
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        // Show Android share sheet
        shareFile(context, uri, filename)
    }
}
```

**Features:**
- ✅ **One-click export** from playground toolbar
- ✅ **Android Share Sheet** integration (save, email, upload to Drive, etc.)
- ✅ **Chunked base64 encoding** prevents memory issues
- ✅ **FileProvider security** for Android 7+ compatibility
- ✅ **Framework-specific templates** with best practices
- ✅ **Complete documentation** in README.txt

### **Advanced Postprocessing Effects** 🎨

Three.js and React Three Fiber playgrounds now support professional postprocessing effects via ES6 module imports.

#### **Available Effects**

**Three.js (super-three v0.173.0)**
```javascript
import * as THREE from 'three';
import { EffectComposer } from 'three/addons/postprocessing/EffectComposer.js';
import { RenderPass } from 'three/addons/postprocessing/RenderPass.js';
import { UnrealBloomPass } from 'three/addons/postprocessing/UnrealBloomPass.js';
import { OutputPass } from 'three/addons/postprocessing/OutputPass.js';

// All globally available
window.EffectComposer
window.RenderPass
window.UnrealBloomPass
window.OutputPass
window.createInspector // three-inspect debugging
```

**React Three Fiber (Three.js r160 + addons)**
```javascript
// Postprocessing loaded automatically
window.EffectComposer
window.RenderPass
window.UnrealBloomPass
window.OutputPass
window.OrbitControls
```

#### **Example: Neon Glow with UnrealBloom**

```javascript
// Three.js
const composer = new EffectComposer(renderer);
const renderPass = new RenderPass(scene, camera);
const bloomPass = new UnrealBloomPass(
    new THREE.Vector2(window.innerWidth, window.innerHeight),
    1.5, // strength
    0.4, // radius
    0.85 // threshold
);

composer.addPass(renderPass);
composer.addPass(bloomPass);
composer.addPass(new OutputPass());

// Render loop
function animate() {
    requestAnimationFrame(animate);
    composer.render();
}
```

#### **Technical Implementation**

**Modern ES6 Modules**: Android WebView (API 26+) fully supports ES6 modules and import maps:
```html
<script type="importmap">
{
    "imports": {
        "three": "https://cdn.jsdelivr.net/npm/super-three@0.173.0/build/three.module.js",
        "three/addons/": "https://cdn.jsdelivr.net/npm/super-three@0.173.0/examples/jsm/"
    }
}
</script>

<script type="module">
    import * as THREE from 'three';
    import { UnrealBloomPass } from 'three/addons/postprocessing/UnrealBloomPass.js';

    window.THREE = THREE;
    window.UnrealBloomPass = UnrealBloomPass;
</script>
```

**Features:**
- ✅ **UnrealBloom** - Neon glow effects
- ✅ **EffectComposer** - Multi-pass rendering pipeline
- ✅ **RenderPass** - Base scene render
- ✅ **OutputPass** - Tone mapping and color correction
- ✅ **Automatic loading** - No manual setup required
- ✅ **Polling system** - 5-second timeout for CDN reliability
- ✅ **Fallback handling** - Continues gracefully if addons fail to load

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

### **RAG System (Retrieval-Augmented Generation)**

The app includes a complete RAG system that enhances AI responses with context from previous conversations.

#### **Architecture Overview**

```
User Query
    ↓
ChatViewModel.sendMessage()
    ↓
1. getRAGContext(query) → Semantic search past conversations
    ↓
2. Build enhanced system prompt with RAG context
    ↓
3. Send to AI provider (Together.ai, OpenAI, Anthropic)
    ↓
4. Receive streaming response
    ↓
5. indexMessageForRAG() → Index both user and AI messages (fire-and-forget)
```

#### **Key Components**

**1. RAGRepository** - High-level API coordinating all RAG services
```kotlin
@Singleton
class RAGRepository @Inject constructor(
    private val ragDao: RAGDao,
    private val embeddingRepository: EmbeddingRepository,
    private val vectorSearchService: VectorSearchService,
    private val ragContextBuilder: RAGContextBuilder
) {
    // Index a message for future retrieval
    suspend fun indexMessage(message: ChatMessage)

    // Build context for AI query
    suspend fun buildContextForQuery(
        userQuery: String,
        libraryId: String? = null,
        topK: Int = 10
    ): String

    // Search messages semantically
    suspend fun searchMessages(
        query: String,
        limit: Int = 10
    ): List<RAGDocument>
}
```

**2. EmbeddingRepository** - Together AI embedding generation
```kotlin
@Singleton
class EmbeddingRepository @Inject constructor(
    private val embeddingService: EmbeddingService
) {
    // Generate 768-dimensional vector embedding
    suspend fun generateEmbedding(text: String): FloatArray

    // Batch processing with rate limiting
    suspend fun generateEmbeddings(texts: List<String>): List<FloatArray>
}
```

**3. VectorSearchService** - Semantic search with cosine similarity
```kotlin
@Singleton
class VectorSearchService @Inject constructor(
    private val ragDao: RAGDao,
    private val embeddingRepository: EmbeddingRepository
) {
    // Pure semantic search
    suspend fun semanticSearch(
        query: String,
        topK: Int = 5,
        sourceType: String? = null
    ): List<RAGDocument>

    // Hybrid: 60% semantic + 40% keyword (FTS4)
    suspend fun hybridSearch(
        query: String,
        topK: Int = 10,
        sourceType: String? = null
    ): List<RAGDocument>
}
```

**4. RAGContextBuilder** - Token-aware context assembly
```kotlin
@Singleton
class RAGContextBuilder @Inject constructor(
    private val vectorSearchService: VectorSearchService
) {
    suspend fun buildContext(
        userQuery: String,
        libraryId: String? = null,
        topK: Int = 10
    ): String
}
```

#### **Database Schema**

```sql
-- Documents table
CREATE TABLE rag_documents (
    id TEXT PRIMARY KEY,
    sourceType TEXT NOT NULL,  -- "message" or "conversation"
    sourceId TEXT NOT NULL,
    chunkText TEXT NOT NULL,
    chunkIndex INTEGER NOT NULL,
    metadata TEXT  -- JSON metadata
)

-- Embeddings table (768-dimensional vectors)
CREATE TABLE rag_embeddings (
    id TEXT PRIMARY KEY,
    documentId TEXT NOT NULL,
    embedding BLOB NOT NULL,  -- FloatArray as bytes
    FOREIGN KEY(documentId) REFERENCES rag_documents(id)
)

-- FTS4 full-text search index
CREATE VIRTUAL TABLE rag_documents_fts
USING fts4(content=rag_documents, chunkText)
```

#### **Configuration**

- **Embedding Model**: `togethercomputer/m2-bert-80M-8k-retrieval`
- **Embedding Dimension**: 768
- **Context Token Limit**: 3000 tokens (~12,000 characters)
- **Search Algorithm**: Hybrid (60% semantic + 40% keyword)
- **Similarity Metric**: Cosine similarity
- **Indexing Strategy**: Fire-and-forget background indexing
- **API Key Requirement**: Requires Together.ai API key (RAG automatically disabled if not configured)
- **Multimodal Support**: Automatically skips indexing messages with images to avoid token limits

#### **Usage in ChatViewModel**

```kotlin
// Automatic RAG enhancement in sendMessage()
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

private fun indexMessageForRAG(message: ChatMessage) {
    if (!_ragEnabled.value) return
    viewModelScope.launch(Dispatchers.IO) {
        try {
            ragRepository.indexMessage(message)
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Failed to index message: ${e.message}", e)
        }
    }
}
```

#### **Features**

- ✅ **Automatic Message Indexing**: Every message is indexed after sending
- ✅ **Semantic Search**: Find relevant context even with different wording
- ✅ **Hybrid Search**: Combines keyword (FTS4) + semantic (vector) search
- ✅ **Token-Aware Context**: Limits context to 3000 tokens to fit in prompts
- ✅ **Library Filtering**: Can filter results by 3D library (Babylon.js, Three.js, etc.)
- ✅ **Fire-and-Forget Indexing**: Non-blocking background indexing
- ✅ **Cosine Similarity**: Standard vector similarity metric
- ✅ **Batch Processing**: Efficient batch embedding generation

#### **Performance Characteristics**

- **Embedding Generation**: ~100-200ms per message (via Together AI API)
- **Vector Search**: O(n) in-memory search (<10ms for <10K embeddings)
- **FTS4 Search**: O(log n) SQLite full-text search
- **Context Assembly**: <50ms for typical queries
- **Storage**: ~3KB per message (text + 768-dim vector)

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
- [x] **Neon cyberpunk styling applied to chat interface** ✨
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

### **Phase 5: RAG System** (COMPLETE ✅)
- [x] RAG database entities (RAGDocumentEntity, RAGEmbeddingEntity, FTS4)
- [x] RAG DAO with vector search queries
- [x] Together AI embedding service integration
- [x] EmbeddingRepository with batch processing
- [x] VectorSearchService with cosine similarity
- [x] Hybrid search (60% semantic + 40% keyword)
- [x] RAGContextBuilder for token-aware context assembly
- [x] RAGRepository high-level API
- [x] ChatViewModel RAG integration (context retrieval + indexing)
- [x] Database migration to version 3

### **Phase 5.5: Neon Cyberpunk Styling** (ALL PHASES COMPLETE ✅)
- [x] **Phase 1: Color System & Theme Foundation**
  - [x] Neon color palette (Electric Pink, Aqua Cyan, Deep Purple, Neon Blue, Acid Green)
  - [x] Dark cyberpunk backgrounds (CyberpunkBlack, CyberpunkDarkGray)
  - [x] NeonCyberpunkColorScheme with Material 3
  - [x] Dark-only theme (no light mode)
- [x] **Phase 2: Font Integration**
  - [x] Exo 2 font via Downloadable Fonts API (Google Fonts)
  - [x] Font provider certificates configuration
  - [x] Typography system with Roboto (Exo 2 fallback due to complexity)
- [x] **Phase 3: Enhanced Neon Effects Library**
  - [x] `neonGlow()` - 8dp blur, 0.35 opacity
  - [x] `neonBorder()` - 8dp glow with colored border
  - [x] `neonButtonGlow()` - 12dp blur (strongest)
  - [x] `neonInputGlow()` - 10dp blur (strong)
  - [x] `neonCardGlow()` - 6dp blur (balanced)
  - [x] `neonOutlineGlow()`, `neonTextGlow()`, `neonAccentLine()`
- [x] **Phase 4: ChatScreen Styling**
  - [x] Input TextField: 10dp neon cyan glow + neon borders
  - [x] Send Button: 12dp neon pink glow (when active)
  - [x] Header: Neon cyan accent divider line
  - [x] Chat bubbles: Already styled with neon borders
- [x] **Phase 5: Settings Screen** (Complete ✅)
  - [x] TopAppBar: m{ai}geXR branding with neon cyan icon and glowing Save button
  - [x] API Cards: Provider-specific neon borders (blue, green, purple, red, orange)
  - [x] Input fields: 10dp glows matching provider colors
  - [x] Sliders: Neon blue (temperature) and green (top-p) with 6dp glows
  - [x] Switches: Conditional cyan glow when enabled
  - [x] Section headers: All NeonCyan
  - [x] Danger zone: Red neon card with glowing delete button
- [x] **Phase 6: Navigation Bar** (Complete ✅)
  - [x] Neon cyan divider line above navigation
  - [x] Color-coded tabs: Cyan (Chat), Pink (Scene), Blue (History)
  - [x] Glowing indicator backgrounds
  - [x] Neon pink notification dot on Scene tab
- [x] **Phase 7: Additional Polish** (Complete ✅)
  - [x] Scaffold background: CyberpunkBlack
  - [x] Branding updates: m{ai}geXR throughout
  - [x] Theme consistency across all screens

**Progress:** 100% Complete (7/7 phases) 🎉 | **Reference:** See `STYLING_PROGRESS.md` for detailed documentation

### **Phase 6-10: Advanced Features** (PLANNED 📋)
- [x] CodeSandbox API integration (React Three Fiber)
- [ ] WebView with Monaco editor
- [ ] Performance analytics
- [ ] Offline mode capabilities
- [ ] Advanced WebXR features
- [ ] Conversation history UI with RAG-powered search

---

## 🔧 Build & Testing

### **Development Environment**

**IMPORTANT**: This project is developed in **WSL (Windows Subsystem for Linux)** environment.

- Gradle commands should be run from WSL terminal (NOT Windows cmd.exe or PowerShell)
- File paths use WSL format: `/mnt/c/Users/...`
- Java/Android SDK paths are configured for WSL
- Do NOT use `cmd.exe /c gradlew.bat` commands - use `./gradlew` instead

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

## 📄 Project Documentation Files

### **STYLING_PROGRESS.md** - Neon Cyberpunk Styling Tracker
**Location:** `/mnt/c/Users/brend/exp/maigeXR/AndroidMaigeXr/STYLING_PROGRESS.md`

Comprehensive multi-session tracking document for the neon cyberpunk styling transformation.

**Purpose:** Maintain continuity across development sessions for the styling project

**Key Sections:**
- ✅ **Branding Requirements Summary** - Color palette, typography, visual style from brand guide
- ✅ **Phase-by-Phase Progress** - 7 phases with detailed implementation status
- ✅ **Session Notes** - Code examples, decisions, and implementation details
- ✅ **Build Status** - Compilation errors and resolutions
- ✅ **Technical Notes** - Performance considerations, Material 3 mappings, accessibility
- ✅ **Quick Resume Guide** - Fast context recovery for next session

**Current Status:**
- **Phase 1-4:** ✅ Complete (Color system, fonts, neon effects, ChatScreen styling)
- **Phase 5-7:** 📋 Planned (Settings, Navigation, Additional polish)
- **Overall Progress:** 57% complete

**Usage:**
1. Check `STYLING_PROGRESS.md` at start of each styling session
2. Update phase status as work is completed
3. Document issues and decisions for future reference
4. Review "Session Summary" section for last session's work

---

## 🐛 Known Issues & Fixes

### **Issue: Font Loading Crash - Exo 2 Downloadable Fonts**

**Status**: RESOLVED ✅ (December 19, 2025)

**Problem**: App crashed on launch when using Downloadable Fonts API for Exo 2 font:
```
java.lang.IllegalStateException: Could not load font
Caused by: android.content.res.Resources$NotFoundException: Font resource ID #0x7f090000
```

**Root Cause**: Downloadable Fonts API complexity:
- Requires `<meta-data>` entries in AndroidManifest.xml for font provider
- Requires async font loading (Compose tries to load synchronously causing crash)
- Dependent on Google Play Services availability
- Font certificates need to match Google Fonts provider exactly

**Solution**: Reverted to **system `FontFamily.SansSerif`** (Roboto on Android)
```kotlin
// BEFORE (BROKEN - Downloadable Fonts):
val ExoFontFamily = FontFamily(Font(R.font.exo2))  // ❌ Requires complex setup

// AFTER (FIXED - System Font):
val ExoFontFamily = FontFamily.SansSerif  // ✅ Works immediately
```

**Benefits of System Font:**
- ✅ Roboto (Android's default) is modern, clean, and professional
- ✅ Zero configuration required
- ✅ No network dependency or Google Play Services requirement
- ✅ No APK size increase
- ✅ Works on all Android devices immediately

**Files Modified:**
- `app/src/main/java/com/xraiassistant/ui/theme/Type.kt` - Line 22

**Future Options:**
1. **Bundle Exo 2 TTF files** directly in `/res/font/` (~150KB APK increase)
2. **Implement async Downloadable Fonts** properly (requires manifest changes + async loading logic)
3. **Keep Roboto** - it's a great modern font that matches Material Design

**Recommendation:** Keep Roboto unless custom branding is critical. It's clean, modern, and zero-hassle.

---

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
- `app/src/main/java/com/xraiassistant/data/local/SettingsDataStore.kt` - Added automatic migration logic for saved preferences (lines 81-121)

**Migration Logic**: The app now automatically migrates old model IDs to new ones when loading settings:

```kotlin
// In SettingsDataStore.getSettings()
private fun migrateModelId(oldId: String): String {
    return when (oldId) {
        // DeepSeek R1 migrations
        "deepseek-ai/DeepSeek-R1-Distill-Llama-70B-free" -> "deepseek-ai/DeepSeek-R1"

        // Llama 3.3 70B migrations
        "meta-llama/Llama-3.3-70B-Instruct-Turbo-Free" -> "meta-llama/Llama-3.3-70B-Instruct-Turbo"

        // If no migration needed, return original
        else -> oldId
    }
}
```

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
- ✅ **Existing users with saved old model IDs are automatically migrated**

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

### **Issue: Reactylon CodeSandbox Instability**

**Status**: DISABLED ⚠️ (December 21, 2025)

**Problem**: Reactylon library disabled due to CodeSandbox TypeScript worker instability. While React version compatibility was resolved, CodeSandbox iframe infrastructure has inherent bugs that cause unreliable scene rendering.

**Implementation Journey**:

1. **Phase 1 - React 19 Incompatibility** (Original Issue)
   - CodeSandbox cannot fetch React 19 packages
   - Template had mismatched types: React 18 runtime with React 19 type definitions

2. **Phase 2 - React 18 Migration** (Completed)
   - Updated `@types/react` from `19.0.0` → `18.3.1`
   - Updated `@types/react-dom` from `19.0.0` → `18.3.1`
   - Upgraded `reactylon` from `3.0.0` → `3.2.1` (latest with React 18 fixes)
   - Simplified template structure (removed double-render issue)

3. **Phase 3 - WebView Crash Prevention** (Completed)
   - Implemented `onRenderProcessGone()` crash handler
   - Added JavaScript error detection
   - Optimized WebView memory settings
   - Added async loading to prevent ANR

4. **Phase 4 - CodeSandbox Iframe Failures** (Current Issue)
   - CodeSandbox TypeScript worker crashes: `Cannot read properties of undefined (reading 'readFile')`
   - Monaco editor (VS Code) integration bugs
   - Scenes fail to render (black screen)
   - **App stays alive** (crash prevention working), but scene doesn't load

**Final CodeSandbox Template** (Working, but CodeSandbox unstable):
```kotlin
fun createReactylonSandbox(code: String): Map<String, CodeSandboxFile> {
    // Runtime dependencies:
    "react": "^18.3.1",
    "react-dom": "^18.3.1",
    "react-reconciler": "^0.29.2",  // Required for React 18
    "reactylon": "^3.2.1",           // Latest version with bug fixes
    "@babylonjs/core": "^8.0.0",
    "@babylonjs/gui": "^8.0.0",
    "@babylonjs/havok": "^1.3.10"

    // Dev dependencies (matching runtime):
    "@types/react": "18.3.1",
    "@types/react-dom": "18.3.1",
    "typescript": "5.4.0"
}
```

**Current Status**:
- ❌ Reactylon **disabled** in library dropdown (line 25: `Library3DRepository.kt`)
- ✅ CodeSandbox template **remains in codebase** (can be re-enabled when CodeSandbox fixes bugs)
- ✅ Crash prevention **working** (app survives CodeSandbox failures)
- ✅ 4 stable libraries available: Babylon.js, Three.js, React Three Fiber, A-Frame

**Why Disabled**:
CodeSandbox is a **third-party service** with bugs beyond our control:
- TypeScript worker crashes on complex React builds
- Monaco editor file system API failures
- Unreliable iframe rendering
- Cannot be fixed from app side

**Files Modified**:
- `app/src/main/java/com/xraiassistant/data/models/CodeSandboxModels.kt` - Template updated (kept for future)
- `app/src/main/java/com/xraiassistant/data/repositories/Library3DRepository.kt` (Line 25) - **Commented out ReactylonLibrary**
- `app/src/main/java/com/xraiassistant/ui/components/SceneScreen.kt` - Crash prevention and CodeSandbox error detection

**Workarounds**:
1. **Use Babylon.js** (recommended) - Direct injection, no CodeSandbox, very stable
2. **Use Three.js** - Direct injection, no CodeSandbox, very stable
3. **Use React Three Fiber** - CodeSandbox build, more stable than Reactylon
4. **Use A-Frame** - Direct injection for VR/AR scenes

**Future Re-enablement**:
Reactylon can be re-enabled when:
1. CodeSandbox fixes TypeScript worker bugs, OR
2. Local bundler implemented (webpack/vite on-device), OR
3. Alternative build service found (StackBlitz, etc.)

**Documentation**:
- `REACTYLON_CODESANDBOX_ISSUES.md` - Detailed analysis of CodeSandbox failures
- `REACTYLON_TESTING_CHECKLIST.md` - Comprehensive testing guide (if re-enabled)
- `REACTYLON_IMPLEMENTATION_PLAN.md` - Original implementation plan
- `WEBVIEW_CRASH_FIX_SUMMARY.md` - Crash prevention implementation

**Impact**:
- ✅ App crash prevention working (no more SIGTRAP deaths)
- ✅ 4 stable 3D libraries available
- ❌ Reactylon unavailable until CodeSandbox improves
- ✅ Codebase ready for quick re-enablement

**Reference**:
- [Reactylon Documentation](https://www.reactylon.com/docs/getting-started/reactylon)
- [CodeSandbox Known Limitations](https://codesandbox.io/docs/learn/sandboxes/limitations)
- Issue: CodeSandbox TypeScript worker `readFile` bug (external, unfixable)

---

### **RAG System and Multimodal Support**

**Status**: FIXED ✅ (December 18, 2025)

**Configuration**: The RAG (Retrieval-Augmented Generation) system is designed to work seamlessly alongside multimodal capabilities without interfering with image-based interactions.

**Key Behaviors**:

1. **Automatic Disabling**: RAG automatically disables itself if Together.ai API key is not configured
   - No errors or failures when using other providers (Google AI, OpenAI, Anthropic)
   - Multimodal support works independently of RAG status

2. **Multimodal Message Handling** (UPDATED): RAG context is completely bypassed when images are present
   - **RAG Context Retrieval**: When sending a message with images, RAG context is NOT added to the system prompt
     - Images are the primary context, not past conversations
     - Prevents AI from being confused by mixing image analysis with historical context
   - **RAG Indexing**: Messages with images are automatically skipped for RAG indexing
     - Prevents base64 image data from exceeding token limits
     - Only text-only messages are indexed for semantic search
   - **Image Clearing**: Selected images are automatically cleared after sending to prevent sticking to next message

3. **Independent Operation**: RAG runs in background and never blocks main chat flow
   - Multimodal messages process normally even if RAG fails
   - Embedding generation errors are logged but don't crash the app
   - Each AI provider works independently (Google AI for multimodal, Together.ai for RAG optional)

**Implementation Details**:

```kotlin
// In ChatViewModel.sendMessage()
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

// In RAGRepository.indexMessage()
// Check if RAG is available (Together.ai API key configured)
if (!embeddingRepository.isRAGAvailable()) {
    Log.d(TAG, "⚠️ Skipping message - RAG not available")
    return@withContext
}

// Skip multimodal messages (messages with images)
if (hadImages) {
    Log.d(TAG, "⚠️ Skipping multimodal message - was sent with images")
    return@withContext
}
```

**Files Modified**:
- `app/src/main/java/com/xraiassistant/data/remote/EmbeddingService.kt` - Added Authorization header parameter
- `app/src/main/java/com/xraiassistant/data/repositories/EmbeddingRepository.kt` - Added API key injection and availability check
- `app/src/main/java/com/xraiassistant/data/repositories/RAGRepository.kt` - Added multimodal message skip logic for indexing
- `app/src/main/java/com/xraiassistant/ui/viewmodels/ChatViewModel.kt` - **NEW**: Skip RAG context retrieval when images present, auto-clear images after sending (lines 231-241, 311-315)

**Impact**:
- ✅ Multimodal support (images) works perfectly regardless of RAG configuration
- ✅ **FIXED**: AI now properly analyzes uploaded images instead of focusing on past conversations
- ✅ RAG gracefully handles missing Together.ai API key
- ✅ No token limit issues with image data
- ✅ Users can use Google AI/OpenAI/Anthropic for multimodal without configuring Together.ai
- ✅ RAG enhances text-only conversations when Together.ai API key is configured
- ✅ Images automatically clear after sending (no persistence to next message)

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
1. Test RAG system end-to-end (verify indexing and context retrieval)
2. Implement conversation history UI with RAG-powered search
3. Add markdown rendering in chat messages
4. Complete WebView integration for 3D rendering
5. Implement JavaScript bridge for code injection

### **Medium-Term Goals**
- RAG system optimization and performance tuning
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

**IMPORTANT**: Follow these strict formatting guidelines for all commit messages to ensure consistency and compatibility across tools.

#### **Character Restrictions**
**CRITICAL**: Do NOT use these special characters anywhere in commit messages:
- ❌ Single quotes (`'`)
- ❌ Double quotes (`"`)
- ❌ Backticks (`` ` ``)

These characters can cause parsing issues with Git hooks, CI/CD pipelines, and automation tools.

#### **Commit Message Structure**

```
# First Line: Summary (Title)
[Type]: [Brief description in imperative mood]

## Summary
[1-3 sentence overview of changes]

## Changes Made
### [Category 1]
- [Change description]
- [Change description]

### [Category 2]
- [Change description]

## Files Created
1. [File path] - [Purpose]

## Files Modified
1. [File path]
   - [What changed]
   - [Why it changed]

## Known Issues Resolved
[If applicable - describe issues fixed]

## Testing Status
- Build: [Status]
- Runtime: [Status]

## Visual Impact
[If applicable - describe user-visible changes]

## Overall Progress
[If part of larger initiative]

## Next Steps
[If applicable - what comes next]

## Reference
[Related documentation, branding guides, etc.]

---

Generated: [Date]
Author: [Your name or Claude Sonnet 4.5]
Phase: [If part of phased work]
```

#### **Conventional Commit Types**
Use these prefixes for the first line:
- `feat:` - New feature implementation
- `fix:` - Bug fixes and issue resolutions
- `refactor:` - Code restructuring without behavior change
- `docs:` - Documentation updates only
- `test:` - Adding or updating tests
- `style:` - Code formatting, UI styling changes
- `perf:` - Performance improvements
- `build:` - Build system or dependency changes
- `ci:` - CI/CD pipeline changes
- `chore:` - Maintenance tasks

#### **Examples**

**Good Commit Messages:**
```
feat: Add neon cyberpunk styling to chat interface

## Summary
Implemented neon cyberpunk visual styling for chat screen following branding guide specifications. Added enhanced glow effects for interactive elements.

## Changes Made
### Enhanced Glow Effects
- Increased button glow from 8dp to 12dp blur
- Added new neonInputGlow modifier for text fields (10dp)
- Updated card glow from 4dp to 6dp blur

### ChatScreen Styling
- Applied 10dp neon cyan glow to input TextField
- Applied 12dp neon pink glow to send button when active
- Added neon cyan accent divider line below header

## Files Modified
1. app/src/main/java/com/xraiassistant/ui/theme/NeonEffects.kt
   - Updated glow intensities for all modifiers
2. app/src/main/java/com/xraiassistant/ui/components/ChatScreen.kt
   - Applied neon effects to input field and send button

## Testing Status
- Build: Compiles successfully
- Runtime: App launches without crashes
- Styling: All neon effects visible and functional

---

Generated: 2025-12-19
Author: Claude Sonnet 4.5
Phase: Neon Cyberpunk Styling - Phase 4 Complete
```

**Bad Commit Messages (DO NOT USE):**
```
# ❌ Using special characters
fix: Fixed "bug" in 'settings' screen

# ❌ Too vague
fix: bug fixes

# ❌ Wrong tense
feat: Added new feature

# ✅ CORRECT versions:
fix: Resolve crash in settings screen when clearing API key
feat: Add conversation history search with semantic filtering
```

#### **Multi-Phase Projects**

For large features spanning multiple sessions (like the neon cyberpunk styling project):

1. **Create a COMMIT_MESSAGE.md** file first with full details
2. **Keep commit titles short** (under 72 characters)
3. **Reference tracking documents** like STYLING_PROGRESS.md
4. **Document phase completion** clearly
5. **Update progress metrics** (e.g., 57% complete, 4/7 phases)
6. **Include file counts** (created vs modified)

#### **Special Characters Workaround**

Instead of using restricted characters, use these alternatives:

| ❌ Avoid | ✅ Use Instead |
|---------|---------------|
| `"quoted text"` | quoted text or [text] |
| `'string'` | string or (text) |
| `` `code` `` | code or [code] or ALL_CAPS |
| `it's` | it is |
| `don't` | do not |

#### **Commit Message Template**

For consistency, create a `.gitmessage` template file in your project root:

```bash
# Save this as .gitmessage
# Configure with: git config commit.template .gitmessage

[type]: [summary in imperative mood - no special characters]

## Summary


## Changes Made


## Files Modified


## Testing Status
- Build:
- Runtime:

---

Generated: YYYY-MM-DD
Author:
```

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
