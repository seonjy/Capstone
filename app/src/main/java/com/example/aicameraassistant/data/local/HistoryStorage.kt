package com.example.aicameraassistant.data.local

import android.content.Context
import com.example.aicameraassistant.data.model.AdjustmentInfo
import com.example.aicameraassistant.data.model.HistoryItem
import com.example.aicameraassistant.data.model.RecommendedSettings
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

internal fun saveHistoryItems(
    context: Context,
    items: List<HistoryItem>
) {
    val jsonArray = JSONArray()

    items.forEach { item ->
        val obj = JSONObject().apply {
            put("id", item.id)
            put("createdAt", item.createdAt)
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
            put("adjustedPhotoPath", item.adjustedPhotoFile?.absolutePath ?: "")
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
            val obj = jsonArray.optJSONObject(i) ?: continue
            runCatching { parseHistoryItem(obj, i) }
                .onSuccess(result::add)
                .onFailure { error ->
                    android.util.Log.w(
                        "HistoryStorage",
                        "손상된 히스토리 항목을 건너뜁니다. index=$i",
                        error
                    )
                }
        }

        result
    } catch (e: Exception) {
        emptyList()
    }
}

private fun parseHistoryItem(obj: JSONObject, index: Int): HistoryItem {
    val storedDate = obj.optString("date", "").trim()
    val storedTime = obj.optString("time", "").trim()
    val title = obj.optString("title", "").trim().ifBlank { "제목 없음" }
    val createdAt = obj.optLong("createdAt", 0L).takeIf { it > 0L }
        ?: parseLegacyCreatedAt(storedDate, storedTime)
    val id = obj.optString("id", "").trim().takeIf { it.isNotBlank() }
        ?: "legacy-$index-${storedDate}-${storedTime}-${title.hashCode()}"

    return HistoryItem(
        id = id,
        createdAt = createdAt,
        category = obj.optString("category", "기타").trim().ifBlank { "기타" },
        title = title,
        date = storedDate.ifBlank { "날짜 미상" },
        time = storedTime.ifBlank { "--:--" },
        originalPhotoFile = validStoredFile(obj.optString("originalPhotoPath", "")),
        adjustedPhotoFile = validStoredFile(obj.optString("adjustedPhotoPath", "")),
        adjustedImageUrl = obj.optString("adjustedImageUrl", "")
            .trim()
            .takeIf { it.isNotBlank() },
        guideText = obj.optString("guideText", ""),
        settings = RecommendedSettings(
            iso = obj.optInt("settingsIso", 0).coerceAtLeast(0),
            shutter = obj.optString("settingsShutter", ""),
            aperture = obj.optString("settingsAperture", ""),
            focalLength = obj.optString("settingsFocalLength", ""),
            ev = obj.optString("settingsEv", ""),
            whiteBalance = obj.optString("settingsWhiteBalance", "")
        ),
        adjustmentInfo = AdjustmentInfo(
            exposureBefore = obj.optInt("exposureBefore", 0),
            exposureAfter = obj.optInt("exposureAfter", 0),
            isoBefore = obj.optInt("isoBefore", 100).coerceAtLeast(0),
            isoAfter = obj.optInt("isoAfter", 0).coerceAtLeast(0),
            wbBefore = obj.optString("wbBefore", "AUTO"),
            wbAfter = obj.optString("wbAfter", "")
        )
    )
}

private fun validStoredFile(path: String): File? {
    return path.trim()
        .takeIf { it.isNotBlank() }
        ?.let(::File)
        ?.takeIf { it.exists() && it.isFile }
}

internal fun deleteHistoryItemFiles(context: Context, item: HistoryItem) {
    val allowedRoots = listOf(context.filesDir, context.cacheDir)
        .mapNotNull { runCatching { it.canonicalFile }.getOrNull() }
    val files = listOfNotNull(item.originalPhotoFile, item.adjustedPhotoFile)
        .distinctBy { it.absolutePath }

    files.forEach { file ->
        val canonicalFile = runCatching { file.canonicalFile }.getOrNull() ?: return@forEach
        val isAppInternalFile = allowedRoots.any { root ->
            canonicalFile.path.startsWith(root.path + File.separator)
        }

        if (isAppInternalFile && canonicalFile.isFile) {
            canonicalFile.delete()
        }
    }
}

private fun parseLegacyCreatedAt(date: String, time: String): Long {
    return runCatching {
        SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA)
            .parse("$date $time")
            ?.time
            ?: 0L
    }.getOrDefault(0L)
}
