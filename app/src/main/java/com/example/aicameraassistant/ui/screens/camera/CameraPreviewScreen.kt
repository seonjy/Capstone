@file:OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
package com.example.aicameraassistant.ui.screens.camera

import android.Manifest
import android.R.attr.text
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.Camera
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import android.content.Context.SENSOR_SERVICE
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import android.hardware.camera2.CaptureRequest
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Handler
import android.os.Looper
import java.io.ByteArrayOutputStream
import okhttp3.RequestBody.Companion.toRequestBody
import coil.compose.AsyncImage
import com.example.aicameraassistant.data.local.copyImageToHistoryStorage
import com.example.aicameraassistant.data.local.getLatestGalleryImageUri
import com.example.aicameraassistant.data.local.loadHistoryItems
import com.example.aicameraassistant.data.local.saveHistoryItems
import com.example.aicameraassistant.data.local.saveImageFromUrlToGallery
import com.example.aicameraassistant.data.local.saveImageToGallery
import com.example.aicameraassistant.data.local.uriToFile
import com.example.aicameraassistant.data.remote.uploadCapturedImage
import com.example.aicameraassistant.data.remote.uploadPreviewFrame
import com.example.aicameraassistant.data.model.AdjustmentInfo
import com.example.aicameraassistant.data.model.HistoryItem
import com.example.aicameraassistant.data.model.RecommendedSettings
import com.example.aicameraassistant.data.model.WeatherTip
import com.example.aicameraassistant.data.model.WeatherType
import com.example.aicameraassistant.navigation.AppScreen
import com.example.aicameraassistant.ui.screens.camera.AiGuideCard
import com.example.aicameraassistant.ui.screens.camera.CameraCircleButton
import com.example.aicameraassistant.ui.screens.camera.calculateBrightness
import com.example.aicameraassistant.ui.screens.camera.CameraLogoPill
import com.example.aicameraassistant.ui.screens.camera.CameraSettingBar
import com.example.aicameraassistant.ui.screens.camera.CameraSettingItem
import com.example.aicameraassistant.ui.screens.camera.EvSettingItem
import com.example.aicameraassistant.ui.screens.camera.ExposureInlineBar
import com.example.aicameraassistant.ui.screens.camera.GuideOverlay
import com.example.aicameraassistant.ui.screens.camera.SceneDetectPill
import com.example.aicameraassistant.ui.screens.camera.WhiteBalanceInlineBar
import com.example.aicameraassistant.ui.screens.camera.WhiteBalanceMode
import com.example.aicameraassistant.ui.screens.home.HomeScreen
import com.example.aicameraassistant.ui.screens.history.HistoryScreen
import com.example.aicameraassistant.ui.screens.main.MainTabScreen
import com.example.aicameraassistant.ui.screens.result.ResultScreen
import com.example.aicameraassistant.ui.screens.permission.CameraPermissionScreen
import com.example.aicameraassistant.ui.screens.web.KlickWebViewScreen
import com.example.aicameraassistant.ui.util.getAiGuideByScene
import com.example.aicameraassistant.ui.util.sceneToKorean
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONArray
import com.example.aicameraassistant.R

