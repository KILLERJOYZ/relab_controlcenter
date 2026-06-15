package com.example.relab_tool.model

enum class TurnDirection { LEFT, RIGHT }

data class TurnInstruction(
    val direction: TurnDirection = TurnDirection.RIGHT,
    val degrees: Int = 0,
    val elevationDegrees: Int = 0
)

data class SatelliteCompassUiState(
    val targetSatellite: GnssSatellite? = null,
    val deviceHeading: Float = 0f,
    val relativeBearing: Float = 0f,
    val turnInstruction: TurnInstruction? = null,
    val nextUpdateSec: Int = 600,
    val allSatellites: List<GnssSatellite> = emptyList()
)
