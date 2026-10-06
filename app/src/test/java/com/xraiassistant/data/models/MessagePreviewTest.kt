package com.xraiassistant.data.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessagePreviewTest {

    @Test
    fun `keeps the prose before the code and drops markdown`() {
        val reply = "## The Lantern Sea\n\nAn **asset-free** voyage. Press `N` to skip.\n\n[INSERT_CODE]```javascript\nlet S;\n```"
        assertEquals("The Lantern Sea An asset-free voyage. Press N to skip.", MessagePreview.from(reply))
    }

    @Test
    fun `keeps link text without the url`() {
        assertEquals("See the docs for more.", MessagePreview.from("See [the docs](https://example.com) for more."))
    }

    @Test
    fun `returns null when the reply is only code`() {
        assertNull(MessagePreview.from("```js\nconst a = 1;\n```"))
        assertNull(MessagePreview.from(null))
    }

    @Test
    fun `long previews are shortened`() {
        val preview = MessagePreview.from("word ".repeat(100))!!
        assertTrue(preview.length <= MessagePreview.MAX_LENGTH)
        assertTrue(preview.endsWith("…"))
    }
}
