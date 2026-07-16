@file:OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
package com.example.aicameraassistant

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

import com.google.android.gms.location.LocationServices

import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Handler
import android.os.Looper
import java.io.ByteArrayOutputStream
import okhttp3.RequestBody.Companion.toRequestBody

import coil.compose.AsyncImage
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import org.json.JSONArray

private const val WEATHER_API_KEY =  "8767d3ec1b1f549c8f473c665dd5b2f2"

enum class WhiteBalanceMode(
    val label: String,
    val awbMode: Int
) {
    // 화이트밸런스 모드 목록
    AUTO("자동", CaptureRequest.CONTROL_AWB_MODE_AUTO),
    DAYLIGHT("맑음", CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT),
    CLOUDY("흐림", CaptureRequest.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT),
    INCANDESCENT("백열등", CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT),
    FLUORESCENT("형광등", CaptureRequest.CONTROL_AWB_MODE_FLUORESCENT)
}

enum class AppScreen {
    MENU,   // 시작 메뉴 화면
    CAMERA, // 카메라 촬영 화면
    RESULT  // 결과 화면
}

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
            val context = LocalContext.current

            val galleryLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                uri?.let {
                    val selectedFile = uriToFile(context, it)

                    if (selectedFile != null) {
                        capturedPhotoFile = selectedFile

                        currentScreen = AppScreen.RESULT
                        isUploading = true
                        guideText = ""
                        uploadError = ""
                        adjustedImageUrl = null

                        uploadCapturedImage(
                            photoFile = selectedFile,
                            baseUrl = "https://lamprophonic-unclosable-maryellen.ngrok-free.dev",
                            onSuccess = { responseText, imageUrl, scene, settings ->

                                isUploading = false
                                detectedScene = scene
                                recommendedSettings = settings
                                guideText = "Scene: $scene\n$responseText"
                                adjustedImageUrl = imageUrl

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

                                historyItems = listOf(newItem) + historyItems
                                saveHistoryItems(this, historyItems)
                            },
                            onError = { errorMessage ->
                                isUploading = false
                                uploadError = errorMessage
                            }
                        )
                    } else {
                        currentScreen = AppScreen.RESULT
                        isUploading = false
                        uploadError = "이미지를 불러오지 못했습니다."
                    }
                }
            }

