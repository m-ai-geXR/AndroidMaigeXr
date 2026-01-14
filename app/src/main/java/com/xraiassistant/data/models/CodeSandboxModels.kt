package com.xraiassistant.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * CodeSandbox API Models
 *
 * Used for creating sandboxes programmatically via the Define API:
 * https://codesandbox.io/docs/api
 */

/**
 * CodeSandbox file structure
 */
@JsonClass(generateAdapter = true)
data class CodeSandboxFile(
    @Json(name = "content") val content: String,
    @Json(name = "isBinary") val isBinary: Boolean = false
)

/**
 * Request to create a new CodeSandbox sandbox
 */
@JsonClass(generateAdapter = true)
data class CodeSandboxDefineRequest(
    @Json(name = "files") val files: Map<String, CodeSandboxFile>
)

/**
 * Response from CodeSandbox Define API
 */
@JsonClass(generateAdapter = true)
data class CodeSandboxDefineResponse(
    @Json(name = "sandbox_id") val sandboxId: String,
    @Json(name = "id") val id: String? = null // Alternative field name
)

/**
 * Helper to build React Three Fiber sandbox
 */
object CodeSandboxTemplates {

    /**
     * Creates a React Three Fiber sandbox with the given code
     *
     * @param code The React Three Fiber component code (should export default App)
     * @return Map of files for CodeSandbox
     */
    fun createReactThreeFiberSandbox(code: String): Map<String, CodeSandboxFile> {
        return mapOf(
            "/package.json" to CodeSandboxFile(
                content = """
                    {
                      "name": "maigeXR-playground",
                      "version": "1.0.0",
                      "description": "AI-generated React Three Fiber scene",
                      "main": "index.js",
                      "dependencies": {
                        "react": "^18.2.0",
                        "react-dom": "^18.2.0",
                        "@react-three/fiber": "^8.15.0",
                        "@react-three/drei": "^9.88.0",
                        "@react-three/postprocessing": "^2.16.3",
                        "three": "^0.158.0"
                      },
                      "devDependencies": {
                        "@types/react": "^18.2.0",
                        "@types/react-dom": "^18.2.0",
                        "typescript": "^5.0.0"
                      }
                    }
                """.trimIndent()
            ),
            "/index.html" to CodeSandboxFile(
                content = """
                    <!DOCTYPE html>
                    <html lang="en">
                      <head>
                        <meta charset="UTF-8" />
                        <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                        <title>m{ai}geXR Playground</title>
                        <style>
                          body { margin: 0; overflow: hidden; }
                          #root { width: 100vw; height: 100vh; }
                        </style>
                      </head>
                      <body>
                        <div id="root"></div>
                        <script type="module" src="/src/index.tsx"></script>
                      </body>
                    </html>
                """.trimIndent()
            ),
            "/src/index.tsx" to CodeSandboxFile(
                content = """
                    import React from 'react';
                    import { createRoot } from 'react-dom/client';
                    import App from './App';

                    // Fullscreen wrapper to ensure canvas fills entire viewport
                    const FullscreenWrapper = () => (
                      <div style={{
                        position: 'fixed',
                        top: 0,
                        left: 0,
                        right: 0,
                        bottom: 0,
                        width: '100vw',
                        height: '100vh',
                        margin: 0,
                        padding: 0,
                        overflow: 'hidden',
                        pointerEvents: 'none'
                      }}>
                        <div style={{ pointerEvents: 'auto', width: '100%', height: '100%' }}>
                          <App />
                        </div>
                      </div>
                    );

                    const root = createRoot(document.getElementById('root')!);
                    root.render(<FullscreenWrapper />);
                """.trimIndent()
            ),
            "/src/App.tsx" to CodeSandboxFile(
                content = code
            ),
            "/tsconfig.json" to CodeSandboxFile(
                content = """
                    {
                      "compilerOptions": {
                        "target": "ES2020",
                        "lib": ["ES2020", "DOM", "DOM.Iterable"],
                        "jsx": "react-jsx",
                        "module": "ESNext",
                        "moduleResolution": "bundler",
                        "strict": true,
                        "esModuleInterop": true,
                        "skipLibCheck": true,
                        "forceConsistentCasingInFileNames": true
                      },
                      "include": ["src"]
                    }
                """.trimIndent()
            )
        )
    }

