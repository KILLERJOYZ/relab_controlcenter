package com.example.relab_tool

import com.example.relab_tool.ui.normalizeForSearch
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchNormalizationTest {

    @Test
    fun testNormalization_Vietnamese() {
        val input = "Độ phân giải"
        val expected = "do phan giai"
        assertEquals(expected, input.normalizeForSearch())
    }

    @Test
    fun testNormalization_Russian() {
        val input = "Разрешение"
        val expected = "разрешение"
        assertEquals(expected, input.normalizeForSearch())
    }

    @Test
    fun testNormalization_Mixed() {
        val input = "Cảm biến Accelerometer"
        val expected = "cam bien accelerometer"
        assertEquals(expected, input.normalizeForSearch())
    }

    @Test
    fun testNormalization_Empty() {
        val input = ""
        val expected = ""
        assertEquals(expected, input.normalizeForSearch())
    }
}
