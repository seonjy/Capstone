package com.example.aicameraassistant.data.model

import java.io.File

data class HistoryItem(
    val category: String,
    val title: String,
    val date: String,
    val time: String,
    val originalPhotoFile: File?,
    val adjustedImageUrl: String?,
    val guideText: String,
    val settings: RecommendedSettings,
    val adjustmentInfo: AdjustmentInfo?
)

data class AdjustmentInfo(
    val exposureBefore: Int,
    val exposureAfter: Int,
    val isoBefore: Int,
    val isoAfter: Int,
    val wbBefore: String,
    val wbAfter: String
)

data class RecommendedSettings(
    val iso: Int = 0,
    val shutter: String = "",
    val aperture: String = "",
    val focalLength: String = "",
    val ev: String = "",
    val whiteBalance: String = ""
)