    /**
     * Creates a Three.js sandbox (vanilla JS, no React)
     */
    fun createThreeJSSandbox(code: String): Map<String, CodeSandboxFile> {
        return mapOf(
            "/package.json" to CodeSandboxFile(
                content = """
                    {
                      "name": "maigeXR-threejs",
                      "version": "1.0.0",
                      "description": "AI-generated Three.js scene",
                      "main": "index.js",
                      "dependencies": {
                        "three": "^0.158.0"
                      }
                    }
                """.trimIndent()
            ),
            "/index.html" to CodeSandboxFile(
                content = """
                    <!DOCTYPE html>
                    <html lang="en">
                      <head>
                        <meta charset="UTF-8" />
                        <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                        <title>m{ai}geXR Three.js Playground</title>
                        <style>
                          body { margin: 0; overflow: hidden; }
                        </style>
                      </head>
                      <body>
                        <script type="module" src="/src/index.js"></script>
                      </body>
                    </html>
                """.trimIndent()
            ),
            "/src/index.js" to CodeSandboxFile(
                content = code
            )
        )
    }

    /**
     * Creates an A-Frame sandbox
     */
    fun createAFrameSandbox(code: String): Map<String, CodeSandboxFile> {
        return mapOf(
            "/package.json" to CodeSandboxFile(
                content = """
                    {
                      "name": "maigeXR-aframe",
                      "version": "1.0.0",
                      "description": "AI-generated A-Frame scene",
                      "dependencies": {
                        "aframe": "^1.5.0"
                      }
                    }
                """.trimIndent()
            ),
            "/index.html" to CodeSandboxFile(
                content = code
            )
        )
    }

    /**
     * Creates a Reactylon sandbox with the given code
     *
     * @param code The Reactylon component code (should export default App or render directly)
     * @return Map of files for CodeSandbox
     */
    fun createReactylonSandbox(code: String): Map<String, CodeSandboxFile> {
        return mapOf(
            "/package.json" to CodeSandboxFile(
                content = """
                    {
                      "name": "maigeXR-reactylon",
                      "version": "1.0.0",
                      "description": "AI-generated Reactylon scene",
                      "main": "index.js",
                      "dependencies": {
                        "react": "^18.3.1",
                        "react-dom": "^18.3.1",
                        "react-reconciler": "^0.29.2",
                        "reactylon": "^3.2.1",
                        "@babylonjs/core": "^8.0.0",
                        "@babylonjs/gui": "^8.0.0",
                        "@babylonjs/havok": "^1.3.10"
                      },
                      "devDependencies": {
                        "@types/react": "18.3.1",
                        "@types/react-dom": "18.3.1",
                        "typescript": "5.4.0"
                      }
                    }
                """.trimIndent()
            ),
            "/index.html" to CodeSandboxFile(
                content = """
                    <!DOCTYPE html>
                    <html lang="en">
                      <head>
                        <meta charset="UTF-8" />
                        <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                        <title>m{ai}geXR Reactylon Playground</title>
                        <style>
                          body { margin: 0; overflow: hidden; }
                          #root { width: 100vw; height: 100vh; }
                          canvas { width: 100%; height: 100%; display: block; }
                        </style>
                      </head>
                      <body>
                        <div id="root"></div>
                        <script type="module" src="/src/App.tsx"></script>
                      </body>
                    </html>
                """.trimIndent()
            ),
            "/src/App.tsx" to CodeSandboxFile(
                content = code
            ),
            "/tsconfig.json" to CodeSandboxFile(
                content = """
                    {
                      "compilerOptions": {
                        "target": "ES2020",
                        "lib": ["ES2020", "DOM", "DOM.Iterable"],
                        "jsx": "react-jsx",
                        "module": "ESNext",
                        "moduleResolution": "bundler",
                        "strict": true,
                        "esModuleInterop": true,
                        "skipLibCheck": true,
                        "forceConsistentCasingInFileNames": true
                      },
                      "include": ["src"]
                    }
                """.trimIndent()
            )
        )
    }
}
