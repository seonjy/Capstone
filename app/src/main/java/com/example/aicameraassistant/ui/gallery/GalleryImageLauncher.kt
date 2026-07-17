package com.example.aicameraassistant.ui.gallery

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.aicameraassistant.data.processGalleryImage
import com.example.aicameraassistant.data.model.HistoryItem
import com.example.aicameraassistant.data.model.RecommendedSettings
import java.io.File

@Composable
internal fun rememberGalleryImageLauncher(
    onFileReady: (File) -> Unit,
    onSuccess: (String, String?, String?, RecommendedSettings, HistoryItem) -> Unit,
    onError: (String) -> Unit,
    onInvalidImage: () -> Unit
): ActivityResultLauncher<String> {
    val context = LocalContext.current

    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            processGalleryImage(
                context = context,
                uri = it,
                onFileReady = onFileReady,
                onSuccess = onSuccess,
                onError = onError,
                onInvalidImage = onInvalidImage
            )
        }
    }
}
