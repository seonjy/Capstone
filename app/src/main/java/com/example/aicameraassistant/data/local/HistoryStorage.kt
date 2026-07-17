package com.example.aicameraassistant.data.local

import android.content.Context
import com.example.aicameraassistant.data.model.AdjustmentInfo
import com.example.aicameraassistant.data.model.HistoryItem
import com.example.aicameraassistant.data.model.RecommendedSettings
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

internal fun saveHistoryItems(
    context: Context,
    items: List<HistoryItem>
) {
    val jsonArray = JSONArray()

    items.forEach { item ->
        val obj = JSONObject().apply {
            put("guideText", item.guideText)

            put("settingsIso", item.settings.iso)
            put("settingsShutter", item.settings.shutter)
            put("settingsAperture", item.settings.aperture)
            put("settingsFocalLength", item.settings.focalLength)
            put("settingsEv", item.settings.ev)
            put("settingsWhiteBalance", item.settings.whiteBalance)

            put("category", item.category)
            put("title", item.title)
            put("date", item.date)
            put("time", item.time)
            put("originalPhotoPath", item.originalPhotoFile?.absolutePath ?: "")
            put("adjustedImageUrl", item.adjustedImageUrl ?: "")

            item.adjustmentInfo?.let {
                put("exposureBefore", it.exposureBefore)
                put("exposureAfter", it.exposureAfter)
                put("isoBefore", it.isoBefore)
                put("isoAfter", it.isoAfter)
                put("wbBefore", it.wbBefore)
                put("wbAfter", it.wbAfter)
            }
        }

        jsonArray.put(obj)
    }

    context.getSharedPreferences("history_prefs", Context.MODE_PRIVATE)
        .edit()
        .putString("history_items", jsonArray.toString())
        .apply()
}

internal fun loadHistoryItems(context: Context): List<HistoryItem> {
    val jsonText = context
        .getSharedPreferences("history_prefs", Context.MODE_PRIVATE)
        .getString("history_items", null)
        ?: return emptyList()

    return try {
        val jsonArray = JSONArray(jsonText)
        val result = mutableListOf<HistoryItem>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)

            val originalPath = obj.optString("originalPhotoPath", "")
            val originalFile = if (originalPath.isNotBlank()) File(originalPath) else null

            result.add(
                HistoryItem(
                    category = obj.optString("category", "기타"),
                    title = obj.optString("title", ""),
                    date = obj.optString("date", ""),
                    time = obj.optString("time", ""),
                    originalPhotoFile = originalFile,
                    adjustedImageUrl = obj.optString("adjustedImageUrl", ""),
                    guideText = obj.optString("guideText", ""),
                    settings = RecommendedSettings(
                        iso = obj.optInt("settingsIso", 0),
                        shutter = obj.optString("settingsShutter", ""),
                        aperture = obj.optString("settingsAperture", ""),
                        focalLength = obj.optString("settingsFocalLength", ""),
                        ev = obj.optString("settingsEv", ""),
                        whiteBalance = obj.optString("settingsWhiteBalance", "")
                    ),
                    adjustmentInfo = AdjustmentInfo(
                        exposureBefore = obj.optInt("exposureBefore", 0),
                        exposureAfter = obj.optInt("exposureAfter", 0),
                        isoBefore = obj.optInt("isoBefore", 100),
                        isoAfter = obj.optInt("isoAfter", 0),
                        wbBefore = obj.optString("wbBefore", "AUTO"),
                        wbAfter = obj.optString("wbAfter", "")
                    )
                )
            )
        }

        result
    } catch (e: Exception) {
        emptyList()
    }
}
