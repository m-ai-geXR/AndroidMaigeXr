# Reactylon Testing Checklist

Use this checklist when testing Reactylon integration after updates or for regression testing.

---

## Pre-Test Setup

- [ ] Build latest APK: `./gradlew clean assembleDebug`
- [ ] Install on Android device/emulator: `./gradlew installDebug`
- [ ] Clear app data to ensure fresh state (optional but recommended)
- [ ] Configure Together.ai API key in Settings (required for AI code generation)
- [ ] Enable logcat filtering: `adb logcat | grep -E "ChatViewModel|CodeSandbox|SceneScreen|Reactylon"`

---

## 1. Library Selection

- [ ] Launch app and navigate to Settings
- [ ] Scroll to "3D Library" section
- [ ] Verify "Reactylon" appears in library dropdown
- [ ] Select Reactylon from dropdown
- [ ] Verify library selection persists after app restart
- [ ] Check logcat for system prompt update: `System prompt set from Reactylon`

---

## 2. Code Generation

### Basic Test
- [ ] Navigate to Chat tab
- [ ] Send message: `Create a rotating cube in Reactylon`
- [ ] Verify AI response includes:
  - [x] Brief explanation of what code does
  - [x] `[INSERT_CODE]```typescript ... ```[/INSERT_CODE]` block
  - [x] Valid React 18 + Reactylon syntax
  - [x] `[RUN_SCENE]` trigger at the end

### Code Structure Validation
Check that generated code includes all required imports:
- [ ] `import React from 'react'`
- [ ] `import { createRoot } from 'react-dom/client'`
- [ ] `import { Engine } from 'reactylon/web'`
- [ ] `import { Scene, box, sphere, ... } from 'reactylon'`
- [ ] `import { Color3, Vector3, createDefaultCameraOrLight } from '@babylonjs/core'`

### Code Pattern Validation
- [ ] Functional component structure (not class components)
- [ ] Proper React hooks usage (useState, useEffect if needed)
- [ ] Engine component with `antialias`, `adaptToDeviceRatio`, `canvasId="canvas"`
- [ ] Scene component with `onSceneReady` callback
- [ ] `createDefaultCameraOrLight(scene, true, true, true)` for camera setup
- [ ] Meshes use `Vector3` for positions (not arrays)
- [ ] Materials nested inside mesh components as children
- [ ] Colors use `Color3` objects (not hex strings or arrays)
- [ ] `createRoot` and `render` call at the end

---

## 3. CodeSandbox Build

- [ ] Tap "Run Scene" button (or wait for auto-trigger if `[RUN_SCENE]` present)
- [ ] Verify loading overlay appears: "Building with CodeSandbox..."
- [ ] Monitor logcat for CodeSandbox API call:
  - [x] `Building Reactylon code with CodeSandbox...`
  - [x] `Calling CodeSandbox API...`
  - [x] `CodeSandbox API response received`
- [ ] Verify no dependency resolution errors in logs
- [ ] Confirm sandbox URL returned: `Sandbox URL: https://codesandbox.io/p/sandbox/...`
- [ ] Build completes within 5-10 seconds

### Error Scenarios
- [ ] If build fails, check logcat for specific error
- [ ] Verify error message is user-friendly (not raw JSON)
- [ ] Test retry mechanism if network failure

---

## 4. Scene Rendering

- [ ] Navigate to Scene tab (should auto-switch if `[RUN_SCENE]` triggered)
- [ ] Verify CodeSandbox iframe loads successfully
- [ ] Confirm 3D scene visible in iframe
- [ ] Check that scene matches AI description (e.g., rotating cube)
- [ ] Verify scene is interactive (can rotate camera with mouse/touch)
- [ ] No cross-origin errors in logcat
- [ ] Canvas fills entire WebView viewport

### Visual Validation
- [ ] Lighting is visible (not a black screen)
- [ ] Meshes have correct colors
- [ ] Animations work if specified (rotation, movement)
- [ ] Ground plane visible if included in code
- [ ] Camera controls work (arc rotate, zoom, pan)

---

## 5. Screenshot Capture

- [ ] Wait 5 seconds after scene renders
- [ ] Check logcat for screenshot capture attempt:
  - [x] `Waiting 5 seconds before capturing screenshot...`
  - [x] `CAPTURING CANVAS SCREENSHOT`
  - [x] `Screenshot captured successfully` OR `Screenshot capture failed`
- [ ] Navigate to History tab
- [ ] Verify conversation entry exists in list
- [ ] Check thumbnail display:
  - [x] If screenshot succeeded: 80x80dp thumbnail with neon cyan border
  - [x] If screenshot failed: Placeholder camera icon (expected due to cross-origin)

**Note:** CodeSandbox iframes have cross-origin restrictions that typically prevent canvas screenshot. Placeholder icon is the expected behavior.

---

## 6. Advanced Tests

### Complex Scene with State
- [ ] Generate scene with React state management
- [ ] Example prompt: `Create a Reactylon scene with boxes that change color when clicked`
- [ ] Verify state hooks (useState) work correctly
- [ ] Test interactive elements respond to clicks

### Multiple Reactylon Scenes
- [ ] Generate first Reactylon scene
- [ ] Wait for build to complete
- [ ] Generate second different Reactylon scene
- [ ] Verify both scenes work independently
- [ ] Check conversation history shows both entries

