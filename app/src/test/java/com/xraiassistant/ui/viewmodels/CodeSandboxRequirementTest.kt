package com.xraiassistant.ui.viewmodels

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** React Three Fiber and Reactylon always run on CodeSandbox, and users are told so. */
class CodeSandboxRequirementTest {

    @Test
    fun onlyBuildFrameworksRequireCodeSandbox() {
        assertTrue(requiresCodeSandbox("reactThreeFiber"))
        assertTrue(requiresCodeSandbox("reactylon"))
        for (id in listOf("babylonjs", "threejs", "aframe", "nova64")) assertFalse(id, requiresCodeSandbox(id))
    }

    @Test
    fun noticeNamesCodeSandbox() {
        assertTrue(codeSandboxNoticeFor("reactThreeFiber").contains("CodeSandbox"))
        assertTrue(codeSandboxNoticeFor("reactylon").startsWith("Reactylon"))
    }
}
