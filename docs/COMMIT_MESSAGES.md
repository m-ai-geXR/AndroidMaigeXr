# Commit Messages

Commit-ready messages for the current session. Copy directly into git commit.

Format note: no quotation marks, backticks or escaped characters, so these can be
pasted without editing. Mirrors the convention used in the iOS repository.

---

## Session: Nova64 3D library integration (2026-10-01)

Added Nova64 as an additional 3D library alongside Babylon.js, Three.js, A-Frame
and React Three Fiber. Nova64 is a retro 3D fantasy console that renders N64 and
PlayStation era low poly graphics on top of Three.js.
See https://nova64.io and https://nova64.io/docs/api-3d

Full cross platform design notes live in the WebMaigeXr repository at
docs/NOVA64_INTEGRATION.md

---

### Commit 1

feat(nova64): add Nova64 retro 3D console as a 3D library

Nova64 is a console rather than a library: it boots its own runtime and then
accepts carts over postMessage, so there is no script to inject. The playground
embeds Nova64's hosted studio runner and pushes the editor buffer into it.

Two constraints shape the implementation.

First, carts must not use the export keyword. The studio runner evaluates cart
source with new Function and then picks up init, update and draw by name, so a
top level export is a syntax error. Nova64's README shows the export form for
file based carts loaded by its CLI, which does not apply here. The system prompt
states this emphatically because it is the easiest way to break every generated
cart at once.

Second, the page must auto run the cart itself. The native injector in
SceneScreen has several strategies and only its fallback calls runCode. When
setFullEditorContent succeeds it returns early and runs nothing, so
playground-nova64.html calls runCode at the end of a successful injection,
deferred with setTimeout so the function still returns a boolean synchronously.
Every existing playground does the same thing, which is what makes them work.

The playground uses the hero-embed runner rather than cart-runner. cart-runner
wraps the screen in a CRT bezel with scanlines, a glare layer and a NOVA-64
hardware badge, and leaves the runtime fullscreen button visible. hero-embed
draws nothing but the canvas, so playing a cart shows only the game. hero-embed
renders at a fixed size, so the page passes its container dimensions and reboots
the frame when the viewport changes shape.

The page reuses the shared playground chrome verbatim: the same style block as
playground-threejs.html, the same header and menu buttons, the same editor and
canvas container structure, the same floating console window, footer and error
display. It uses the same layout model too, where the editor and the scene are a
full screen swap rather than a side by side split. The only difference is the
render surface, which is an iframe rather than a canvas.

The page is fully self contained and does not load playground-utils.js from the
android_asset file scheme. See commit 2 for why.

Files added:
- app/src/main/java/com/xraiassistant/domain/models/Nova64Library.kt
  Library3D implementation with metadata, system prompt, default cart and four
  worked examples: a corridor runner, a low poly solar system, an interactive
  colour grid and a VR tunnel. requiresBuild is false.
- app/src/main/assets/playground-nova64.html
  Monaco editor, floating console and the studio runner iframe. Identical to the
  iOS copy, since the page has no platform specific subresources.

Files modified:
- app/src/main/java/com/xraiassistant/data/repositories/Library3DRepository.kt
  Registers Nova64Library and returns null from getFrameworkKind, since Nova64
  uses direct injection like Three.js and has no build system entry.
- app/src/main/java/com/xraiassistant/ui/components/LibrarySelectorModal.kt
  Adds the Fantasy Console subtitle for the nova64 id.

---

### Commit 2

fix(webview): load the Nova64 playground from an https base URL

The Nova64 studio runner replies to the host with
event.source.postMessage(msg, event.origin). That is correct usage, but it
requires the host page to have a real scheme and host origin.

The playground was loaded with loadDataWithBaseURL and a null base URL, which
gives the document an opaque origin that serialises to the string null. That is
not a parseable URL, so postMessage threw a SyntaxError inside the runner's
EXECUTE_CODE handler, at the first progress log, which runs before the cart
source is evaluated. The cart therefore never ran and the screen stayed blank.
The only clue was an error whose line and column pointed into the minified
runner bundle.

The Nova64 playground now loads with base URL
https://nova64.io/maigexr-playground/ which gives the document a real origin and
also makes it same origin with the runner, so the runner's replies arrive. All
other playgrounds keep the null base URL, so CDN resources and the
android_asset helper scripts continue to load as before.

This is why playground-nova64.html is fully self contained: an https document
cannot load android_asset subresources, so the page inlines its own native
bridge. It also only sends bridge actions the host already handles, since
unknown action names only produce Unknown action noise in the log.

The durable fix belongs upstream in nova64. Wrapping that progress log in a try
and catch, or posting to the wildcard origin, would make every studio host
immune.

Files modified:
- app/src/main/java/com/xraiassistant/ui/components/SceneScreen.kt
  Chooses an https base URL for the Nova64 playground and keeps null for every
  other playground.

---

### Commit 3

docs: document the Nova64 integration

Files modified:
- CLAUDE.md
  Adds Nova64 to the supported 3D libraries list with its cart shape, the reason
  it is rendered through an embedded runner, and a note that it needs no build
  step. Also updates the Library3DRepository description.

Files added:
- docs/COMMIT_MESSAGES.md
  This file.

---

## Testing notes

Pre-existing build failure, unrelated to these commits: compileDebugKotlin fails
in app/src/main/java/com/xraiassistant/data/repositories/AIProviderRepository.kt
at roughly lines 58 and 92, which pass an effort argument to a
generateResponse that does not declare one. Fixing that exposes a second
mismatch in data/remote/AIProviderService.kt around line 68 where positional
arguments are misaligned. The AI provider layer appears to be mid refactor. No
errors are reported in any file touched by these commits.

What was verified mechanically:
- compileDebugKotlin reports no errors in Nova64Library.kt,
  Library3DRepository.kt, LibrarySelectorModal.kt or SceneScreen.kt. The Kotlin
  compiler reports frontend errors for the whole module, so the absence of
  errors in these files is meaningful.
- All cart sources in Nova64Library.kt load through a faithful copy of Nova64's
  own studio executor and expose init, update and draw with no export keyword.
- Every nova64 namespace call in the prompts, templates and examples resolves
  against NAMESPACE_MAP from the nova64 package.
- The playground page structure, native hooks, auto run and studio protocol
  handling were exercised in a stub DOM harness that replays the real injection
  sequence, including the case where the injector never calls runCode.

Suggested manual checks once the provider layer builds:
- Select Nova64 in the library selector and confirm the Fantasy Console subtitle.
- Run the default cart and confirm a spinning cube, an orbiting orb, fog and
  bloom appear rather than a black screen.
- Confirm no CRT bezel, scanlines or hardware badge are visible while a cart runs.
- Rotate the device and confirm the console reboots at the new size and the cart
  resumes.
