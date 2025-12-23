# Documentation Index

This folder contains all project documentation files organized by topic.

## Reactylon Implementation Documentation

### REACTYLON_IMPLEMENTATION_PLAN.md
Original implementation plan for re-enabling Reactylon with React 18 support. Contains:
- 4-phase implementation strategy
- Technical background and requirements
- Risk assessment and rollback strategy
- Success criteria and monitoring

### REACTYLON_TESTING_CHECKLIST.md
Comprehensive testing checklist for Reactylon integration. Includes:
- Pre-test setup instructions
- Library selection and code generation tests
- CodeSandbox build verification
- Scene rendering checks
- Screenshot capture tests
- Regression testing for other libraries
- Error scenarios and edge cases
- Quick 5-minute smoke test

### REACTYLON_CODESANDBOX_ISSUES.md
Detailed analysis of CodeSandbox infrastructure failures. Documents:
- What actually happening vs expected behavior
- JavaScript errors explained (TypeScript worker, Monaco editor)
- Why Reactylon was originally disabled
- Core problem with CodeSandbox as third-party service
- Recommendations (accept limitation, disable, or build local bundler)
- Log analysis and testing procedures

### REACTYLON_FINAL_STATUS_COMMIT.md
Complete summary of Reactylon implementation journey. Contains:
- Full 4-phase implementation journey
- React 19 to React 18 migration (completed)
- WebView crash prevention (completed)
- CodeSandbox infrastructure failures (unfixable)
- Final decision to disable Reactylon
- Files modified and created summary
- Future re-enablement criteria

## WebView Crash Prevention Documentation

### WEBVIEW_CRASH_FIX.md
Multi-layer WebView hardening implementation plan with 7 layers:
1. Multiprocess WebView mode
2. Increased memory limits
3. Crash detection and recovery
4. Async loading (ANR prevention)
5. JavaScript error isolation
6. Memory monitoring and cleanup
7. Fallback loading strategy

### WEBVIEW_CRASH_FIX_SUMMARY.md
Quick reference for WebView crash prevention. Includes:
- Problem analysis (SIGTRAP crashes)
- 4 implemented fixes
- Before/after comparison
- Testing checklist
- Expected results

### WEBVIEW_CRASH_FIX_COMMIT.md
Complete commit documentation for WebView crash prevention. Contains:
- Detailed problem statement
- 4-layer hardening solution
- Implementation details for each layer
- Code examples and explanations
- Files modified summary
- Testing checklist and expected results

## Styling Documentation

### STYLING_PROGRESS.md
Multi-session tracking document for neon cyberpunk styling transformation. Contains:
- Branding requirements summary
- 7-phase implementation progress
- Session notes with code examples
- Build status and error resolutions
- Technical notes (performance, accessibility)
- Quick resume guide for context recovery

## Legacy and Archived Documentation

### ANDROID_README.md
Previous Android-specific README (archived)

### BUILD_FIX_APPLIED.md
Historical build fix documentation (archived)

### CLAUDE_OLD.md
Previous version of CLAUDE.md project instructions (archived)

### checklist.md
General development checklist (archived)

## Documentation Organization

**Active Documentation** (frequently referenced):
- REACTYLON_FINAL_STATUS_COMMIT.md - Current Reactylon status
- REACTYLON_CODESANDBOX_ISSUES.md - CodeSandbox failure analysis
- WEBVIEW_CRASH_FIX_SUMMARY.md - Crash prevention quick reference
- STYLING_PROGRESS.md - Ongoing styling project tracker

**Reference Documentation** (implementation details):
- REACTYLON_IMPLEMENTATION_PLAN.md - How Reactylon was re-enabled
- REACTYLON_TESTING_CHECKLIST.md - Testing procedures
- WEBVIEW_CRASH_FIX.md - Detailed hardening plan
- WEBVIEW_CRASH_FIX_COMMIT.md - Crash prevention implementation

**Archived Documentation** (historical):
- ANDROID_README.md
- BUILD_FIX_APPLIED.md
- CLAUDE_OLD.md
- checklist.md

## Quick Navigation

### Need to understand Reactylon status?
→ Start with REACTYLON_FINAL_STATUS_COMMIT.md

### Need to debug CodeSandbox failures?
→ See REACTYLON_CODESANDBOX_ISSUES.md

### Need to understand WebView crashes?
→ Start with WEBVIEW_CRASH_FIX_SUMMARY.md

### Need to continue styling work?
→ See STYLING_PROGRESS.md

### Need to test Reactylon if re-enabled?
→ Use REACTYLON_TESTING_CHECKLIST.md

---

**Note**: Main project documentation (CLAUDE.md, README.md) remains in root directory.
**Commit messages**: See COMMIT_MESSAGE.md in root directory.

Last updated: 2025-12-21
