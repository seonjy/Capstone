package com.example.aicameraassistant

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.aicameraassistant.data.local.loadHistoryItems
import com.example.aicameraassistant.data.local.saveHistoryItems
import com.example.aicameraassistant.data.model.HistoryItem
import com.example.aicameraassistant.data.model.RecommendedSettings
import com.example.aicameraassistant.navigation.AppScreen
import com.example.aicameraassistant.navigation.AppNavigation
import com.example.aicameraassistant.ui.gallery.rememberGalleryImageLauncher
import com.example.aicameraassistant.ui.screens.permission.hasCameraPermission
import java.io.File
class MainActivity : ComponentActivity() {

    // 카메라 권한 허용 여부 상태 저장
    private var permissionGrantedState by mutableStateOf(false)

    // 카메라 권한 요청 launcher
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            permissionGrantedState = isGranted
        }

    private var currentScreen by mutableStateOf(AppScreen.MENU)

    private var capturedPhotoFile by mutableStateOf<File?>(null)
    private var isUploading by mutableStateOf(false)
    private var guideText by mutableStateOf("")
    private var uploadError by mutableStateOf("")
    private var adjustedImageUrl by mutableStateOf<String?>(null)

    private var historyItems by mutableStateOf(listOf<HistoryItem>())

    private var detectedScene by mutableStateOf<String?>(null)

    private var recommendedSettings by mutableStateOf(RecommendedSettings())



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 앱 시작 시 현재 카메라 권한 확인
        permissionGrantedState = hasCameraPermission(this)

        historyItems = loadHistoryItems(this)

        // 권한이 없으면 요청
        if (!permissionGrantedState) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            val galleryLauncher = rememberGalleryImageLauncher(
                onFileReady = { selectedFile ->
                        capturedPhotoFile = selectedFile

                        currentScreen = AppScreen.RESULT
                        isUploading = true
                        guideText = ""
                        uploadError = ""
                        adjustedImageUrl = null
                },
                onSuccess = { responseText, imageUrl, scene, settings, newItem ->
                    isUploading = false
                    capturedPhotoFile = newItem.originalPhotoFile
                    detectedScene = scene
                    recommendedSettings = settings
                    guideText = "Scene: $scene\n$responseText"
                    adjustedImageUrl = imageUrl

                    historyItems = listOf(newItem) + historyItems
                    saveHistoryItems(this, historyItems)
                },
                onError = { errorMessage ->
                    isUploading = false
                    uploadError = errorMessage
                },
                onInvalidImage = {
                    currentScreen = AppScreen.RESULT
                    isUploading = false
                    uploadError = "이미지를 불러오지 못했습니다."
                }
            )

//            KlickWebViewScreen()

            AppNavigation(
                permissionGranted = permissionGrantedState,
                currentScreen = currentScreen,
                historyItems = historyItems,
                isUploading = isUploading,
                guideText = guideText,
                uploadError = uploadError,
                capturedPhotoFile = capturedPhotoFile,
                adjustedImageUrl = adjustedImageUrl,
                detectedScene = detectedScene,
                recommendedSettings = recommendedSettings,
                onRequestPermission = {
                    requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                },
                onCameraClick = {
                    currentScreen = AppScreen.CAMERA
                },
                onGalleryClick = {
                    galleryLauncher.launch("image/*")
                },
                onBackFromCamera = {
                    currentScreen = AppScreen.MENU
                },
                onSaveHistory = { newItem ->
                    historyItems = listOf(newItem) + historyItems
                    saveHistoryItems(this, historyItems)
                },
                onBackFromResult = {
                    currentScreen = AppScreen.MENU
                    isUploading = false
                    guideText = ""
                    uploadError = ""
                    adjustedImageUrl = null
                    detectedScene = null
                }
            )
        }
    }
}
