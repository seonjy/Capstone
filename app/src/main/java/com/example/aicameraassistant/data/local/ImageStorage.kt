package com.example.aicameraassistant.data.local

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream

// 원본 이미지를 갤러리에 저장하는 함수
internal fun saveImageToGallery(context: Context, imageFile: File): Boolean {
    return try {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, imageFile.name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/CameraAssist")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return false

        resolver.openOutputStream(imageUri).use { outputStream ->
            FileInputStream(imageFile).use { inputStream ->
                inputStream.copyTo(outputStream!!)
            }
        }

        // 저장 완료 처리
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(imageUri, values, null, null)

        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

// 보정 이미지 다운로드 후 갤러리에 저장하는 함수
internal fun saveImageFromUrlToGallery(
    context: Context,
    imageUrl: String,
    onResult: (Boolean) -> Unit
) {
    Thread {
        try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(imageUrl)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    android.util.Log.e("GALLERY", "보정 이미지 다운로드 실패: HTTP ${response.code}")
                    onResult(false)
                    return@use
                }

                val imageBytes = response.body?.bytes()
                if (imageBytes == null || imageBytes.isEmpty()) {
                    android.util.Log.e("GALLERY", "보정 이미지 바이트가 비어 있음")
                    onResult(false)
                    return@use
                }

                val fileName = "adjusted_${System.currentTimeMillis()}.jpg"

                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/CameraAssist"
                    )
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val imageUri = resolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
                )

                if (imageUri == null) {
                    android.util.Log.e("GALLERY", "MediaStore insert 실패")
                    onResult(false)
                    return@use
                }

                resolver.openOutputStream(imageUri).use { outputStream ->
                    if (outputStream == null) {
                        android.util.Log.e("GALLERY", "OutputStream 생성 실패")
                        onResult(false)
                        return@use
                    }
                    outputStream.write(imageBytes)
                    outputStream.flush()
                }

                // 저장 완료 처리
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(imageUri, values, null, null)

                android.util.Log.d("GALLERY", "보정 이미지 갤러리 저장 성공")
                onResult(true)
            }
        } catch (e: Exception) {
            android.util.Log.e("GALLERY", "보정 이미지 저장 실패", e)
            onResult(false)
        }
    }.start()
}

// 최신 갤러리 이미지 가져오는 함수
internal fun getLatestGalleryImageUri(context: Context): Uri? {
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DATE_ADDED
    )

    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

    return try {
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

            if (cursor.moveToFirst()) {
                val id = cursor.getLong(idColumn)

                Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )
            } else {
                null
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// 갤러리에서 선택한 Uri를 임시 File로 변환
internal fun uriToFile(context: Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = File.createTempFile("gallery_image_", ".jpg", context.cacheDir)

        inputStream.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        tempFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

internal fun copyImageToHistoryStorage(context: Context, sourceFile: File): File {
    val historyDir = File(context.filesDir, "history_images")
    if (!historyDir.exists()) {
        historyDir.mkdirs()
    }

    val savedFile = File(
        historyDir,
        "history_${System.currentTimeMillis()}.jpg"
    )

    sourceFile.copyTo(savedFile, overwrite = true)

    return savedFile
}

internal fun downloadImageToHistoryStorage(
    context: Context,
    imageUrl: String?,
    onResult: (File?) -> Unit
) {
    if (imageUrl.isNullOrBlank()) {
        onResult(null)
        return
    }

    Thread {
        val savedFile = try {
            val request = Request.Builder()
                .url(imageUrl)
                .build()
            val client = OkHttpClient()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    null
                } else {
                    val imageBytes = response.body?.bytes()
                    if (imageBytes == null || imageBytes.isEmpty()) {
                        null
                    } else {
                        val historyDir = File(context.filesDir, "history_images")
                        if (!historyDir.exists()) {
                            historyDir.mkdirs()
                        }

                        File(
                            historyDir,
                            "adjusted_${System.currentTimeMillis()}.jpg"
                        ).apply {
                            writeBytes(imageBytes)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            null
        }

        onResult(savedFile)
    }.start()
}

internal fun saveResultImageToGallery(
    context: Context,
    adjustedPhotoFile: File?,
    adjustedImageUrl: String?,
    originalPhotoFile: File?,
    onResult: (Boolean) -> Unit
) {
    val mainHandler = Handler(Looper.getMainLooper())
    val validAdjustedPhoto = adjustedPhotoFile?.takeIf { it.exists() && it.isFile }
    val validOriginalPhoto = originalPhotoFile?.takeIf { it.exists() && it.isFile }

    if (validAdjustedPhoto == null && !adjustedImageUrl.isNullOrBlank()) {
        saveImageFromUrlToGallery(context, adjustedImageUrl) { saved ->
            if (saved || validOriginalPhoto == null) {
                mainHandler.post { onResult(saved) }
            } else {
                Thread {
                    val originalSaved = saveImageToGallery(context, validOriginalPhoto)
                    mainHandler.post { onResult(originalSaved) }
                }.start()
            }
        }
        return
    }

    val localImage = validAdjustedPhoto ?: validOriginalPhoto
    if (localImage == null) {
        onResult(false)
        return
    }

    Thread {
        val saved = saveImageToGallery(context, localImage)
        mainHandler.post { onResult(saved) }
    }.start()
}
