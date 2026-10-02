# m{ai}geXR Android

**AI-powered 3D and Extended Reality development, on Android.**

[![Sponsor seacloud9](https://img.shields.io/badge/Sponsor-seacloud9-ea4aaa?logo=githubsponsors&logoColor=white)](https://github.com/sponsors/seacloud9)

Describe a scene in plain English; m{ai}geXR writes the code for your chosen 3D
framework, runs it in an embedded playground, and keeps editing it as you keep
talking.

Kotlin / Jetpack Compose client, sharing its model catalog, 3D library set and
system prompts with the iOS (`iOSMaigeXr/`) and desktop (`WebMaigeXr/`) clients.

**Android 8.0+ (min SDK 26)** · target and compile SDK 34 · version 1.0.0

---

## Features

### Multi-provider AI

Five providers, defined in
[data/models/AIModel.kt](app/src/main/java/com/xraiassistant/data/models/AIModel.kt):

| Provider | Models |
|---|---|
| **Together.ai** | DeepSeek R1, Llama 3.3 70B, Llama 3 8B Lite, Llama 3.1 8B Turbo, Qwen 2.5 7B Turbo |
| **OpenAI** | GPT-6 Astra, GPT-5.6 Sol / Terra / Luna, GPT-5.2 |
| **Anthropic** | Claude Fable 5.1, Opus 5, Sonnet 5, Haiku 4.5, Opus 4.6, Sonnet 4.6 |
| **Google AI** | Gemini 3.1 Pro, Gemini 2.5 Pro / Flash / Flash Lite |
| **xAI** | Grok 4, Grok 4 Fast Reasoning, Grok 3, Grok 3 Mini, Grok Code Fast |

**Two control modes.** The frontier models (Claude 5 series, GPT-5.6 / GPT-6)
removed `temperature` and `top_p` and reject requests carrying them, so they take
a discrete **Reasoning Effort** level instead — `low`, `medium`, `high`, `xhigh`
or `max`. Every other model keeps **Temperature** and **Top-p**. The model
selector shows whichever applies.

Responses stream, and the OkHttp read timeout is raised for reasoning models,
which can think for well over a minute before emitting a first token.

### 3D libraries

Five libraries are active, each with its own system prompt, starter template and
playground page in [app/src/main/assets/](app/src/main/assets/):

| Library | How it runs |
|---|---|
| **Babylon.js** | CDN injection into `playground-babylonjs.html` — the default |
| **Three.js** | Direct injection into `playground-threejs.html` |
| **React Three Fiber** | CodeSandbox build step |
| **A-Frame** | CDN injection, WebXR VR/AR |
| **Nova64** | Embedded studio runner — see below |

**Reactylon is implemented but disabled** in
[Library3DRepository.kt](app/src/main/java/com/xraiassistant/data/repositories/Library3DRepository.kt):
its CodeSandbox TypeScript worker proved unstable on Android WebView. The code
and the re-enablement criteria are kept — see
[docs/REACTYLON_CODESANDBOX_ISSUES.md](docs/REACTYLON_CODESANDBOX_ISSUES.md) and
[docs/REACTYLON_FINAL_STATUS_COMMIT.md](docs/REACTYLON_FINAL_STATUS_COMMIT.md).

**Nova64** is a retro 3D fantasy console (N64/PS1-era low-poly rendering on top
of Three.js), not a library you call. It boots its own runtime and accepts
*carts*, so the playground embeds Nova64's hosted `hero-embed` runner and pushes
the editor buffer into it over `postMessage`. Two constraints shape it:

- **Carts must not use `export`.** The runner evaluates source with
  `new Function()` and then looks up `init` / `update` / `draw` by name, so a
  top-level `export` is a syntax error. The system prompt says so emphatically.
- **The page must run the cart itself.** The native injector in `SceneScreen`
  returns early when `setFullEditorContent` succeeds and never reaches the
  fallback that calls `runCode()`, so `playground-nova64.html` calls it at the
  end of a successful injection.

The playground also loads from an `https` base URL, because an opaque origin
makes the runner throw before the cart executes. Cross-platform design notes
live in the desktop repository at `WebMaigeXr/docs/NOVA64_INTEGRATION.md`.

### Development environment

- **Monaco editor** in a hardened WebView, with live scene rendering
- **Code → Run Scene → Settings** bottom navigation
- **Conversation history** with threaded message views and reply indicators
- **Favorites** for scenes worth keeping
- **Image input** for models that accept it (`ImagePicker`)
- **Markdown rendering** of AI responses (CommonMark + Compose RichText)
- **Library and model selector** modals
- **Vaporwave splash screen** (`assets/splash.html`), visually matched to the
  iOS and desktop clients

### Local storage and RAG

Room database with DAOs for conversations, favorites and RAG, plus an embedding
repository for on-device retrieval over your own scene history — nothing leaves
the device except the request you send to your chosen AI provider. API keys are
held in encrypted SharedPreferences via AndroidX Security Crypto; other settings
go to DataStore.

### WebView hardening

The 3D playground runs inside a WebView that used to take SIGTRAP crashes under
load. It is now hardened in layers — multiprocess mode, raised memory limits,
crash detection and recovery, async loading to avoid ANRs, JavaScript error
isolation, memory monitoring, and a fallback loading strategy. See
[docs/WEBVIEW_CRASH_FIX_SUMMARY.md](docs/WEBVIEW_CRASH_FIX_SUMMARY.md).

---

## Getting started

### Prerequisites

- **Android Studio** (Flamingo or later)
- **Android SDK 34**, min device/emulator API 26
- **JDK 17** (bundled with recent Android Studio as the JBR)

### Build and run

1. Open the `AndroidMaigeXr` folder in Android Studio and let Gradle sync
2. Connect a device or start an emulator
3. Press **▶ Run**

Or from the command line:

```bash
./gradlew assembleDebug
./gradlew installDebug
./gradlew test            # unit tests
```

> `gradle.properties` intentionally does not pin `org.gradle.java.home` — set it
> locally if your JDK is not the one Android Studio provides.

### Configure a provider

1. Tap **Settings** (⚙️) in the bottom navigation
2. Paste a key for the provider you want:
   - **Google AI** — [aistudio.google.com/apikey](https://aistudio.google.com/apikey) (free tier, no card)
   - **Together.ai** — [api.together.ai](https://api.together.ai/settings/api-keys)
   - **OpenAI** — [platform.openai.com](https://platform.openai.com/api-keys)
   - **Anthropic** — [console.anthropic.com](https://console.anthropic.com)
   - **xAI** — [console.x.ai](https://console.x.ai)
3. Pick a model and a 3D library, then save

### First scene

From the **Code** tab, ask for something:

> Create a glowing green planet with rings and three orbiting moons

Tap **Run Scene** when the code appears. Then keep going — *"make the planet
blue"*, *"add stars"*, *"speed up the moons"* — each message edits the scene you
already have.

---

## Architecture

MVVM over a three-layer clean architecture:

```
Presentation   Compose UI → ViewModels → StateFlow
Domain         Models + repository interfaces
Data           Repository impls → Room, DataStore, Retrofit
```

**Key components**

- **`ChatViewModel`** — AI integration hub, reactive state via StateFlow
- **`AIProviderRepository`** / **`AIProviderService`** — multi-provider client
  with retry logic and per-provider request shaping
- **`Library3DRepository`** — 3D framework management and build routing
- **`ConversationRepository`**, **`FavoriteRepository`**, **`RAGRepository`**,
  **`EmbeddingRepository`** — Room-backed persistence
- **`SettingsRepository`** / **`SettingsDataStore`** — encrypted keys plus
  DataStore preferences

---

## Technology stack

**Core** — Kotlin 1.9.20, Jetpack Compose 1.5.4, Material Design 3,
Android Gradle Plugin 8.2.0, compile/target SDK 34, min SDK 26

**Architecture & DI** — Hilt 2.48 (KSP), ViewModel + StateFlow,
DataStore Preferences

**Networking** — Retrofit 2.9, OkHttp 4.11, Moshi (KSP codegen)

**Storage** — Room 2.6 (KSP), AndroidX Security Crypto

**UI** — Compose BOM, Material Icons Extended, Coil, CommonMark +
Compose RichText, AndroidX WebKit

**Monetization** — Play Services Ads (banner + manager)

**Testing** — JUnit 4

---

## Project structure

```
app/src/main/
├── java/com/xraiassistant/
│   ├── MainActivity.kt
│   ├── XRAiAssistantApplication.kt     # Hilt application
│   ├── config/                         # app configuration
│   ├── data/
│   │   ├── local/
│   │   │   ├── SettingsDataStore.kt
│   │   │   ├── dao/                    # Conversation, Favorite, RAG
│   │   │   ├── entities/               # Room entities
│   │   │   └── migrations/
│   │   ├── models/
│   │   │   ├── AIModel.kt              # provider + model catalog
│   │   │   └── ChatMessage.kt
│   │   ├── remote/AIProviderService.kt # HTTP client per provider
│   │   └── repositories/               # AI, Library3D, Settings,
│   │                                   # Conversation, Favorite,
│   │                                   # RAG, Embedding
│   ├── domain/models/                  # Library3D + per-library impls
│   │                                   # (AFrame, Reactylon, Nova64, …)
│   ├── monetization/                   # AdManager, AdBannerView
│   ├── ui/
│   │   ├── components/                 # Chat, Scene, Splash, selectors,
│   │   │                               # threaded messages, image picker
│   │   ├── screens/MainScreen.kt
│   │   ├── theme/                      # neon cyberpunk theme
│   │   └── viewmodels/ChatViewModel.kt
│   └── di/AppModule.kt
└── assets/
    ├── playground-babylonjs.html
    ├── playground-threejs.html
    ├── playground-react-three-fiber.html
    ├── playground-reactylon.html        # present but library disabled
    ├── playground-aframe.html
    ├── playground-nova64.html
    ├── playground-utils.js
    ├── jszip.min.js
    └── splash.html
```

---

## Status

**Working**

- Multi-provider AI with streaming, retry and effort-based controls
- Five active 3D libraries with per-library prompts and playgrounds
- WebView playgrounds with Monaco editing and live scene rendering
- Conversation history, threaded replies, favorites
- Room persistence and on-device RAG
- Encrypted API key storage
- Neon cyberpunk theme and splash screen
- Unit tests for the AI provider layer

**Known limitations**

- **Reactylon is disabled** pending stable CodeSandbox TypeScript workers
- **React Three Fiber needs network access** for its CodeSandbox build step; the
  injection-based libraries and Nova64 do not
- **A clean checkout may not compile without a local JDK path** — see the
  `gradle.properties` note above

**In progress**

- Styling parity pass with the iOS client — see
  [docs/STYLING_PROGRESS.md](docs/STYLING_PROGRESS.md)
- Broader test coverage beyond the AI provider layer

---

## Documentation

- [CLAUDE.md](CLAUDE.md) — architecture and development guide
- [docs/README.md](docs/README.md) — index of all project documentation
- [docs/WEBVIEW_CRASH_FIX_SUMMARY.md](docs/WEBVIEW_CRASH_FIX_SUMMARY.md) — WebView hardening
- [docs/REACTYLON_FINAL_STATUS_COMMIT.md](docs/REACTYLON_FINAL_STATUS_COMMIT.md) — why Reactylon is off
- [docs/STYLING_PROGRESS.md](docs/STYLING_PROGRESS.md) — theming progress
- `WebMaigeXr/docs/NOVA64_INTEGRATION.md` — cross-platform Nova64 design notes

---

## License

MIT. Note that no `LICENSE` file is currently committed in this repository —
only `mcp-webgpu/` has one. Worth adding.
