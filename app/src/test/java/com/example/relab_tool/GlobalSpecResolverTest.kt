package com.example.relab_tool

import com.example.relab_tool.utils.spec.GlobalSpecResolver
import com.example.relab_tool.utils.spec.GlobalSpecResolver.ParsedCameraSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GlobalSpecResolverTest {

    @Test
    fun testParseLensLine_MainWide() {
        val line = "50 MP, f/1.8, 24mm (wide), 1/1.56\", 1.0µm, PDAF, OIS"
        val spec = GlobalSpecResolver.parseLensLine(line, "back")
        assertNotNull(spec)
        spec?.let {
            assertEquals("back", it.facing)
            assertEquals("50 MP", it.res)
            assertEquals(1.8f, it.aperture)
            assertEquals(24.0, it.equiv ?: 0.0, 0.01)
            assertEquals("1/1.56\"", it.sensorSize)
            assertEquals(1.0f, it.pixelSize ?: 0f, 0.01f)
            assertEquals("wide", it.role)
        }
    }

    @Test
    fun testParseLensLine_Ultrawide() {
        val line = "12 MP, f/2.2, 13mm, 120˚ (ultrawide), 1/3.06\", 1.12 um"
        val spec = GlobalSpecResolver.parseLensLine(line, "back")
        assertNotNull(spec)
        spec?.let {
            assertEquals("back", it.facing)
            assertEquals("12 MP", it.res)
            assertEquals(2.2f, it.aperture)
            assertEquals(13.0, it.equiv ?: 0.0, 0.01)
            assertEquals("1/3.06\"", it.sensorSize)
            assertEquals(1.12f, it.pixelSize ?: 0f, 0.01f)
            assertEquals("ultrawide", it.role)
        }
    }

    @Test
    fun testParseLensLine_Telephoto() {
        val line = "48 MP, f/3.5, 120mm (periscope telephoto), 1/2.0\", 0.8µm, PDAF, OIS, 5x optical zoom"
        val spec = GlobalSpecResolver.parseLensLine(line, "back")
        assertNotNull(spec)
        spec?.let {
            assertEquals("back", it.facing)
            assertEquals("48 MP", it.res)
            assertEquals(3.5f, it.aperture)
            assertEquals(120.0, it.equiv ?: 0.0, 0.01)
            assertEquals("1/2.0\"", it.sensorSize)
            assertEquals(0.8f, it.pixelSize ?: 0f, 0.01f)
            assertEquals("telephoto", it.role)
        }
    }

    @Test
    fun testParseLensLine_Front() {
        val line = "32 MP, f/2.2, 26mm (wide), 1/2.74\", 0.8µm"
        val spec = GlobalSpecResolver.parseLensLine(line, "front")
        assertNotNull(spec)
        spec?.let {
            assertEquals("front", it.facing)
            assertEquals("32 MP", it.res)
            assertEquals(2.2f, it.aperture)
            assertEquals(26.0, it.equiv ?: 0.0, 0.01)
            assertEquals("1/2.74\"", it.sensorSize)
            assertEquals(0.8f, it.pixelSize ?: 0f, 0.01f)
            assertEquals("wide", it.role)
        }
    }

    @Test
    fun testParseLensLine_NoMegapixel() {
        val line = "PDAF, OIS, 5x optical zoom"
        val spec = GlobalSpecResolver.parseLensLine(line, "back")
        assertNull(spec)
    }

    @Test
    fun testParseGsmArenaHtml() {
        val mockHtml = """
            <html>
            <body>
            <table>
            <tr>
            <th>Main Camera</th>
            <td class="nfo">50 MP, f/1.8, 24mm (wide), 1/1.56", 1.0µm<br>12 MP, f/2.2, 13mm (ultrawide)</td>
            </tr>
            </table>
            <table>
            <tr>
            <th>Selfie camera</th>
            <td class="nfo">32 MP, f/2.2, 26mm (wide), 1/2.74", 0.8µm</td>
            </tr>
            </table>
            </body>
            </html>
        """.trimIndent()

        val parsed = GlobalSpecResolver.parseGsmArenaHtml(mockHtml)
        assertEquals(3, parsed.size)

        val first = parsed[0]
        assertEquals("back", first.facing)
        assertEquals("50 MP", first.res)
        assertEquals(24.0, first.equiv ?: 0.0, 0.01)
        assertEquals("wide", first.role)

        val second = parsed[1]
        assertEquals("back", second.facing)
        assertEquals("12 MP", second.res)
        assertEquals(13.0, second.equiv ?: 0.0, 0.01)
        assertEquals("ultrawide", second.role)

        val third = parsed[2]
        assertEquals("front", third.facing)
        assertEquals("32 MP", third.res)
        assertEquals(26.0, third.equiv ?: 0.0, 0.01)
        assertEquals("wide", third.role)
    }

    @Test
    fun testFindBestMatch() {
        val specs = listOf(
            ParsedCameraSpec("back", "50 MP", "", 24.0, 1.8f, 1.0f, "1/1.56\"", "wide"),
            ParsedCameraSpec("back", "12 MP", "", 13.0, 2.2f, 1.12f, "1/3.06\"", "ultrawide"),
            ParsedCameraSpec("back", "10 MP", "", 70.0, 2.4f, 1.0f, "1/3.94\"", "telephoto"),
            ParsedCameraSpec("front", "32 MP", "", 26.0, 2.2f, 0.8f, "1/2.74\"", "wide")
        )

        // Match by focal length proximity
        val match1 = GlobalSpecResolver.findBestMatch(specs, "back", 4.5f, 26f, "wide")
        assertNotNull(match1)
        assertEquals("50 MP", match1?.res) // 24mm is closest to 26mm

        val match2 = GlobalSpecResolver.findBestMatch(specs, "back", 2.0f, 14f, "ultrawide")
        assertNotNull(match2)
        assertEquals("12 MP", match2?.res) // 13mm is closest to 14mm

        // Match by role if focal length is null
        val match3 = GlobalSpecResolver.findBestMatch(specs, "back", null, null, "telephoto")
        assertNotNull(match3)
        assertEquals("10 MP", match3?.res)
    }
}
