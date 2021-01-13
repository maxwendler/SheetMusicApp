package com.example.sheetmusicapp.parser

import com.example.sheetmusicapp.data.*
import com.example.sheetmusicapp.scoreModel.*
import com.google.gson.*
import java.lang.reflect.Type

class ScoreSerializer: JsonSerializer<Score> {
    override fun serialize(
        src: Score,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        val barList = src.barList.toList()
        val barDataList = mutableListOf<BarData>()
        barList.forEach{ bar ->
            val timeSignatureData = TimeSignatureData(bar.timeSignature.numerator, bar.timeSignature.denominator)
            val voiceIntervalsList = mutableMapOf<Int, MutableList<RhythmicIntervalData>>()
            bar.voices.forEach{ elem ->
                val intervalData = mutableListOf<RhythmicIntervalData>()
                elem.value.intervals.forEach{
                    val length = RhythmicLengthData(it.getLengthCopy().basicLength, it.getLengthCopy().lengthModifier)
                    intervalData.add(RhythmicIntervalData(length, it.getNoteHeadsCopy(), it.startUnit))
                }
                voiceIntervalsList[elem.key] = intervalData
            }
            val data = BarData(bar.barNr,
                    timeSignatureData,
                    voiceIntervalsList
            )
            barDataList.add(data)
        }
        val data = ScoreData(barDataList, src.title)
//        return JsonPrimitive(GsonBuilder().setPrettyPrinting().create().toJson(data))
        return Gson().toJsonTree(data)
    }
}

class ScoreDeserializer: JsonDeserializer<Score> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Score {
        val data = Gson().fromJson(json, ScoreData::class.java)
        val barList = mutableListOf<Bar>()
        data.bars.forEach{ barData ->
            val timeSignature = TimeSignature(barData.timeSignature.numerator, barData.timeSignature.denominator)
            val voiceList = mutableMapOf<Int, MutableList<RhythmicInterval>>()
            barData.voiceIntervals.forEach{ voice ->
                val intervalList = mutableListOf<RhythmicInterval>()
                voice.value.forEach{
                    val length = RhythmicLength(it.length.basic, it.length.modifier)
                    val interval = RhythmicInterval(length, it.noteHeads.toMutableMap(), it.startUnit)
                    intervalList.add(interval)
                }
                voiceList[voice.key] = intervalList
            }
            barList.add(Bar(barData.barNr, timeSignature, voiceList))
        }
        return Score(barList, data.title)
    }
}