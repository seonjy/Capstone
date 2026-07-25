package com.example.aicameraassistant.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.example.aicameraassistant.data.model.HistoryItem
import com.example.aicameraassistant.data.model.RecommendedSettings
import com.example.aicameraassistant.ui.screens.camera.CameraPreviewScreen
import com.example.aicameraassistant.ui.screens.main.MainTabScreen
import com.example.aicameraassistant.ui.screens.permission.CameraPermissionScreen
import com.example.aicameraassistant.ui.screens.result.ResultScreen
import java.io.File

@Composable
internal fun AppNavigation(
    permissionGranted: Boolean,
    currentScreen: AppScreen,
    historyItems: List<HistoryItem>,
    isUploading: Boolean,
    guideText: String,
    uploadError: String,
    capturedPhotoFile: File?,
    adjustedPhotoFile: File?,
    adjustedImageUrl: String?,
    detectedScene: String?,
    recommendedSettings: RecommendedSettings,
    onRequestPermission: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onBackFromCamera: () -> Unit,
    onSaveHistory: (HistoryItem) -> Unit,
    onDeleteHistoryItem: (HistoryItem) -> Unit,
    onBackFromResult: () -> Unit
) {
    if (permissionGranted) {

        BackHandler(enabled = currentScreen != AppScreen.MENU) {
            when (currentScreen) {
                AppScreen.CAMERA -> onBackFromCamera()
                AppScreen.RESULT -> onBackFromResult()
                AppScreen.MENU -> Unit
            }
        }

        when (currentScreen) {

            AppScreen.MENU -> MainTabScreen(
                onCameraClick = onCameraClick,
                onGalleryClick = onGalleryClick,
                historyItems = historyItems,
                onDeleteHistoryItem = onDeleteHistoryItem
            )

            AppScreen.CAMERA -> CameraPreviewScreen(
                onBack = onBackFromCamera,
                onSaveHistory = onSaveHistory
            )

            AppScreen.RESULT -> {
                ResultScreen(
                    isUploading = isUploading,
                    guideText = guideText,
                    uploadError = uploadError,
                    originalPhotoFile = capturedPhotoFile,
                    adjustedPhotoFile = adjustedPhotoFile,
                    adjustedImageUrl = adjustedImageUrl,
                    scene = detectedScene,
                    settings = recommendedSettings,
                    onBackToCamera = onBackFromResult
                )
            }
        }

    } else {
        CameraPermissionScreen(
            onRequestPermission = onRequestPermission
        )
    }
}
