@file:OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
package com.example.aicameraassistant

import android.Manifest
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
import androidx.compose.ui.viewinterop.AndroidView
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

import android.os.Environment
import android.provider.MediaStore

import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import coil.compose.AsyncImage
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Locale

enum class WhiteBalanceMode(
    val label: String,
    val awbMode: Int
) {
    AUTO("AUTO", CaptureRequest.CONTROL_AWB_MODE_AUTO),
    DAYLIGHT("DAY", CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT),
    CLOUDY("CLOUDY", CaptureRequest.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT),
    INCANDESCENT("INCAND.", CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT),
    FLUORESCENT("FLUOR.", CaptureRequest.CONTROL_AWB_MODE_FLUORESCENT)
}
class MainActivity : ComponentActivity() {

    // 카메라 권한 상태 저장
    private var permissionGrantedState by mutableStateOf(false)

    // 권한 요청 launcher
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            permissionGrantedState = isGranted
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 현재 카메라 권한 상태 확인
        permissionGrantedState = hasCameraPermission(this)

        // 권한 없으면 요청
        if (!permissionGrantedState) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            // 권한 상태에 따라 화면 분기
            if (permissionGrantedState) {
                CameraPreviewScreen()
            } else {
                CameraPermissionScreen(
                    onRequestPermission = {
                        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                )
            }
        }
    }
}

// 카메라 권한 체크 함수
private fun hasCameraPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}

@Composable
fun CameraPermissionScreen(
    onRequestPermission: () -> Unit
) {
    // 권한이 없을 때 표시되는 화면
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "카메라 권한이 필요합니다.")
            Button(onClick = onRequestPermission) {
                Text("권한 요청하기")
            }
        }
    }
}




