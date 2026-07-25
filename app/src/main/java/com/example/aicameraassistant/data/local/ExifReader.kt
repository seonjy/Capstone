package com.example.aicameraassistant.data.local

import android.media.ExifInterface
import com.example.aicameraassistant.data.model.CapturedSettings
import java.io.File
import java.util.Locale

internal fun readCapturedSettings(file: File?): CapturedSettings {
    if (file == null || !file.exists() || !file.isFile) return CapturedSettings()

    return runCatching {
        val exif = ExifInterface(file.absolutePath)

        CapturedSettings(
            iso = attribute(exif, ExifInterface.TAG_ISO_SPEED_RATINGS),
            shutter = rationalAsSeconds(attribute(exif, ExifInterface.TAG_EXPOSURE_TIME)),
            aperture = rationalAsDecimal(attribute(exif, ExifInterface.TAG_F_NUMBER))
                ?.let { "f/$it" },
            focalLength = rationalAsDecimal(attribute(exif, ExifInterface.TAG_FOCAL_LENGTH))
                ?.let { "$it mm" },
            ev = rationalAsSignedDecimal(
                attribute(exif, ExifInterface.TAG_EXPOSURE_BIAS_VALUE)
            ),
            whiteBalance = when (attribute(exif, ExifInterface.TAG_WHITE_BALANCE)) {
                "0" -> "자동"
                "1" -> "수동"
                else -> null
            }
        )
    }.getOrDefault(CapturedSettings())
}

private fun attribute(exif: ExifInterface, tag: String): String? =
    exif.getAttribute(tag)?.trim()?.takeIf { it.isNotBlank() }

private fun rationalAsSeconds(raw: String?): String? {
    val seconds = rationalToDouble(raw) ?: return null
    if (seconds <= 0.0) return null

    return if (seconds < 1.0) {
        val denominator = (1.0 / seconds).toInt().coerceAtLeast(1)
        "1/$denominator 초"
    } else {
        "${formatDecimal(seconds)}초"
    }
}

private fun rationalAsDecimal(raw: String?): String? =
    rationalToDouble(raw)?.let(::formatDecimal)

private fun rationalAsSignedDecimal(raw: String?): String? =
    rationalToDouble(raw)?.let { value ->
        val formatted = formatDecimal(value)
        if (value > 0) "+$formatted" else formatted
    }

private fun rationalToDouble(raw: String?): Double? {
    if (raw.isNullOrBlank()) return null
    val parts = raw.split('/')
    return if (parts.size == 2) {
        val numerator = parts[0].toDoubleOrNull() ?: return null
        val denominator = parts[1].toDoubleOrNull()?.takeIf { it != 0.0 } ?: return null
        numerator / denominator
    } else {
        raw.toDoubleOrNull()
    }
}

private fun formatDecimal(value: Double): String =
    String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
