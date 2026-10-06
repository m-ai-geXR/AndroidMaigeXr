package com.xraiassistant.ui.components

import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Hardware keys must reach the running Nova64 cart, not the hidden code editor.
 *
 * Loads the real playground the way SceneScreen does (same base URL, so the page is
 * same-origin with the console iframe), runs the starter demo, then sends a real
 * KEYCODE_M through the instrumentation and checks the console iframe received it.
 *
 * Needs network: Monaco comes from unpkg and the console from nova64.io.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class Nova64KeyboardFocusTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var scenario: ActivityScenario<ComponentActivity>
    private lateinit var webView: WebView

    @Before
    fun loadPlayground() {
        val pageLoaded = CountDownLatch(1)
        scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            webView = WebView(activity).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                isFocusable = true
                isFocusableInTouchMode = true
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        view?.requestFocus()
                        pageLoaded.countDown()
                    }
                }
            }
            activity.setContentView(webView)

            val html = activity.assets.open("playground-nova64.html").bufferedReader().use { it.readText() }
            webView.loadDataWithBaseURL(NOVA64_BASE_URL, html, "text/html", "UTF-8", null)
        }
        assertTrue("playground page did not load", pageLoaded.await(30, TimeUnit.SECONDS))
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    @Test
    fun keyPressesReachTheConsoleAfterADemoRuns() {
        assertTrue("editor never became ready", waitFor("window.editorReady === true", 60_000))

        // Same path as picking a demo from the dropdown.
        evaluate("loadExample('starter')")
        assertTrue("console never became ready", waitFor("window.runnerReady === true", 60_000))
        assertTrue(
            "console iframe does not have focus after the demo ran",
            waitFor("document.activeElement && document.activeElement.id === 'novaRunner'", 15_000)
        )

        // The console re-boots when it re-fits its size, which replaces the iframe's
        // window, so re-install the probe and retry a few times rather than racing it.
        // Nova64 reads e.code on its window, so that is what the probe checks.
        var received = false
        repeat(5) {
            if (received) return@repeat
            evaluate(
                """
                (function () {
                    var w = document.getElementById('novaRunner').contentWindow;
                    w.__keys = [];
                    w.addEventListener('keydown', function (e) { w.__keys.push(e.code); }, true);
                    return true;
                })()
                """.trimIndent()
            )
            sendHardwareKey(KeyEvent.KEYCODE_M, SCANCODE_M)
            received = waitFor(
                "(document.getElementById('novaRunner').contentWindow.__keys || []).indexOf('KeyM') >= 0",
                2_000
            )
        }
        assertTrue("KeyM never reached the Nova64 console. State: ${describeInputState()}", received)

        assertTrue(
            "the console's canvas does not have focus. State: ${describeInputState()}",
            waitFor(CANVAS_FOCUSED, 2_000)
        )

        // Tab used to move focus out of the console to the toolbar above it.
        sendHardwareKey(KeyEvent.KEYCODE_TAB, SCANCODE_TAB)
        Thread.sleep(500)
        assertTrue(
            "Tab moved focus out of the console. State: ${describeInputState()}",
            evaluate("(function () { try { return !!($CANVAS_FOCUSED); } catch (e) { return false; } })()") == "true"
        )
    }

    @Test
    fun sceneScreenshotIsARealFrameNotBlack() {
        assertTrue("editor never became ready", waitFor("window.editorReady === true", 60_000))
        evaluate("loadExample('starter')")
        assertTrue("console never became ready", waitFor("window.runnerReady === true", 60_000))

        // The app calls this a few seconds after a cart starts; it must return a
        // JPEG of what the console drew, not null and not a black frame.
        val gotShot = waitFor(
            "(function () { var s = captureCanvasScreenshot(); return typeof s === 'string' && s.indexOf('data:image/jpeg') === 0 && s.length > 2000; })()",
            20_000
        )
        assertTrue("captureCanvasScreenshot() never returned a usable frame", gotShot)
    }

    @Test
    fun commandLineRunsCodeInTheConsoleAndListsItsGlobals() {
        assertTrue("editor never became ready", waitFor("window.editorReady === true", 60_000))
        evaluate("loadExample('starter')")
        assertTrue("console never became ready", waitFor("window.runnerReady === true", 60_000))
        assertTrue("console API never appeared", waitFor(
            "typeof document.getElementById('novaRunner').contentWindow.nova64 === 'object'", 30_000
        ))

        val script = InstrumentationRegistry.getInstrumentation().targetContext
            .assets.open("playground-commandline.js").bufferedReader().use { it.readText() }
        evaluate("$script\n;window.maigeCommandLine.setEnabled(true)")
        assertTrue("command line is not shown", waitFor(
            "!document.getElementById('maige-cl').hidden && !!document.getElementById('maige-cl-input')", 2_000
        ))

        // Runs in the console's scope, not the playground page's.
        evaluate("maigeCommandLine.run('typeof nova64.scene')")
        assertTrue("command did not run in the console frame. Output: ${commandLineOutput()}", waitFor(
            "document.getElementById('maige-cl-out').textContent.indexOf('\"object\"') >= 0", 2_000
        ))

        evaluate("maigeCommandLine.run('globals().indexOf(\"nova64\") >= 0')")
        assertTrue("globals() does not list nova64. Output: ${commandLineOutput()}", waitFor(
            "/true\\s*$/.test(document.getElementById('maige-cl-out').textContent)", 2_000
        ))

        // Typing into the line must not drive the game.
        evaluate(
            """
            (function () {
                var w = document.getElementById('novaRunner').contentWindow;
                w.__keys = [];
                w.addEventListener('keydown', function (e) { w.__keys.push(e.code); }, true);
                document.getElementById('maige-cl-input').focus();
                return true;
            })()
            """.trimIndent()
        )
        sendHardwareKey(KeyEvent.KEYCODE_M, SCANCODE_M)
        Thread.sleep(500)
        assertTrue(
            "a key typed into the command line reached the game",
            evaluate("document.getElementById('novaRunner').contentWindow.__keys.length") == "0"
        )

        evaluate("maigeCommandLine.setEnabled(false)")
        assertTrue("toggle off did not hide the command line",
            waitFor("document.getElementById('maige-cl').hidden === true", 2_000))
    }

    private fun commandLineOutput(): String =
        evaluate("(document.getElementById('maige-cl-out') || {}).textContent || ''")

    /** A key as a physical keyboard sends it, scan code included, so the page gets a real e.code. */
    private fun sendHardwareKey(keyCode: Int, scanCode: Int) {
        val now = SystemClock.uptimeMillis()
        for (action in listOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP)) {
            instrumentation.sendKeySync(
                KeyEvent(now, now, action, keyCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, scanCode, 0, InputDevice.SOURCE_KEYBOARD)
            )
        }
    }

    /** Who holds focus at each level, and what sits on top of the scene's centre. */
    private fun describeInputState(): String {
        var viewFocus = ""
        instrumentation.runOnMainSync {
            viewFocus = "webView.hasFocus=${webView.hasFocus()} isFocused=${webView.isFocused} " +
                "windowFocus=${webView.hasWindowFocus()}"
        }
        val page = evaluate(
            """
            (function () {
                function tag(el) {
                    if (!el) return 'null';
                    var cs = getComputedStyle(el);
                    return el.tagName + '#' + el.id + '.' + el.className +
                        '[pe=' + cs.pointerEvents + ' op=' + cs.opacity + ' z=' + cs.zIndex + ' pos=' + cs.position + ']';
                }
                var f = document.getElementById('novaRunner');
                var r = f.getBoundingClientRect();
                var x = r.left + r.width / 2, y = r.top + r.height / 2;
                var stack = (document.elementsFromPoint ? document.elementsFromPoint(x, y) : []).slice(0, 6).map(tag);
                var inner = '';
                try {
                    var d = f.contentDocument;
                    inner = 'frameDocFocus=' + d.hasFocus() + ' frameActive=' + tag(d.activeElement) +
                        ' frameTop=' + tag(d.elementFromPoint(r.width / 2, r.height / 2));
                } catch (e) { inner = 'frame inaccessible: ' + e.message; }
                return 'docFocus=' + document.hasFocus() + ' active=' + tag(document.activeElement) +
                    ' frameRect=' + Math.round(r.width) + 'x' + Math.round(r.height) +
                    ' stackAtCentre=' + stack.join(' > ') + ' ' + inner +
                    ' keysSeen=' + JSON.stringify(f.contentWindow.__keys || null);
            })()
            """.trimIndent()
        )
        return "$viewFocus | $page"
    }

    private fun evaluate(js: String): String {
        val done = CountDownLatch(1)
        var result: String? = null
        instrumentation.runOnMainSync {
            webView.evaluateJavascript(js) { value ->
                result = value
                done.countDown()
            }
        }
        done.await(10, TimeUnit.SECONDS)
        return result ?: "null"
    }

    private fun waitFor(condition: String, timeoutMillis: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            if (evaluate("(function () { try { return !!($condition); } catch (e) { return false; } })()") == "true") {
                return true
            }
            Thread.sleep(250)
        }
        return false
    }

    private companion object {
        const val NOVA64_BASE_URL = "https://nova64.io/maigexr-playground/"

        // Linux input scan codes (KEY_M, KEY_TAB); Chromium derives e.code from these.
        const val SCANCODE_M = 50
        const val SCANCODE_TAB = 15

        const val CANVAS_FOCUSED =
            "document.activeElement && document.activeElement.id === 'novaRunner' && " +
                "document.getElementById('novaRunner').contentDocument.activeElement.tagName === 'CANVAS'"
    }
}