// 카메라 프리뷰 UI
@Composable
fun CameraPreviewScreen(
    onBack: () -> Unit,
    onSaveHistory: (HistoryItem) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    // 현재 선택된 화이트밸런스 모드
    var wbMode by remember { mutableStateOf(WhiteBalanceMode.AUTO) }

    // UI 상태값
    var resultText by remember { mutableStateOf("대기 중") }
    var isLevel by remember { mutableStateOf(false) }
    var isDark by remember { mutableStateOf(false) }

    var isFlashOn by remember { mutableStateOf(false) }

    // 결과 화면 표시 여부
    var showResultScreen by remember { mutableStateOf(false) }

    // 업로드 / 결과 관련 상태값
    var isUploading by remember { mutableStateOf(false) }
    var guideText by remember { mutableStateOf("") }
    var uploadError by remember { mutableStateOf("") }
    var capturedPhotoFile by remember { mutableStateOf<File?>(null) }
    var adjustedImageUrl by remember { mutableStateOf<String?>(null) }

    var recommendedSettings by remember {
        mutableStateOf(RecommendedSettings())
    }

    // CameraX 카메라 객체 및 노출값 상태
    var camera by remember { mutableStateOf<Camera?>(null) }
    var exposureIndex by remember { mutableStateOf(0f) }
    var exposureRange by remember { mutableStateOf(0..0) }


    var selectedSetting by remember { mutableStateOf<String?>(null) }

    var detectedScene by remember { mutableStateOf("감지 중") }
    var detectedConfidence by remember { mutableStateOf("--") }
    var lastSceneDetectTime by remember { mutableStateOf(0L) }
    var isSceneDetecting by remember { mutableStateOf(false) }

    // 카메라 프리뷰를 보여주는 View
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // 프리뷰용 CameraX Preview 객체
    val preview = remember(wbMode) {
        val builder = Preview.Builder()
        val extender = Camera2Interop.Extender(builder)

        extender.setCaptureRequestOption(
            CaptureRequest.CONTROL_AWB_MODE,
            wbMode.awbMode
        )
        builder.build()
    }

    // 촬영용 ImageCapture 객체
    val imageCapture = remember(wbMode) {
        val builder = ImageCapture.Builder()
        val extender = Camera2Interop.Extender(builder)

        extender.setCaptureRequestOption(
            CaptureRequest.CONTROL_AWB_MODE,
            wbMode.awbMode
        )
        builder.build()
    }

    // 실시간 분석용 ImageAnalysis 객체
    val imageAnalysis = remember(wbMode) {
        val builder = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)

        val extender = Camera2Interop.Extender(builder)
        extender.setCaptureRequestOption(
            CaptureRequest.CONTROL_AWB_MODE,
            wbMode.awbMode
        )

        builder.build()
    }

    // 서버 주소
    val BASE_URL = "https://lamprophonic-unclosable-maryellen.ngrok-free.dev"

    var latestGalleryUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(Unit) {
        latestGalleryUri = getLatestGalleryImageUri(context)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val selectedFile = uriToFile(context, it)

            if (selectedFile != null) {
                capturedPhotoFile = selectedFile
                showResultScreen = true
                isUploading = true
                guideText = ""
                uploadError = ""
                adjustedImageUrl = null

                uploadCapturedImage(
                    photoFile = selectedFile,
                    baseUrl = BASE_URL,
                    onSuccess = { responseText, imageUrl, scene, settings ->
                        isUploading = false

                        val koreanScene = sceneToKorean(scene)

                        detectedScene = koreanScene
                        recommendedSettings = settings
                        guideText = "Scene: $koreanScene\n$responseText"
                        adjustedImageUrl = imageUrl

                        val category = koreanScene

                        val historyPhotoFile =
                            copyImageToHistoryStorage(context, selectedFile)

                        val newItem = HistoryItem(
                            category = category,
                            title = selectedFile.name,
                            date = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(Date()),
                            time = SimpleDateFormat("HH:mm", Locale.KOREA).format(Date()),
                            originalPhotoFile = historyPhotoFile,
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

                        onSaveHistory(newItem)
                    },
                    onError = { errorMessage ->
                        isUploading = false
                        uploadError = errorMessage
                    }
                )
            } else {
                showResultScreen = true
                isUploading = false
                uploadError = "이미지를 불러오지 못했습니다."
            }
        }
    }

