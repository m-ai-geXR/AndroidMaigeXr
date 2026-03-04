// Auto-detect WSL vs Windows and fix SDK + JDK paths so the same project
// builds with ./gradlew (WSL/Linux) and gradlew.bat (Windows) without
// manually editing local.properties or gradle.properties each time you switch.

val onLinux = System.getProperty("os.name", "").lowercase().contains("linux")

fun winToWsl(winPath: String): String {
    val cleaned = winPath.replace("\\:", ":").replace("\\\\", "/").replace("\\", "/")
    return "/mnt/" + cleaned[0].lowercaseChar() + cleaned.substring(2)
}

fun wslToWin(wslPath: String): String {
    val drive = wslPath[5].uppercaseChar()
    return "$drive:\\" + wslPath.substring(7).replace("/", "\\")
}

// --- Fix sdk.dir in local.properties ---
val localPropsFile = file("local.properties")
if (localPropsFile.exists()) {
    val props = java.util.Properties()
    localPropsFile.inputStream().use { props.load(it) }
    val currentSdk = props.getProperty("sdk.dir") ?: ""

    if (onLinux && (currentSdk.startsWith("C:", ignoreCase = true) || currentSdk.startsWith("C\\:"))) {
        val wslPath = winToWsl(currentSdk)
        if (file(wslPath).isDirectory) {
            props.setProperty("sdk.dir", wslPath)
            localPropsFile.writer().use { props.store(it, "Auto-configured for WSL") }
            println("[\u2713] sdk.dir -> $wslPath")
        }
    } else if (!onLinux && currentSdk.startsWith("/mnt/")) {
        val winPath = wslToWin(currentSdk)
        props.setProperty("sdk.dir", winPath)
        localPropsFile.writer().use { props.store(it, "Auto-configured for Windows") }
        println("[\u2713] sdk.dir -> $winPath")
    }
}

// --- Fix org.gradle.java.home in gradle.properties ---
val gradlePropsFile = file("gradle.properties")
if (gradlePropsFile.exists()) {
    val props = java.util.Properties()
    gradlePropsFile.inputStream().use { props.load(it) }
    val currentJdk = props.getProperty("org.gradle.java.home") ?: ""

    if (onLinux && (currentJdk.startsWith("C:", ignoreCase = true) || currentJdk.startsWith("C\\:"))) {
        val wslPath = winToWsl(currentJdk)
        if (file(wslPath).isDirectory) {
            props.setProperty("org.gradle.java.home", wslPath)
            gradlePropsFile.writer().use { props.store(it, "Auto-configured for WSL") }
            println("[\u2713] java.home -> $wslPath (restart Gradle daemon to apply)")
        }
    } else if (!onLinux && currentJdk.startsWith("/mnt/")) {
        val winPath = wslToWin(currentJdk)
        props.setProperty("org.gradle.java.home", winPath)
        gradlePropsFile.writer().use { props.store(it, "Auto-configured for Windows") }
        println("[\u2713] java.home -> $winPath (restart Gradle daemon to apply)")
    }
}

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "XRAiAssistant"
include(":app")
