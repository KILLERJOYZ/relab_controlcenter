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
            pillarObj.put("score", pillarScore.score)
            pillarObj.put("isSkipped", pillarScore.isSkipped)
            
            val subArray = JSONArray()
            for (sub in pillarScore.subScores) {
                val subObj = JSONObject()
                subObj.put("name", sub.name)
                subObj.put("rawValue", sub.rawValue)
                subObj.put("unit", sub.unit)
                subObj.put("score", sub.score)
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
                val score = pillarObj.getInt("score")
                val isSkipped = pillarObj.optBoolean("isSkipped", false)
                
                val subScores = mutableListOf<SubScore>()
                val subArray = pillarObj.getJSONArray("subScores")
                for (j in 0 until subArray.length()) {
                    val subObj = subArray.getJSONObject(j)
                    val name = subObj.getString("name")
                    val rawValue = subObj.getDouble("rawValue")
                    val unit = subObj.getString("unit")
                    val subScoreVal = subObj.getInt("score")
                    val isPartial = subObj.optBoolean("isPartial", false)
                    subScores.add(SubScore(name, rawValue, unit, subScoreVal, isPartial))
                }
                list.add(PillarScore(pillar, score, subScores, isSkipped))
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