//            KlickWebViewScreen()

            if (permissionGrantedState) {

                when (currentScreen) {

                    AppScreen.MENU -> MainTabScreen(
                        onCameraClick = {
                            currentScreen = AppScreen.CAMERA
                        },
                        onGalleryClick = {
                            galleryLauncher.launch("image/*")
                        },
                        historyItems = historyItems
                    )

                    AppScreen.CAMERA -> CameraPreviewScreen(
                        onBack = {
                            currentScreen = AppScreen.MENU
                        },
                        onSaveHistory = { newItem ->
                            historyItems = listOf(newItem) + historyItems
                            saveHistoryItems(this, historyItems)
                        }
                    )

                    AppScreen.RESULT -> {
                        ResultScreen(
                            isUploading = isUploading,
                            guideText = guideText,
                            uploadError = uploadError,
                            originalPhotoFile = capturedPhotoFile,
                            adjustedImageUrl = adjustedImageUrl,
                            scene = detectedScene,
                            settings = recommendedSettings,
                            onBackToCamera = {
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

// 카메라 권한 확인 함수
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
    // 카메라 권한이 없을 때 표시되는 화면
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

// 홈 화면 UI
@Composable
fun HomeScreen(
    historyItems: List<HistoryItem>,
    onCameraClick: () -> Unit,
    onViewAllClick: () -> Unit
) {
    var weatherType by remember {
        mutableStateOf(WeatherType.SUNNY)
    }
    val weatherTip = getWeatherTip(weatherType)

    val context = LocalContext.current

    LaunchedEffect(Unit) {

        val fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(context)

        try {

            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->

                    if (location != null) {

                        fetchCurrentWeatherType(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            apiKey = WEATHER_API_KEY
                        ) { result ->

                            weatherType = result
                        }
                    }
                }

        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFFFF))
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Klick",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF061B31)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1C1E53)
            ),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                Text(
                    text = "AI 카메라 어시스턴트",
                    color = Color.White,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "장면을 읽고,\n빛을 짚어드립니다.",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "5가지 장면을 자동으로 인식해 ISO·셔터·화이트밸런스를 추천합니다.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onCameraClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(color = 0xFF523AFB),
                        contentColor = Color(0xFFFFFFFF)
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "카메라 시작 ->",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "지원 장면",
            color = Color(0xFF000000),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(16.dp))

        val supportScenes = listOf(
            Triple("음식", R.drawable.food, Color(0xFFE91E63)),
            Triple("풍경", R.drawable.landscape, Color(0xFF2ECC71)),
            Triple("야경", R.drawable.night, Color(0xFF6C5CE7)),
            Triple("명암", R.drawable.contrast, Color(0xFF000000)),
            Triple("인물", R.drawable.portrait, Color(0xFFFF66CC))
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            supportScenes.forEach { scene ->
                val title = scene.first
                val imageRes = scene.second
                val dotColor = scene.third

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(0.72f)
                        .clip(RoundedCornerShape(14.dp))
                ) {
                    Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.25f))
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(5.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )

                    Text(
                        text = title,
                        color = Color(0xFFFFFFFF),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(7.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "최근 촬영",
                color = Color.Black,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "전체보기",
                color = Color(0xFF8B5CF6),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable {
                    onViewAllClick()
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            historyItems.take(3).forEach { item ->

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1F2937)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {

                    if (item.originalPhotoFile != null) {

                        AsyncImage(
                            model = item.originalPhotoFile,
                            contentDescription = item.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF1F5F9)
            )
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFFD36E),
                                    Color(0xFFFF7AD9)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = "☀",
                        color = Color.White,
                        fontSize = 22.sp
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "오늘의 촬영 팁",
                        color = Color(0xFF7C3AED),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = weatherTip.weatherText,
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = weatherTip.tipText,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }

//                Text(
//                    text = "→",
//                    color = Color.Gray,
//                    fontSize = 20.sp
//                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

fun mapWeatherCodeToType(code: Int, cloudiness: Int): WeatherType {
    return when {
        code in 200..599 -> WeatherType.RAIN
        code in 600..699 -> WeatherType.SNOW
        code == 800 -> WeatherType.SUNNY
        code in 801..804 -> WeatherType.CLOUDY
        cloudiness >= 60 -> WeatherType.CLOUDY
        else -> WeatherType.SUNNY
    }
}

fun fetchCurrentWeatherType(
    latitude: Double,
    longitude: Double,
    apiKey: String,
    onResult: (WeatherType) -> Unit
) {
    Thread {
        try {
            val url =
                "https://api.openweathermap.org/data/2.5/weather?lat=$latitude&lon=$longitude&appid=$apiKey&units=metric"

            val client = OkHttpClient()
            val request = Request.Builder()
                .url(url)
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    onResult(WeatherType.SUNNY)
                    return@use
                }

                val json = JSONObject(body)

                val weatherArray = json.getJSONArray("weather")
                val weatherCode = weatherArray.getJSONObject(0).optInt("id", 800)

                val cloudiness = json
                    .optJSONObject("clouds")
                    ?.optInt("all", 0) ?: 0

                val weatherType = mapWeatherCodeToType(weatherCode, cloudiness)

                onResult(weatherType)
            }
        } catch (e: Exception) {
            onResult(WeatherType.SUNNY)
        }
    }.start()
}

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1F2937)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = title,
                color = Color.Gray,
                fontSize = 13.sp
            )
        }
    }
}

sealed class BottomTab(
    val label: String,
    val icon: ImageVector
) {
    object Home : BottomTab("홈", Icons.Default.Home)
    object Camera : BottomTab("카메라", Icons.Default.PhotoCamera)
    object Gallery : BottomTab("갤러리", Icons.Default.PhotoLibrary)
    object History : BottomTab("히스토리", Icons.Default.CalendarMonth)
}

fun formatHistoryDateLabel(date: String): String {
    val formatter = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA)

    return try {
        val itemDate = formatter.parse(date)
        val today = formatter.format(Date())
        val yesterday = formatter.format(
            Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
        )

        when (date) {
            today -> "오늘"
            yesterday -> "어제"
            else -> date
        }
    } catch (e: Exception) {
        date
    }
}


