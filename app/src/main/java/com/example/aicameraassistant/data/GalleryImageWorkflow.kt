package com.example.aicameraassistant.data

import android.content.Context
import android.net.Uri
import com.example.aicameraassistant.data.local.copyImageToHistoryStorage
import com.example.aicameraassistant.data.local.uriToFile
import com.example.aicameraassistant.data.model.AdjustmentInfo
import com.example.aicameraassistant.data.model.HistoryItem
import com.example.aicameraassistant.data.model.RecommendedSettings
import com.example.aicameraassistant.data.remote.uploadCapturedImage
import com.example.aicameraassistant.ui.util.sceneToKorean
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun processGalleryImage(
    context: Context,
    uri: Uri,
    onFileReady: (File) -> Unit,
    onSuccess: (String, String?, String?, RecommendedSettings, HistoryItem) -> Unit,
    onError: (String) -> Unit,
    onInvalidImage: () -> Unit
) {
    val selectedFile = uriToFile(context, uri)

    if (selectedFile != null) {
        onFileReady(selectedFile)

        uploadCapturedImage(
            photoFile = selectedFile,
            baseUrl = "https://lamprophonic-unclosable-maryellen.ngrok-free.dev",
            onSuccess = { responseText, imageUrl, scene, settings ->
                val category = sceneToKorean(scene)
                val historyPhotoFile =
                    copyImageToHistoryStorage(context, selectedFile)

                val newItem = HistoryItem(
                    category = category,
                    title = selectedFile.name,
                    date = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(Date()),
                    time = SimpleDateFormat("HH:mm", Locale.KOREA).format(Date()),
                    originalPhotoFile = selectedFile,
                    adjustedImageUrl = imageUrl,
                    guideText = responseText,
                    settings = settings,
                    adjustmentInfo = AdjustmentInfo(
                        exposureBefore = 0,
                        exposureAfter = 0,
                        isoBefore = 100,
                        isoAfter = settings.iso,
                        wbBefore = "AUTO",
                        wbAfter = settings.whiteBalance
                    )
                )

                onSuccess(responseText, imageUrl, scene, settings, newItem)
            },
            onError = onError
        )
    } else {
        onInvalidImage()
    }
}