@Composable
fun CameraPreviewScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    var wbMode by remember { mutableStateOf(WhiteBalanceMode.AUTO) }

    // UI 상태
    var resultText by remember { mutableStateOf("대기 중") }
    var isLevel by remember { mutableStateOf(false) }
    var isDark by remember { mutableStateOf(false) }
    var showResultScreen by remember { mutableStateOf(false) }

    var isUploading by remember { mutableStateOf(false) }
    var guideText by remember { mutableStateOf("") }
    var uploadError by remember { mutableStateOf("") }
    var capturedPhotoFile by remember { mutableStateOf<File?>(null) }
    var adjustedImageUrl by remember { mutableStateOf<String?>(null) }

    var camera by remember { mutableStateOf<Camera?>(null) }
    var exposureIndex by remember { mutableStateOf(0f) }
    var exposureRange by remember { mutableStateOf(0..0) }

    // CameraX 객체는 한 번만 생성해서 계속 사용
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    val preview = remember(wbMode) {
        val builder = Preview.Builder()
        val extender = Camera2Interop.Extender(builder)
        extender.setCaptureRequestOption(
            CaptureRequest.CONTROL_AWB_MODE,
            wbMode.awbMode
        )
        builder.build()
    }

    val imageCapture = remember(wbMode) {
        val builder = ImageCapture.Builder()
        val extender = Camera2Interop.Extender(builder)
        extender.setCaptureRequestOption(
            CaptureRequest.CONTROL_AWB_MODE,
            wbMode.awbMode
        )
        builder.build()
    }

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


    val BASE_URL = "https://lamprophonic-unclosable-maryellen.ngrok-free.dev"

    // 센서 매니저
    val sensorManager = remember {
        context.getSystemService(SENSOR_SERVICE) as SensorManager
    }

    val accelerometer = remember {
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    // 기울기 감지
    DisposableEffect(isPortrait) {
        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                    val x = event.values[0]
                    val y = event.values[1]

                    isLevel = if (isPortrait) {
                        kotlin.math.abs(x) < 1.5f
                    } else {
                        kotlin.math.abs(y) < 1.5f
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

    // 밝기 분석기 연결
    LaunchedEffect(imageAnalysis) {
        imageAnalysis.setAnalyzer(
            ContextCompat.getMainExecutor(context)
        ) { image ->
            val brightness = calculateBrightness(image)
            isDark = brightness < 90
            image.close()
        }
    }

    // CameraX bind
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

    // 결과 화면으로 전환
    if (showResultScreen) {
        ResultScreen(
            isUploading = isUploading,
            guideText = guideText,
            uploadError = uploadError,
            adjustedImageUrl = adjustedImageUrl,
            onBackToCamera = {
                showResultScreen = false
                isUploading = false
                guideText = ""
                uploadError = ""
            }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // 카메라 프리뷰
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { previewView }
        )

        // 가이드선
        GuideOverlay(isLevel = isLevel)

        // 상태 텍스트
        Text(
            text = "업로드 결과: $resultText",
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 50.dp)
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 250.dp)
                .fillMaxWidth(0.9f)
                .horizontalScroll(rememberScrollState())
        ) {
            WhiteBalanceMode.entries.forEach { mode ->
                Button(
                    onClick = { wbMode = mode },
                    modifier = Modifier.padding(end = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (wbMode == mode) Color.White else Color.DarkGray,
                        contentColor = if (wbMode == mode) Color.Black else Color.White
                    )
                ) {
                    Text(mode.label)
                }
            }
        }

        // 노출값 조절 UI
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 180.dp)
                .fillMaxWidth(0.85f)
        ) {
            Text(
                text = "노출값(EV): ${exposureIndex.toInt()}",
                color = Color.White
            )

            Slider(
                value = exposureIndex,
                onValueChange = { newValue ->
                    exposureIndex = newValue
                    camera?.cameraControl?.setExposureCompensationIndex(newValue.toInt())
                },
                valueRange = exposureRange.first.toFloat()..exposureRange.last.toFloat(),
                steps = (exposureRange.last - exposureRange.first - 1).coerceAtLeast(0)
            )

            Text(
                text = "범위: ${exposureRange.first} ~ ${exposureRange.last}",
                color = Color.White
            )
        }

        // 테스트 업로드 버튼
        Button(
            onClick = {
                uploadTestImage(
                    context = context,
                    onResult = { resultText = it }
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
        ) {
            Text("서버 업로드 테스트")
        }


        // 셔터 버튼
        Button(
            onClick = {
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

                            // 🔹 여기 추가 (원본 사진 갤러리 저장)
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

                            uploadCapturedImage(
                                photoFile = photoFile,
                                baseUrl = BASE_URL,
                                onSuccess = {responseText, imageUrl, scene ->

                                    android.util.Log.d("UPLOAD", "responseText = $responseText")
                                    android.util.Log.d("UPLOAD", "imageUrl = $imageUrl")
                                    android.util.Log.d("UPLOAD", "scene = $scene")

                                    isUploading = false
                                    guideText = "Scene: $scene\n$responseText"
                                    adjustedImageUrl = imageUrl

                                    if (!imageUrl.isNullOrBlank()) {
                                        saveImageFromUrlToGallery(context, imageUrl) { saved ->
                                            if (saved) {
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
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .size(72.dp)
                .clip(CircleShape),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            )
        ) {
            Text("")
        }

        // 밝기 경고
        if (isDark) {
            Text(
                text = "너무 어두워요",
                color = Color.Yellow,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 120.dp)
            )
        }
    }
}


fun calculateBrightness(image: ImageProxy): Double {

    val buffer = image.planes[0].buffer
    val data = ByteArray(buffer.remaining())
    buffer.get(data)

    var sum = 0L
    for (byte in data) {
        sum += (byte.toInt() and 0xFF)
    }

    return sum.toDouble() / data.size
}

@Composable
fun GuideOverlay(
    isLevel: Boolean
) {
    val lineColor = if (isLevel) Color.Green else Color.Red

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {

        val centerY = size.height / 2f

        // 항상 가로 수평선
        drawLine(
            color = lineColor,
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun ResultScreen(
    isUploading: Boolean,
    guideText: String,
    uploadError: String,
    adjustedImageUrl: String?,
    onBackToCamera: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            when {
                isUploading -> {
                    CircularProgressIndicator()
                    Text("분석 중...")
                }

                uploadError.isNotEmpty() -> {
                    Text(uploadError, color = Color.Red)
                }

                else -> {

                    Text("분석 결과")

                    if (!adjustedImageUrl.isNullOrBlank()) {

                        Text("이미지 URL: $adjustedImageUrl")

                        AsyncImage(
                            model = adjustedImageUrl,
                            contentDescription = "보정된 이미지",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        )

                    } else {
                        Text("보정 이미지 URL이 없습니다.", color = Color.Red)
                    }

                    Text(text = guideText)
                }
            }
        }

        Button(
            onClick = onBackToCamera,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Text("카메라로 돌아가기")
        }
    }
}

// 테스트 이미지 업로드
private fun uploadTestImage(
    context: Context,
    onResult: (String) -> Unit
) {
    val url = "https://lamprophonic-unclosable-maryellen.ngrok-free.dev/upload"

    try {
        // drawable의 testimage 읽기
        val resourceId = context.resources.getIdentifier(
            "testimage",
            "drawable",
            context.packageName
        )

        if (resourceId == 0) {
            onResult("drawable/testimage 파일을 찾을 수 없음")
            return
        }

        val inputStream = context.resources.openRawResource(resourceId)

        // 캐시 디렉토리에 임시 파일 생성
        val tempFile = File(context.cacheDir, "testimage.jpg")
        FileOutputStream(tempFile).use { output ->
            inputStream.copyTo(output)
        }

        val client = OkHttpClient()


        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("user_id", "test_user_001")
            .addFormDataPart(
                "file",
                "testimage.jpg",
                tempFile.asRequestBody("image/jpeg".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()


        // 네트워크 요청은 백그라운드에서 실행
        Thread {
            try {
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()

                    if (!response.isSuccessful) {
                        onResult("실패: HTTP ${response.code}")
                        return@use
                    }

                    // JSON 응답에서 ok 값 확인
                    val ok = try {
                        JSONObject(body).optBoolean("ok", false)
                    } catch (e: Exception) {
                        false
                    }

                    onResult(
                        if (ok) "성공(ok=true)"
                        else "응답은 왔는데 ok=false"
                    )
                }
            } catch (e: Exception) {
                onResult("실패: ${e.message}")
            }
        }.start()

    } catch (e: Exception) {
        onResult("로컬 이미지 읽기 실패: ${e.message}")
    }
}

// 촬영 이미지 업로드
private fun uploadCapturedImage(
    photoFile: File,
    baseUrl: String,
    onSuccess: (String, String?, String?) -> Unit,
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

                val (parsedMessage, resolvedImageUrl, parsedScene) = try {
                    val json = JSONObject(body)

                    var imageUrl: String? = null
                    var sceneText: String? = null

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

                    Triple(messageText, imageUrl, sceneText)

                } catch (e: Exception) {
                    android.util.Log.e("UPLOAD", "json parse error", e)
                    Triple(body, null, null)
                }

                onSuccess(parsedMessage, resolvedImageUrl, parsedScene)
            }
        } catch (e: Exception) {
            android.util.Log.e("UPLOAD", "upload failed", e)
            onError("업로드 실패: ${e.message}")
        }
    }.start()
}

// 원본 이미지를 갤러리에 저장하는 함수
private fun saveImageToGallery(context: Context, imageFile: File): Boolean {
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
private fun saveImageFromUrlToGallery(
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