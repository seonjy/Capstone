package com.example.aicameraassistant.data.remote

import android.os.Handler
import android.os.Looper
import com.example.aicameraassistant.data.model.RecommendedSettings
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File

internal fun uploadPreviewFrame(
    imageBytes: ByteArray,
    baseUrl: String,
    onSuccess: (String, Double) -> Unit,
    onError: (String) -> Unit
) {
    val url = "$baseUrl/upload"

    val client = OkHttpClient()

    val requestBody = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("user_id", "test_user_001")
        .addFormDataPart("mode", "preview")
        .addFormDataPart(
            "file",
            "preview_frame.jpg",
            imageBytes.toRequestBody("image/jpeg".toMediaType())
        )
        .build()

    val request = Request.Builder()
        .url(url)
        .post(requestBody)
        .build()

    Thread {
        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    Handler(Looper.getMainLooper()).post {
                        onError("HTTP ${response.code}")
                    }
                    return@use
                }

                val json = JSONObject(body)
                val trackB = json.getJSONObject("track_b")

                val scene = trackB.optString("scene", "")
                val confidence = trackB.optDouble("confidence", 0.0)

                Handler(Looper.getMainLooper()).post {
                    onSuccess(scene, confidence)
                }
            }
        } catch (e: Exception) {
            Handler(Looper.getMainLooper()).post {
                onError(e.message ?: "프리뷰 분석 실패")
            }
        }
    }.start()
}

// 촬영한 이미지를 서버로 업로드하는 함수
internal fun uploadCapturedImage(
    photoFile: File,
    baseUrl: String,
    onSuccess: (String, String?, String?, RecommendedSettings) -> Unit,
    onError: (String) -> Unit
) {
    val url = "$baseUrl/upload"

    val client = OkHttpClient()

    val requestBody = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("user_id", "test_user_001")
        .addFormDataPart(
            "file",
            photoFile.name,
            photoFile.asRequestBody("image/jpeg".toMediaType())
        )
        .build()

    val request = Request.Builder()
        .url(url)
        .post(requestBody)
        .build()

    Thread {
        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()

                android.util.Log.d("UPLOAD", "response code = ${response.code}")
                android.util.Log.d("UPLOAD", "response body = $body")

                if (!response.isSuccessful) {
                    onError("업로드 실패: HTTP ${response.code}\n$body")
                    return@use
                }

                try {
                    val json = JSONObject(body)

                    var imageUrl: String? = null
                    var sceneText: String? = null
                    var recommendedSettings = RecommendedSettings()

                    if (json.has("adjusted_image_info")) {
                        val imageInfo = json.getJSONObject("adjusted_image_info")
                        val rawUrl = imageInfo.optString("url", "").trim()
                        val savedAs = imageInfo.optString("saved_as", "").trim()

                        android.util.Log.d("UPLOAD", "rawUrl = $rawUrl")
                        android.util.Log.d("UPLOAD", "savedAs = $savedAs")

                        imageUrl = when {
                            rawUrl.startsWith("http://") || rawUrl.startsWith("https://") -> {
                                rawUrl
                            }

                            rawUrl.isNotEmpty() -> {
                                val cleanedPath = rawUrl
                                    .replace("\\", "/")
                                    .replace("/images//images/", "/images/")
                                    .replace("//images/", "/images/")
                                    .replace("/images/images/", "/images/")
                                    .let {
                                        if (!it.startsWith("/")) "/$it" else it
                                    }

                                "$baseUrl$cleanedPath"
                            }

                            savedAs.isNotEmpty() -> {
                                val cleanedFileName = savedAs
                                    .replace("\\", "/")
                                    .substringAfterLast("/")

                                "$baseUrl/images/$cleanedFileName"
                            }

                            else -> null
                        }

                        android.util.Log.d("UPLOAD", "resolved imageUrl = $imageUrl")
                    }

                    if (json.has("track_b")) {
                        val trackB = json.getJSONObject("track_b")
                        sceneText = trackB.optString("scene", "").trim()

                        android.util.Log.d("UPLOAD", "sceneText = $sceneText")
                    }

                    if (json.has("recommendation")) {
                        val recommendation = json.getJSONObject("recommendation")

                        recommendedSettings = RecommendedSettings(
                            iso = recommendation.optInt("recommended_iso", 0),
                            shutter = recommendation.optString("recommended_shutter", "").trim(),
                            aperture = recommendation.optString("recommended_aperture", "").trim(),
                            focalLength = recommendation.optString("recommended_focal_length", "").trim(),
                            ev = recommendation.optString("recommended_ev", "").trim(),
                            whiteBalance = recommendation.optString("recommended_white_balance", "").trim()
                        )

                        android.util.Log.d("UPLOAD", "recommended settings = $recommendedSettings")
                    }

                    val messageText: String = if (json.has("guide")) {
                        val guideObj = json.getJSONObject("guide")
                        val msg = guideObj.optString("message", "")
                        val tip = guideObj.optString("tip", "")

                        when {
                            msg.isNotEmpty() && tip.isNotEmpty() -> "$msg\n$tip"
                            msg.isNotEmpty() -> msg
                            tip.isNotEmpty() -> tip
                            json.optBoolean("ok", false) -> "분석이 완료되었습니다."
                            else -> body
                        }
                    } else {
                        when {
                            json.has("message") -> json.optString("message")
                            json.optBoolean("ok", false) -> "분석이 완료되었습니다."
                            else -> body
                        }
                    }

                    onSuccess(messageText, imageUrl, sceneText, recommendedSettings)

                } catch (e: Exception) {
                    android.util.Log.e("UPLOAD", "json parse error", e)
                    onError("응답 파싱 실패: ${e.message}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("UPLOAD", "upload failed", e)
            onError("업로드 실패: ${e.message}")
        }
    }.start()
}
