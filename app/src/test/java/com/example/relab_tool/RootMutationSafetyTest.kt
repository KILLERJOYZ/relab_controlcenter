package com.example.relab_tool

import com.example.relab_tool.utils.RootUtils
import org.junit.Assert.assertFalse
import org.junit.Test

class RootMutationSafetyTest {
    @Test fun unsupportedRootWritesAreFailClosed() {
        assertFalse(RootUtils.setChargeLimit(80))
        assertFalse(RootUtils.setChargeLimit(-1))
        assertFalse(RootUtils.setGovernor("performance"))
        assertFalse(RootUtils.runShellCommand("exit 0"))
    }
}
