package com.example.relab_tool.benchmark.data

import androidx.room.TypeConverter
import com.example.relab_tool.benchmark.domain.model.BenchmarkPillar
import com.example.relab_tool.benchmark.domain.model.PillarScore
import com.example.relab_tool.benchmark.domain.model.ScoreTier
import com.example.relab_tool.benchmark.domain.model.SubScore
import org.json.JSONArray
import org.json.JSONObject

class BenchmarkConverters {
    @TypeConverter
    fun fromPillarScores(scores: List<PillarScore>): String {
        val array = JSONArray()
        for (pillarScore in scores) {
            val pillarObj = JSONObject()
            pillarObj.put("pillar", pillarScore.pillar.name)
            val safePillarScore = if (pillarScore.score.isNaN() || pillarScore.score.isInfinite()) 0.0 else pillarScore.score
            pillarObj.put("score", safePillarScore)
            pillarObj.put("isSkipped", pillarScore.isSkipped)
            
            val subArray = JSONArray()
            for (sub in pillarScore.subScores) {
                val subObj = JSONObject()
                subObj.put("name", sub.name)
                val safeRawValue = if (sub.rawValue.isNaN() || sub.rawValue.isInfinite()) 0.0 else sub.rawValue
                subObj.put("rawValue", safeRawValue)
                subObj.put("unit", sub.unit)
                val safeSubScore = if (sub.score.isNaN() || sub.score.isInfinite()) 0.0 else sub.score
                subObj.put("score", safeSubScore)
                subObj.put("isPartial", sub.isPartial)
                subArray.put(subObj)
            }
            pillarObj.put("subScores", subArray)
            array.put(pillarObj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toPillarScores(json: String): List<PillarScore> {
        val list = mutableListOf<PillarScore>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val pillarObj = array.getJSONObject(i)
                val pillarName = pillarObj.getString("pillar")
                val pillar = BenchmarkPillar.valueOf(pillarName)
                val score = pillarObj.optDouble("score", 0.0)
                val safeScore = if (score.isNaN() || score.isInfinite()) 0.0 else score
                val isSkipped = pillarObj.optBoolean("isSkipped", false)
                
                val subScores = mutableListOf<SubScore>()
                val subArray = pillarObj.getJSONArray("subScores")
                for (j in 0 until subArray.length()) {
                    val subObj = subArray.getJSONObject(j)
                    val name = subObj.getString("name")
                    val rawValue = subObj.optDouble("rawValue", 0.0)
                    val safeRawValue = if (rawValue.isNaN() || rawValue.isInfinite()) 0.0 else rawValue
                    val unit = subObj.getString("unit")
                    val subScoreVal = subObj.optDouble("score", 0.0)
                    val safeSubScoreVal = if (subScoreVal.isNaN() || subScoreVal.isInfinite()) 0.0 else subScoreVal
                    val isPartial = subObj.optBoolean("isPartial", false)
                    subScores.add(SubScore(name, safeRawValue, unit, safeSubScoreVal, isPartial))
                }
                list.add(PillarScore(pillar, safeScore, subScores, isSkipped))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    @TypeConverter
    fun fromScoreTier(tier: ScoreTier): String {
        return tier.name
    }

    @TypeConverter
    fun toScoreTier(name: String): ScoreTier {
        return try {
            ScoreTier.valueOf(name)
        } catch (e: Exception) {
            ScoreTier.ENTRY
        }
    }
}