//    // 갤러리에서 이미지 선택 launcher
//    val galleryLauncher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.GetContent()
//    ) { uri: Uri? ->
//        uri?.let {
//            val selectedFile = uriToFile(context, it)
//
//            if (selectedFile != null) {
//                // 선택한 이미지를 결과 화면으로 넘기고 서버 업로드 시작
//                capturedPhotoFile = selectedFile
//                currentScreen = AppScreen.RESULT
//                isUploading = true
//                guideText = ""
//                uploadError = ""
//                adjustedImageUrl = null
//
//                uploadCapturedImage(
//                    photoFile = selectedFile,
//                    baseUrl = BASE_URL,
//                    onSuccess = { responseText, imageUrl, scene ->
//                        android.util.Log.d("UPLOAD", "responseText = $responseText")
//                        android.util.Log.d("UPLOAD", "imageUrl = $imageUrl")
//                        android.util.Log.d("UPLOAD", "scene = $scene")
//
//                        isUploading = false
//                        guideText = "Scene: $scene\n$responseText"
//                        adjustedImageUrl = imageUrl
//
//                        // 보정 이미지가 있으면 갤러리에 저장
//                        if (!imageUrl.isNullOrBlank()) {
//                            saveImageFromUrlToGallery(context, imageUrl) { saved ->
//                                if (saved) {
//                                    android.util.Log.d("GALLERY", "보정 이미지 저장 완료")
//                                } else {
//                                    android.util.Log.e("GALLERY", "보정 이미지 저장 실패")
//                                }
//                            }
//                        }
//                    },
//                    onError = { errorMessage ->
//                        isUploading = false
//                        uploadError = errorMessage
//                    }
//                )
//            } else {
//                currentScreen = AppScreen.RESULT
//                isUploading = false
//                uploadError = "선택한 이미지를 불러오지 못했습니다."
//            }
//        }
//    }
    // 센서 매니저 가져오기
    val sensorManager = remember {
        context.getSystemService(SENSOR_SERVICE) as SensorManager
    }

    // 가속도 센서 가져오기
    val accelerometer = remember {
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    // 기울기 감지해서 수평 여부 판단
    DisposableEffect(isPortrait) {
        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                    val x = event.values[0]
                    val y = event.values[1]

                    isLevel = if (isPortrait) {
                        kotlin.math.abs(x) < 0.5f
                    } else {
                        kotlin.math.abs(y) < 0.5f
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        if (accelerometer != null) {
            sensorManager.registerListener(
                sensorListener,
                accelerometer,
                SensorManager.SENSOR_DELAY_UI
            )
        }

        onDispose {
            sensorManager.unregisterListener(sensorListener)
        }
    }

    // 프리뷰 화면 밝기 분석
    LaunchedEffect(imageAnalysis) {
        imageAnalysis.setAnalyzer(
            ContextCompat.getMainExecutor(context)
        ) { image ->

            val brightness = calculateBrightness(image)
            isDark = brightness < 90

            val now = System.currentTimeMillis()

            if (!isSceneDetecting && now - lastSceneDetectTime > 3000L) {
                lastSceneDetectTime = now
                isSceneDetecting = true

                val jpegBytes = imageProxyToJpegBytes(image)

                if (jpegBytes != null) {
                    uploadPreviewFrame(
                        imageBytes = jpegBytes,
                        baseUrl = BASE_URL,
                        onSuccess = { scene, confidence ->
                            detectedScene = sceneToKorean(scene)
                            detectedConfidence = "${(confidence * 100).toInt()}%"
                            isSceneDetecting = false
                        },
                        onError = {
                            detectedScene = "분석 실패"
                            detectedConfidence = "--"
                            isSceneDetecting = false
                        }
                    )
                } else {
                    isSceneDetecting = false
                }
            }

            image.close()
        }
    }

    // CameraX를 lifecycle에 연결
    LaunchedEffect(previewView, lifecycleOwner, preview, imageAnalysis, imageCapture) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                preview.setSurfaceProvider(previewView.surfaceProvider)

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.unbindAll()

                val boundCamera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis,
                    imageCapture
                )

                camera = boundCamera

                val state = boundCamera.cameraInfo.exposureState
                val range = state.exposureCompensationRange
                exposureRange = range.lower..range.upper
                exposureIndex = state.exposureCompensationIndex.toFloat()

                resultText = "카메라 준비 완료"
            } catch (e: Exception) {
                resultText = "카메라 바인딩 실패: ${e.message}"
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // 결과 화면
    if (showResultScreen) {
        ResultScreen(
            isUploading = isUploading,
            guideText = guideText,
            uploadError = uploadError,
            originalPhotoFile = capturedPhotoFile,
            adjustedImageUrl = adjustedImageUrl,
            scene = detectedScene,
            settings = recommendedSettings,
            onBackToCamera = {
                showResultScreen = false
                isUploading = false
                guideText = ""
                uploadError = ""
                adjustedImageUrl = null
                detectedScene = "감지 중"
            }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.84f),
            factory = { previewView }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.84f)
                .background(Color.Black.copy(alpha = 0.15f))
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CameraCircleButton(
                text = "←",
                onClick = {
                    onBack()
                }
            )

            SceneDetectPill(
                scene = "$detectedScene 감지",
                accuracy = detectedConfidence
            )

            CameraCircleButton(
                text = "⚙",
                onClick = {}
            )
        }

        if (isDark) {
            Text(
                text = "너무 어두워요",
                color = Color.Yellow,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp)
            )
        }

        GuideOverlay(isLevel = isLevel)

        AiGuideCard(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 18.dp)
                .padding(bottom = 220.dp),
            guide = getAiGuideByScene(detectedScene)
        )

        when (selectedSetting) {

            "WB" -> {

                WhiteBalanceInlineBar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 28.dp)
                        .padding(bottom = 140.dp),

                    selectedMode = wbMode,

                    onModeSelected = { mode ->
                        wbMode = mode
                        selectedSetting = null
                    },

                    onClose = {
                        selectedSetting = null
                    }
                )
            }

            "EV" -> {

                ExposureInlineBar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 28.dp)
                        .padding(bottom = 140.dp),

                    exposureIndex = exposureIndex,
                    exposureRange = exposureRange,

                    onExposureChange = { newValue ->
                        camera?.cameraControl?.setExposureCompensationIndex(newValue)
                        exposureIndex = newValue.toFloat()
                    },

                    onClose = {
                        selectedSetting = null
                    }
                )
            }

            else -> {

                CameraSettingBar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 28.dp)
                        .padding(bottom = 140.dp),

                    wb = wbMode.label,
                    ev = exposureIndex.toInt().toString(),

                    onWbClick = {
                        selectedSetting = "WB"
                    },

                    onEvClick = {
                        selectedSetting = "EV"
                    }
                )
            }
        }


        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 44.dp)
                .size(82.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable {
                    resultText = "촬영 시작"

                    val photoFile = File(
                        context.cacheDir,
                        SimpleDateFormat(
                            "yyyyMMdd_HHmmss",
                            Locale.US
                        ).format(System.currentTimeMillis()) + ".jpg"
                    )

                    val outputOptions =
                        ImageCapture.OutputFileOptions.Builder(photoFile).build()

                    imageCapture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {

                            override fun onImageSaved(
                                outputFileResults: ImageCapture.OutputFileResults
                            ) {
                                resultText = "촬영 성공"

                                val saved = saveImageToGallery(context, photoFile)
                                if (saved) {
                                    android.util.Log.d("GALLERY", "원본 사진 갤러리 저장 성공")
                                } else {
                                    android.util.Log.e("GALLERY", "원본 사진 갤러리 저장 실패")
                                }

                                capturedPhotoFile = photoFile
                                showResultScreen = true
                                isUploading = true
                                guideText = ""
                                uploadError = ""
                                adjustedImageUrl = null

                                uploadCapturedImage(
                                    photoFile = photoFile,
                                    baseUrl = BASE_URL,
                                    onSuccess = { responseText, imageUrl, scene, settings ->
                                        android.util.Log.d("UPLOAD", "responseText = $responseText")
                                        android.util.Log.d("UPLOAD", "imageUrl = $imageUrl")
                                        android.util.Log.d("UPLOAD", "scene = $scene")

                                        isUploading = false

                                        val koreanScene = sceneToKorean(scene)

                                        detectedScene = koreanScene
                                        recommendedSettings = settings
                                        guideText = "Scene: $koreanScene\n$responseText"
                                        adjustedImageUrl = imageUrl

                                        val category = koreanScene

                                        val newItem = HistoryItem(
                                            category = category,
                                            title = photoFile.name,
                                            date = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(Date()),
                                            time = SimpleDateFormat("HH:mm", Locale.KOREA).format(Date()),
                                            originalPhotoFile = photoFile,
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

                                        onSaveHistory(newItem)

                                        if (!imageUrl.isNullOrBlank()) {
                                            saveImageFromUrlToGallery(context, imageUrl) { savedAdjusted ->
                                                if (savedAdjusted) {
                                                    android.util.Log.d("GALLERY", "보정 이미지 저장 완료")
                                                } else {
                                                    android.util.Log.e("GALLERY", "보정 이미지 저장 실패")
                                                }
                                            }
                                        }
                                    },
                                    onError = { errorMessage ->
                                        isUploading = false
                                        uploadError = errorMessage
                                    }
                                )
                            }

                            override fun onError(
                                exception: ImageCaptureException
                            ) {
                                resultText = "촬영 실패: ${exception.message}"
                            }
                        }
                    )
                }
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 32.dp, bottom = 58.dp)
                .size(54.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF1F2937))
                .clickable {
                    galleryLauncher.launch("image/*")
                },
            contentAlignment = Alignment.Center
        ) {
            if (latestGalleryUri != null) {
                AsyncImage(
                    model = latestGalleryUri,
                    contentDescription = "최근 갤러리 사진",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = "🖼",
                    fontSize = 24.sp
                )
            }
        }

        CameraCircleButton(
            text = "⚡",
            isSelected = isFlashOn,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 32.dp, bottom = 58.dp),
            onClick = {
                isFlashOn = !isFlashOn

                imageCapture.flashMode =
                    if (isFlashOn) {
                        ImageCapture.FLASH_MODE_ON
                    } else {
                        ImageCapture.FLASH_MODE_OFF
                    }
            }
        )
        // 화면 전환 버튼
//        CameraCircleButton(
//            text = "↔",
//            modifier = Modifier
//                .align(Alignment.BottomEnd)
//                .padding(end = 32.dp, bottom = 60.dp),
//            onClick = {}
//        )

    }
}

private fun imageProxyToJpegBytes(image: ImageProxy): ByteArray? {
    return try {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(
            nv21,
            ImageFormat.NV21,
            image.width,
            image.height,
            null
        )

        val outputStream = ByteArrayOutputStream()

        yuvImage.compressToJpeg(
            Rect(0, 0, image.width, image.height),
            70,
            outputStream
        )

        outputStream.toByteArray()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