@Composable
fun HistoryScreen(
    historyItems: List<HistoryItem>
) {
    var selectedFilter by remember { mutableStateOf("전체") }
    var selectedItem by remember { mutableStateOf<HistoryItem?>(null) }

    val filters = listOf("전체", "음식", "풍경", "야경", "명암", "인물")

    val filteredItems = historyItems
        .filter { selectedFilter == "전체" || it.category == selectedFilter }
        .sortedByDescending { "${it.date} ${it.time}" }

    val groupedItems = filteredItems.groupBy { it.date }

    if (selectedItem != null) {
        HistoryDetailScreen(
            item = selectedItem!!,
            onBack = { selectedItem = null }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = "GALLERY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7C8798)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "장면별 히스토리",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Light,
                    color = Color(0xFF1F2937)
                )
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(
                        width = 1.dp,
                        color = Color(0xFFE5E7EB),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        // TODO: 기능 나중에 추가
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { filter ->
                HistoryFilterChip(
                    text = filter,
                    selected = selectedFilter == filter,
                    count = if (filter == "전체") historyItems.size else null,
                    onClick = { selectedFilter = filter },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(28.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            groupedItems.forEach { (date, itemsForDate) ->
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatHistoryDateLabel(date),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937)
                        )

                        Text(
                            text = itemsForDate.size.toString(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7C8798)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.height(
                            (((itemsForDate.size + 2) / 3) * 118).dp
                        )
                    ) {
                        items(itemsForDate) { item ->
                            HistoryPhotoCard(
                                item = item,
                                onClick = { selectedItem = item }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryFilterChip(
    text: String,
    selected: Boolean,
    count: Int? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = when (text) {
        "음식" -> Color(0xFFE91E63)
        "풍경" -> Color(0xFF2ECC71)
        "야경" -> Color(0xFF6C5CE7)
        "명암" -> Color(0xFF0F172A)
        "인물" -> Color(0xFFE056FD)
        else -> Color.White
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (selected) Color(0xFF0B2341)
                else Color.White
            )
            .border(
                width = 1.dp,
                color = if (selected)
                    Color(0xFF0B2341)
                else
                    Color(0xFFE5E7EB),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (text != "전체") {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(categoryColor)
            )

            Spacer(modifier = Modifier.width(4.dp))
        }

        Text(
            text = if (count != null) "$text $count" else text,
            color = if (selected) Color.White else Color(0xFF4B5563),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
fun HistoryPhotoCard(
    item: HistoryItem,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFE5E7EB))
            .clickable { onClick() }
    ) {
        if (item.originalPhotoFile != null) {
            AsyncImage(
                model = item.originalPhotoFile,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Text(
            text = item.category,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F2937),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(7.dp)
                .background(
                    Color.White.copy(alpha = 0.85f),
                    RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 6.dp, vertical = 3.dp)
        )

        Text(
            text = item.time,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(7.dp)
                .background(
                    Color.Black.copy(alpha = 0.45f),
                    RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

//@Composable
//fun HistoryScreen(
//    historyItems: List<HistoryItem>
//) {
//    var selectedCategory by remember { mutableStateOf<String?>(null) }
//    var selectedItem by remember { mutableStateOf<HistoryItem?>(null) }
//
//    val allCategories = historyItems.map { it.category }.distinct()
//    val groupedItems = historyItems.groupBy { it.category }
//
//    Box(
//        modifier = Modifier.fillMaxSize()
//    ) {
//        // 🔥 배경 이미지
//        Image(
//            painter = painterResource(id = R.drawable.background1),
//            contentDescription = "배경 이미지",
//            modifier = Modifier.fillMaxSize(),
//            contentScale = ContentScale.Crop
//        )
//
//        // 🔽 기존 화면들
//        when {
//            selectedItem != null -> {
//                HistoryDetailScreen(
//                    item = selectedItem!!,
//                    onBack = {
//                        selectedItem = null
//                    }
//                )
//            }
//
//            selectedCategory != null -> {
//                HistoryCategoryScreen(
//                    category = selectedCategory!!,
//                    itemsList = groupedItems[selectedCategory!!] ?: emptyList(),
//                    onBack = {
//                        selectedCategory = null
//                    },
//                    onItemClick = { clickedItem ->
//                        selectedItem = clickedItem
//                    }
//                )
//            }
//
//            else -> {
//                HistoryListScreen(
//                    categories = allCategories,
//                    groupedItems = groupedItems,
//                    onCategoryClick = { clickedCategory ->
//                        selectedCategory = clickedCategory
//                    },
//                    onItemClick = { clickedItem ->
//                        selectedItem = clickedItem
//                    }
//                )
//            }
//        }
//    }
//}

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

enum class WeatherType {
    SUNNY,
    CLOUDY,
    RAIN,
    SNOW
}

data class WeatherTip(
    val weatherText: String,
    val tipText: String
)

fun getWeatherTip(weatherType: WeatherType): WeatherTip {
    return when (weatherType) {

        WeatherType.SUNNY -> WeatherTip(
            weatherText = "맑음 · 골든아워 촬영 추천",
            tipText = "인물 · 풍경 촬영에 최적인 시간대입니다"
        )

        WeatherType.CLOUDY -> WeatherTip(
            weatherText = "흐림 · 부드러운 자연광",
            tipText = "인물 피부톤 표현에 좋은 날씨입니다"
        )

        WeatherType.RAIN -> WeatherTip(
            weatherText = "비 · 반사광 활용 추천",
            tipText = "물웅덩이와 야간 조명을 활용해보세요"
        )

        WeatherType.SNOW -> WeatherTip(
            weatherText = "눈 · 차가운 색감 강조",
            tipText = "노출을 높여 깨끗한 눈 표현을 해보세요"
        )
    }
}

// 목록 화면 추가
@Composable
fun HistoryListScreen(
    categories: List<String>,
    groupedItems: Map<String, List<HistoryItem>>,
    onCategoryClick: (String) -> Unit,
    onItemClick: (HistoryItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "History",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
// 나중에 정렬 사용할 예정
//            Button(onClick = { }) {
//                Text("정렬")
//            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(categories) { category ->
                val itemsInCategory = groupedItems[category] ?: emptyList()

                Column {
                    Text(
                        text = category,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .clickable { onCategoryClick(category) }
                    )

                    if (itemsInCategory.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .background(Color(0xFFEAF6FD)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "아직 저장된 사진이 없습니다",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(itemsInCategory) { historyItem ->
                                Column(
                                    modifier = Modifier
                                        .width(120.dp)
                                        .clickable {
                                            onItemClick(historyItem)
                                        }
                                ) {
                                    if (historyItem.originalPhotoFile != null) {
                                        AsyncImage(
                                            model = historyItem.originalPhotoFile,
                                            contentDescription = historyItem.title,
                                            modifier = Modifier
                                                .size(120.dp)
                                                .background(Color(0xFFEAF6FD)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(120.dp)
                                                .background(Color(0xFF87CEEB)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "이미지 없음",
                                                color = Color.White,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = historyItem.title,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        color = Color.DarkGray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 카테고리 전체 화면
//@Composable
//fun HistoryCategoryScreen(
//    category: String,
//    itemsList: List<HistoryItem>,
//    onBack: () -> Unit,
//    onItemClick: (HistoryItem) -> Unit
//) {
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp)
//    ) {
//        Row(
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            IconButton(onClick = onBack) {
//                Icon(
//                    imageVector = Icons.Default.ArrowBack,
//                    contentDescription = "뒤로가기"
//                )
//            }
//
//            Spacer(modifier = Modifier.width(8.dp))
//
//            Text(
//                text = category,
//                fontSize = 22.sp,
//                fontWeight = FontWeight.Bold
//            )
//        }
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        if (itemsList.isEmpty()) {
//            Box(
//                modifier = Modifier.fillMaxSize(),
//                contentAlignment = Alignment.Center
//            ) {
//                Text(
//                    text = "이 카테고리에 저장된 사진이 없습니다",
//                    color = Color.Gray
//                )
//            }
//        } else {
//            LazyVerticalGrid(
//                columns = GridCells.Fixed(3),
//                verticalArrangement = Arrangement.spacedBy(12.dp),
//                horizontalArrangement = Arrangement.spacedBy(12.dp),
//                modifier = Modifier.fillMaxSize()
//            ) {
//                items(itemsList) { item ->
//                    Column(
//                        modifier = Modifier
//                            .clickable {
//                                onItemClick(item)
//                            }
//                    ) {
//                        if (item.originalPhotoFile != null) {
//                            AsyncImage(
//                                model = item.originalPhotoFile,
//                                contentDescription = item.title,
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .aspectRatio(1f)
//                                    .background(Color(0xFFEAF6FD)),
//                                contentScale = ContentScale.Crop
//                            )
//                        } else {
//                            Box(
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .aspectRatio(1f)
//                                    .background(Color(0xFF87CEEB)),
//                                contentAlignment = Alignment.Center
//                            ) {
//                                Text(
//                                    text = "이미지 없음",
//                                    color = Color.White,
//                                    fontSize = 12.sp
//                                )
//                            }
//                        }
//
//                        Spacer(modifier = Modifier.height(4.dp))
//
//                        Text(
//                            text = item.title,
//                            fontSize = 11.sp,
//                            maxLines = 1,
//                            color = Color.DarkGray
//                        )
//                    }
//                }
//            }
//        }
//    }
//}

// 상세 화면 추가
@Composable
fun HistoryDetailScreen(
    item: HistoryItem,
    onBack: () -> Unit
) {
    ResultScreen(
        isUploading = false,
        guideText = item.guideText,
        uploadError = "",
        originalPhotoFile = item.originalPhotoFile,
        adjustedImageUrl = item.adjustedImageUrl,
        scene = item.category,
        settings = item.settings,
        onBackToCamera = onBack
    )
}


@Composable
fun MainTabScreen(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    historyItems: List<HistoryItem>
) {
    var selectedTab by remember {
        mutableStateOf<BottomTab>(BottomTab.Home)
    }

    val tabs = listOf(
        BottomTab.Home,
        BottomTab.Camera,
        BottomTab.Gallery,
        BottomTab.History
    )

    Scaffold(
        containerColor = Color(0xFFFFFFFF),

        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFFFFFFFF)
            ) {
                tabs.forEach { tab ->

                    NavigationBarItem(
                        selected = selectedTab == tab,

                        onClick = {

                            selectedTab = tab

                            when (tab) {

                                BottomTab.Camera -> {
                                    onCameraClick()
                                }

                                BottomTab.Gallery -> {
                                    onGalleryClick()
                                }

                                else -> Unit
                            }
                        },

                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label
                            )
                        },

                        label = {
                            Text(tab.label)
                        },

                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF8B5CF6),
                            selectedTextColor = Color(0xFF8B5CF6),
                            indicatorColor = Color(0x332A2F45),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                }
            }
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            when (selectedTab) {

                BottomTab.Home -> {
                    HomeScreen(
                        historyItems = historyItems,
                        onCameraClick = onCameraClick,
                        onViewAllClick = {
                            selectedTab = BottomTab.History
                        }
                    )
                }

                BottomTab.History -> {
                    HistoryScreen(historyItems = historyItems)
                }

                else -> Unit
            }
        }
    }
}

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

@Composable
fun CameraCircleButton(
    text: String,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) Color(0xFFFFD166)
                else Color.White.copy(alpha = 0.28f)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.Black else Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CameraLogoPill() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black.copy(alpha = 0.35f))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "Klick",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SceneDetectPill(
    scene: String,
    accuracy: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "🌺",
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = scene,
            color = Color(0xFF1F2937),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = accuracy,
            color = Color(0xFFE056FD),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AiGuideCard(
    guide: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.94f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF9D4EDD)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✦",
                color = Color.White,
                fontSize = 20.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = "KLICK AI · 구도",
                color = Color(0xFF6D28D9),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = guide,
                color = Color(0xFF1F2937),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CameraSettingBar(
//    iso: String,
//    shutter: String,
//    aperture: String,
    wb: String,
    ev: String,
    modifier: Modifier = Modifier,
//    onIsoClick: () -> Unit,
//    onShutterClick: () -> Unit,
//    onApertureClick: () -> Unit,
    onWbClick: () -> Unit,
    onEvClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF263238).copy(alpha = 0.88f))
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
//        CameraSettingItem("ISO", iso, onIsoClick)
//        CameraSettingItem("SS", shutter, onShutterClick)
//        CameraSettingItem("F", aperture, onApertureClick)
        CameraSettingItem("WB", wb, onWbClick)
        CameraSettingItem("EV", ev, onEvClick)
    }
}

@Composable
fun CameraSettingItem(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun WhiteBalanceInlineBar(
    selectedMode: WhiteBalanceMode,
    onModeSelected: (WhiteBalanceMode) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF263238).copy(alpha = 0.88f))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "←",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(CircleShape)
                .clickable { onClose() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )

        WhiteBalanceMode.values().forEach { mode ->
            val selected = mode == selectedMode

            Text(
                text = mode.label,
                color = if (selected) Color.Black else Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (selected) Color(0xFFFFD166)
                        else Color.White.copy(alpha = 0.16f)
                    )
                    .clickable {
                        onModeSelected(mode)
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
fun ExposureInlineBar(
    exposureIndex: Float,
    exposureRange: IntRange,
    onExposureChange: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF263238).copy(alpha = 0.88f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "←",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(CircleShape)
                .clickable { onClose() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "EV ${exposureIndex.toInt()}",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.width(10.dp))

        Slider(
            value = exposureIndex,
            onValueChange = { value ->
                onExposureChange(value.toInt())
            },
            valueRange = exposureRange.first.toFloat()..exposureRange.last.toFloat(),
            steps = (exposureRange.last - exposureRange.first - 1).coerceAtLeast(0),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun EvSettingItem(
    value: String,
    onMinusClick: () -> Unit,
    onPlusClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "-",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(CircleShape)
                .clickable { onMinusClick() }
                .padding(horizontal = 6.dp)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "EV",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = value,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = "+",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(CircleShape)
                .clickable { onPlusClick() }
                .padding(horizontal = 6.dp)
        )
    }
}

@Composable
fun ResultScreen(
    isUploading: Boolean,
    guideText: String,
    uploadError: String,
    originalPhotoFile: File?,
    adjustedImageUrl: String?,
    scene: String?,
    settings: RecommendedSettings,
    onBackToCamera: () -> Unit
) {
    val resultImage = adjustedImageUrl ?: originalPhotoFile
    val sceneKor = sceneToKorean(scene)
    val sceneEng = sceneToEnglish(scene)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FC))
    ) {
        when {
            isUploading -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color(0xFF5B35FF))

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "분석 중...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                }
            }

            uploadError.isNotEmpty() -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = uploadError,
                        color = Color.Red,
                        fontSize = 15.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onBackToCamera,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF5B35FF),
                            contentColor = Color.White
                        )
                    ) {
                        Text("돌아가기")
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 28.dp)
                ) {
                    ResultImageCard(
                        imageModel = resultImage,
                        category = sceneKor,
                        accuracy = "96%"
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = "KLICK 추천 · $sceneEng",
                        color = Color(0xFF5B35FF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (guideText.isNotBlank()) {
                            guideText
                        } else {
                            "촬영 환경에 맞는 추천값을 적용해보세요."
                        },
                        color = Color(0xFF1F2937),
                        fontSize = 20.sp,
                        lineHeight = 38.sp,
                        fontWeight = FontWeight.Light
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    ResultSettingGrid(settings = settings)

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = "촬영 가이드",
                        color = Color(0xFF1F2937),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    ResultGuideCard(
                        number = "01",
                        title = "심도",
                        description = "피사체에 초점을 맞추고 배경은 자연스럽게 분리"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ResultGuideCard(
                        number = "02",
                        title = "광원",
                        description = "45° 측면광을 활용해 입체감 확보"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ResultGuideCard(
                        number = "03",
                        title = "구도",
                        description = "주 피사체를 그리드 교차점 근처에 배치"
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onBackToCamera,
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF5B35FF),
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "뒤로가기",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = onBackToCamera,
                            modifier = Modifier
                                .width(92.dp)
                                .height(60.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF5B35FF)
                            )
                        ) {
                            Text(
                                text = "저장",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

fun sceneToKorean(scene: String?): String {
    return when (scene?.lowercase()) {
        "food", "음식" -> "음식"
        "landscape", "풍경" -> "풍경"
        "night", "야경" -> "야경"
        "contrast", "명암" -> "명암"
        "portrait", "person", "인물" -> "인물"
        else -> "기타"
    }
}

fun getAiGuideByScene(scene: String?): String {
    return when (scene) {
        "음식" -> "45° 측면, 접시는 그리드 교차점에 두기"
        "풍경" -> "수평선을 맞추고 하늘과 지면 비율 조절하기"
        "야경" -> "흔들림을 줄이고 밝은 광원을 피하기"
        "명암" -> "빛과 그림자의 경계를 살려 촬영하기"
        "인물" -> "얼굴에 빛이 고르게 닿도록 위치 조정하기"
        else -> "장면을 감지하면 촬영 가이드를 알려드려요"
    }
}

fun sceneToEnglish(scene: String?): String {
    return when (scene?.lowercase()) {
        "food", "음식" -> "FOOD"
        "landscape", "풍경" -> "LANDSCAPE"
        "night", "야경" -> "NIGHT"
        "contrast", "명암" -> "CONTRAST"
        "portrait", "person", "인물" -> "PORTRAIT"
        else -> "UNKNOWN"
    }
}

@Composable
fun ResultImageCard(
    imageModel: Any?,
    category: String,
    accuracy: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(330.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFE5E7EB))
    ) {
        if (imageModel != null) {
            AsyncImage(
                model = imageModel,
                contentDescription = "분석 이미지",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.92f))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "👤",
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = category,
                color = Color(0xFF1F2937),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = accuracy,
                color = Color(0xFF10B981),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFD1FAE5))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
fun ResultSettingGrid(
    settings: RecommendedSettings
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth()) {
                ResultSettingCell(
                    "↔ ISO",
                    settings.iso.toString(),
                    Modifier.weight(1f)
                )

                ResultSettingCell(
                    "◉ 셔터",
                    settings.shutter,
                    Modifier.weight(1f)
                )

                ResultSettingCell(
                    "◉ 조리개",
                    settings.aperture,
                    Modifier.weight(1f)
                )
            }

            Divider(color = Color(0xFFE5E7EB))

            Row(modifier = Modifier.fillMaxWidth()) {
                ResultSettingCell(
                    "☼ WB",
                    settings.whiteBalance,
                    Modifier.weight(1f)
                )

                ResultSettingCell(
                    "↯ EV",
                    settings.ev,
                    Modifier.weight(1f)
                )

                ResultSettingCell(
                    "✣ 초점",
                    settings.focalLength,
                    Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ResultSettingCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(92.dp)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            color = Color(0xFF9CA3AF),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = value,
            color = Color(0xFF1F2937),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ResultGuideCard(
    number: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFEDE9FE)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                color = Color(0xFF6D28D9),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = title,
                color = Color(0xFF1F2937),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                color = Color(0xFF9CA3AF),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// 이미지 밝기 계산 함수
// Y plane 평균값으로 밝기를 대략 계산
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

private fun uploadPreviewFrame(
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

@Composable
fun GuideOverlay(
    isLevel: Boolean
) {
    // 수평이면 초록, 아니면 빨강
    val lineColor = if (isLevel) Color.Green else Color.Red

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {

        val cameraPreviewHeight = size.height * 0.88f
        val centerY = cameraPreviewHeight / 2f

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

// drawable/testimage를 서버로 업로드하는 테스트 함수
private fun uploadTestImage(
    context: Context,
    onResult: (String) -> Unit
) {
    val url = "https://lamprophonic-unclosable-maryellen.ngrok-free.dev/upload"

    try {
        // drawable 안의 testimage 찾기
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

        // 캐시 디렉토리에 임시 파일로 저장 후 업로드
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


        // 네트워크 요청은 백그라운드에서 실행(메인스레드가 아닌 별도 스레드에서 실행)
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

// 촬영한 이미지를 서버로 업로드하는 함수
private fun uploadCapturedImage(
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
private fun getLatestGalleryImageUri(context: Context): Uri? {
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
private fun uriToFile(context: Context, uri: Uri): File? {
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

private fun copyImageToHistoryStorage(context: Context, sourceFile: File): File {
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

private fun saveHistoryItems(
    context: Context,
    items: List<HistoryItem>
) {
    val jsonArray = JSONArray()

    items.forEach { item ->
        val obj = JSONObject().apply {
            put("guideText", item.guideText)

            put("settingsIso", item.settings.iso)
            put("settingsShutter", item.settings.shutter)
            put("settingsAperture", item.settings.aperture)
            put("settingsFocalLength", item.settings.focalLength)
            put("settingsEv", item.settings.ev)
            put("settingsWhiteBalance", item.settings.whiteBalance)

            put("category", item.category)
            put("title", item.title)
            put("date", item.date)
            put("time", item.time)
            put("originalPhotoPath", item.originalPhotoFile?.absolutePath ?: "")
            put("adjustedImageUrl", item.adjustedImageUrl ?: "")

            item.adjustmentInfo?.let {
                put("exposureBefore", it.exposureBefore)
                put("exposureAfter", it.exposureAfter)
                put("isoBefore", it.isoBefore)
                put("isoAfter", it.isoAfter)
                put("wbBefore", it.wbBefore)
                put("wbAfter", it.wbAfter)
            }
        }

        jsonArray.put(obj)
    }

    context.getSharedPreferences("history_prefs", Context.MODE_PRIVATE)
        .edit()
        .putString("history_items", jsonArray.toString())
        .apply()
}

private fun loadHistoryItems(context: Context): List<HistoryItem> {
    val jsonText = context
        .getSharedPreferences("history_prefs", Context.MODE_PRIVATE)
        .getString("history_items", null)
        ?: return emptyList()

    return try {
        val jsonArray = JSONArray(jsonText)
        val result = mutableListOf<HistoryItem>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)

            val originalPath = obj.optString("originalPhotoPath", "")
            val originalFile = if (originalPath.isNotBlank()) File(originalPath) else null

            result.add(
                HistoryItem(
                    category = obj.optString("category", "기타"),
                    title = obj.optString("title", ""),
                    date = obj.optString("date", ""),
                    time = obj.optString("time", ""),
                    originalPhotoFile = originalFile,
                    adjustedImageUrl = obj.optString("adjustedImageUrl", ""),
                    guideText = obj.optString("guideText", ""),
                    settings = RecommendedSettings(
                        iso = obj.optInt("settingsIso", 0),
                        shutter = obj.optString("settingsShutter", ""),
                        aperture = obj.optString("settingsAperture", ""),
                        focalLength = obj.optString("settingsFocalLength", ""),
                        ev = obj.optString("settingsEv", ""),
                        whiteBalance = obj.optString("settingsWhiteBalance", "")
                    ),
                    adjustmentInfo = AdjustmentInfo(
                        exposureBefore = obj.optInt("exposureBefore", 0),
                        exposureAfter = obj.optInt("exposureAfter", 0),
                        isoBefore = obj.optInt("isoBefore", 100),
                        isoAfter = obj.optInt("isoAfter", 0),
                        wbBefore = obj.optString("wbBefore", "AUTO"),
                        wbAfter = obj.optString("wbAfter", "")
                    )
                )
            )
        }

        result
    } catch (e: Exception) {
        emptyList()
    }
}

//// 결과 화면 미리보기
//@androidx.compose.ui.tooling.preview.Preview(showBackground = true, showSystemUi = true)
//@Composable
//fun ResultScreenPreview() {
//
//    val context = LocalContext.current
//
//    val originalImage =
//        painterResource(id = R.drawable.testimage)
//
//    val adjustedImage =
//        painterResource(id = R.drawable.testimage)
//
//    ResultScreen(
//        isUploading = false,
//        guideText = "Scene: 풍경\n밝기가 적절합니다.\n구도가 안정적입니다.",
//        uploadError = "",
//        originalPhotoFile = null,
//        adjustedImageUrl = null,
//        onBackToCamera = {}
//    )
//}

@Composable
fun KlickWebViewScreen() {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.allowContentAccess = true

                webViewClient = WebViewClient()

                loadUrl("file:///android_asset/Klick.html")
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}