### Library Switching
- [ ] Select Babylon.js library
- [ ] Generate a Babylon.js scene (plain JavaScript)
- [ ] Verify it works
- [ ] Switch back to Reactylon
- [ ] Generate another Reactylon scene (TypeScript/React)
- [ ] Verify switching doesn't cause issues
- [ ] Check that system prompts update correctly

### Example Code Execution
Run each example from ReactylonLibrary.kt:
- [ ] Floating Gems example
- [ ] Interactive Color Boxes example
- [ ] Animated Rainbow Spheres example
- [ ] Dynamic Material Playground example

---

## 7. Error Scenarios

### Invalid TypeScript Code
- [ ] Manually edit AI-generated code to remove required import
- [ ] Example: Remove `import { Color3, Vector3 } from '@babylonjs/core'`
- [ ] Attempt to build
- [ ] Verify TypeScript compilation error is shown
- [ ] Check error message is helpful

### CodeSandbox API Timeout
- [ ] Enable airplane mode to simulate offline
- [ ] Attempt to build Reactylon code
- [ ] Verify timeout error is handled gracefully
- [ ] Re-enable network and retry

### WebView Reload During Build
- [ ] Start Reactylon build
- [ ] Immediately switch to different tab and back
- [ ] Verify build completes or recovers gracefully

### Rapid Library Switching
- [ ] Switch between libraries rapidly:
  - Reactylon → Babylon.js → Reactylon → Three.js → Reactylon
- [ ] Verify no crashes or build queue issues
- [ ] Check system prompts update correctly each time

---

## 8. Performance & Memory

- [ ] Monitor app memory usage during builds (Android Studio Profiler)
- [ ] Verify memory doesn't grow significantly (< 50MB increase)
- [ ] Test with 5+ consecutive Reactylon builds
- [ ] Check for memory leaks (should return to baseline after GC)
- [ ] Verify app remains responsive during build
- [ ] No ANR (Application Not Responding) dialogs

---

## 9. Regression Testing (Other Libraries)

Ensure Reactylon re-enablement didn't break other libraries:

### Babylon.js
- [ ] Select Babylon.js library
- [ ] Generate scene: `Create a spinning torus`
- [ ] Verify direct injection works (no CodeSandbox build)
- [ ] Scene renders in WebView

### Three.js
- [ ] Select Three.js library
- [ ] Generate scene: `Create a red sphere`
- [ ] Verify direct injection works
- [ ] Scene renders correctly

### A-Frame
- [ ] Select A-Frame library
- [ ] Generate scene: `Create a VR scene with cubes`
- [ ] Verify direct injection works
- [ ] Scene renders with VR controls

### React Three Fiber
- [ ] Select React Three Fiber library
- [ ] Generate scene: `Create an animated box`
- [ ] Verify CodeSandbox build works
- [ ] Scene renders in iframe

---

## 10. Edge Cases

### Long TypeScript Code
- [ ] Generate complex scene with 500+ lines of code
- [ ] Verify CodeSandbox can handle large payloads
- [ ] Build completes without errors

### Special Characters in Code
- [ ] Generate scene with string templates, emojis, Unicode
- [ ] Example: `Create a box with label 'Hello 🌍 World'`
- [ ] Verify escaping works correctly

### Concurrent Builds
- [ ] Generate Reactylon scene
- [ ] Immediately generate another without waiting for first to complete
- [ ] Verify builds queue properly or second replaces first
- [ ] No race conditions or crashes

---

## Completion Criteria

### Must Pass (Blocking Issues)
- [x] Reactylon appears in library dropdown
- [x] AI generates valid React 18 + Reactylon code
- [x] CodeSandbox builds complete without dependency errors
- [x] 3D scenes render correctly in WebView iframe
- [x] No crashes or ANRs
- [x] No regression in other 3D libraries

### Should Pass (Important but Not Blocking)
- [x] Screenshot capture attempts (even if blocked)
- [x] Conversation history displays entries
- [x] Build times under 10 seconds
- [x] Interactive elements work in scenes
- [x] State management works correctly

### Nice to Have (Optional)
- [ ] Screenshot capture succeeds (unlikely due to cross-origin)
- [ ] Build times under 5 seconds
- [ ] Advanced XR features work (if generated by AI)

---

## Test Results Log

**Date:** __________
**Tester:** __________
**Build Version:** __________
**Device/Emulator:** __________

### Summary
- Total Tests: _____ / _____
- Passed: _____
- Failed: _____
- Blocked: _____

### Critical Issues Found
1.
2.
3.

### Notes


---

## Quick Smoke Test (5 minutes)

If you need a fast validation, run this minimal checklist:

1. [ ] Reactylon appears in dropdown
2. [ ] Generate simple scene: `Create a blue rotating cube`
3. [ ] CodeSandbox build succeeds
4. [ ] Scene renders in iframe
5. [ ] No crashes

If all 5 pass, Reactylon is working correctly. Run full checklist for comprehensive validation.

---

**Last Updated:** December 21, 2025
**Reactylon Version:** 3.0.0
**React Version:** 18.3.1
**Status:** Active - Re-enabled after React 18 type definition fix
