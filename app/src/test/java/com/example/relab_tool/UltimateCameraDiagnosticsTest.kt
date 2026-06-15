package com.example.relab_tool

import com.example.relab_tool.utils.UltimateCameraDiagnostics
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.sqrt

class UltimateCameraDiagnosticsTest {

    @Test
    fun testConvertDiagonalToOpticalFormat_StandardSensors() {
        // Standard high-end 1-inch sensor (e.g. Sony IMX989, size ~ 13.2 x 8.8 mm)
        // Diagonal = sqrt(13.2^2 + 8.8^2) = 15.86 mm
        val diagIMX989 = sqrt(13.2 * 13.2 + 8.8 * 8.8)
        assertEquals("1\"", UltimateCameraDiagnostics.convertDiagonalToOpticalFormat(diagIMX989))

        // Samsung GN2 (1/1.12", size 11.2 x 8.4 mm)
        // Diagonal = sqrt(11.2^2 + 8.4^2) = 14.0 mm
        val diagGN2 = sqrt(11.2 * 11.2 + 8.4 * 8.4)
        assertEquals("1/1.12\"", UltimateCameraDiagnostics.convertDiagonalToOpticalFormat(diagGN2))

        // Sony IMX707 (1/1.28", size 9.76 x 7.32 mm)
        // Diagonal = sqrt(9.76^2 + 7.32^2) = 12.2 mm
        val diagIMX707 = sqrt(9.76 * 9.76 + 7.32 * 7.32)
        assertEquals("1/1.28\"", UltimateCameraDiagnostics.convertDiagonalToOpticalFormat(diagIMX707))

        // Samsung HP2 (1/1.3", size 9.6 x 7.2 mm, diagonal 12.0 mm)
        // Historically marketed as 1/1.33" or 1/1.3"
        val diagHP2 = sqrt(9.6 * 9.6 + 7.2 * 7.2)
        // 12.0 mm falls very close to standard 1/1.33" (12.00 mm)
        assertEquals("1/1.33\"", UltimateCameraDiagnostics.convertDiagonalToOpticalFormat(diagHP2))

        // Sony IMX766 (1/1.56", size 8.19 x 6.14 mm)
        // Diagonal = sqrt(8.19^2 + 6.14^2) = 10.24 mm
        val diagIMX766 = sqrt(8.19 * 8.19 + 6.14 * 6.14)
        assertEquals("1/1.56\"", UltimateCameraDiagnostics.convertDiagonalToOpticalFormat(diagIMX766))

        // Sony IMX686 (1/1.7", size 7.6 x 5.7 mm)
        // Diagonal = sqrt(7.6^2 + 5.7^2) = 9.5 mm
        val diagIMX686 = sqrt(7.6 * 7.6 + 5.7 * 5.7)
        assertEquals("1/1.7\"", UltimateCameraDiagnostics.convertDiagonalToOpticalFormat(diagIMX686))

        // Sony IMX586 (1/2", size 6.4 x 4.8 mm)
        // Diagonal = sqrt(6.4^2 + 4.8^2) = 8.0 mm
        val diagIMX586 = sqrt(6.4 * 6.4 + 4.8 * 4.8)
        assertEquals("1/2\"", UltimateCameraDiagnostics.convertDiagonalToOpticalFormat(diagIMX586))

        // Sony IMX363 (1/2.55", size 5.64 x 4.23 mm, diagonal = 7.05 mm)
        val diagIMX363 = sqrt(5.64 * 5.64 + 4.23 * 4.23)
        assertEquals("1/2.55\"", UltimateCameraDiagnostics.convertDiagonalToOpticalFormat(diagIMX363))

        // Sony IMX355 (1/4", size 4.0 x 3.0 mm, diagonal = 5.0 mm)
        val diagIMX355 = sqrt(4.0 * 4.0 + 3.0 * 3.0)
        assertEquals("1/3.2\"", UltimateCameraDiagnostics.convertDiagonalToOpticalFormat(diagIMX355)) // 16.0 / 5.0 = 3.2. This is correct!
    }

    @Test
    fun testConvertDiagonalToOpticalFormat_UnusualSensors() {
        // Test dynamic fallback for an unusual sensor diagonal (e.g. 15.0 mm)
        // 16.0 / 15.0 = 1.0666 -> rounds to 1.07 -> "1/1.07\""
        assertEquals("1/1.07\"", UltimateCameraDiagnostics.convertDiagonalToOpticalFormat(15.0))

        // Test dynamic fallback for another unusual diagonal (e.g. 3.5 mm)
        // 16.0 / 3.5 = 4.5714 -> rounds to 4.57 -> "1/4.57\""
        assertEquals("1/4.57\"", UltimateCameraDiagnostics.convertDiagonalToOpticalFormat(3.5))

        // N/A for invalid sizes
        assertEquals("N/A", UltimateCameraDiagnostics.convertDiagonalToOpticalFormat(0.0))
        assertEquals("N/A", UltimateCameraDiagnostics.convertDiagonalToOpticalFormat(-5.0))
    }
}
