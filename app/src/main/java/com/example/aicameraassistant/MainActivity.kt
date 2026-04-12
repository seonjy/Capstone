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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

import coil.compose.AsyncImage
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 앱 시작 시 현재 카메라 권한 확인
        permissionGrantedState = hasCameraPermission(this)

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
                            onSuccess = { responseText, imageUrl, scene, iso, shutter ->

                                isUploading = false
                                guideText = "Scene: $scene\n$responseText"
                                adjustedImageUrl = imageUrl

                                val category = if (!scene.isNullOrBlank()) scene else "기타"

                                val newItem = HistoryItem(
                                    category = category,
                                    title = selectedFile.name,
                                    date = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(Date()),
                                    originalPhotoFile = selectedFile,
                                    adjustedImageUrl = imageUrl,
                                    adjustmentInfo = AdjustmentInfo(
                                        exposureBefore = 0,
                                        exposureAfter = 0, // 셔터는 나중에 변환
                                        isoBefore = 100,
                                        isoAfter = iso,
                                        wbBefore = "AUTO",
                                        wbAfter = "DAYLIGHT"
                                    )
                                )

                                historyItems = listOf(newItem) + historyItems
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
                        }
                    )

                    AppScreen.RESULT -> {
                        ResultScreen(
                            isUploading = isUploading,
                            guideText = guideText,
                            uploadError = uploadError,
                            originalPhotoFile = capturedPhotoFile,
                            adjustedImageUrl = adjustedImageUrl,
                            onBackToCamera = {
                                currentScreen = AppScreen.MENU
                                isUploading = false
                                guideText = ""
                                uploadError = ""
                                adjustedImageUrl = null
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

// 시작 화면 UI

@Composable
fun StartMenuScreen(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    historyItems: List<HistoryItem>
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 배경 이미지
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = "배경 이미지",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 96.dp)
        ) {

            // 🔥 상단 텍스트
            Text(
                text = "카메라 어시스트",
                fontSize = 28.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.weight(1f)) // 위/아래 공간 분리

            // 🔽 버튼 영역
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Button(
                    onClick = onCameraClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xCC87CEEB),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "사진 촬영",
                        fontSize = 20.sp
                    )
                }

                Button(
                    onClick = onGalleryClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xCC87CEEB),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "갤러리",
                        fontSize = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

sealed class BottomTab(
    val label: String,
    val icon: ImageVector
) {
    object Start : BottomTab("시작", Icons.Default.Home)
    object History : BottomTab("히스토리", Icons.Default.List)
}

@Composable
fun HistoryScreen(
    historyItems: List<HistoryItem>
) {
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedItem by remember { mutableStateOf<HistoryItem?>(null) }

    val allCategories = historyItems.map { it.category }.distinct()
    val groupedItems = historyItems.groupBy { it.category }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 🔥 배경 이미지
        Image(
            painter = painterResource(id = R.drawable.background1),
            contentDescription = "배경 이미지",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 🔽 기존 화면들
        when {
            selectedItem != null -> {
                HistoryDetailScreen(
                    item = selectedItem!!,
                    onBack = {
                        selectedItem = null
                    }
                )
            }

            selectedCategory != null -> {
                HistoryCategoryScreen(
                    category = selectedCategory!!,
                    itemsList = groupedItems[selectedCategory!!] ?: emptyList(),
                    onBack = {
                        selectedCategory = null
                    },
                    onItemClick = { clickedItem ->
                        selectedItem = clickedItem
                    }
                )
            }

            else -> {
                HistoryListScreen(
                    categories = allCategories,
                    groupedItems = groupedItems,
                    onCategoryClick = { clickedCategory ->
                        selectedCategory = clickedCategory
                    },
                    onItemClick = { clickedItem ->
                        selectedItem = clickedItem
                    }
                )
            }
        }
    }
}

data class HistoryItem(
    val category: String,
    val title: String,
    val date: String,
    val originalPhotoFile: File?,
    val adjustedImageUrl: String?,
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
@Composable
fun HistoryCategoryScreen(
    category: String,
    itemsList: List<HistoryItem>,
    onBack: () -> Unit,
    onItemClick: (HistoryItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "뒤로가기"
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = category,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (itemsList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "이 카테고리에 저장된 사진이 없습니다",
                    color = Color.Gray
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(itemsList) { item ->
                    Column(
                        modifier = Modifier
                            .clickable {
                                onItemClick(item)
                            }
                    ) {
                        if (item.originalPhotoFile != null) {
                            AsyncImage(
                                model = item.originalPhotoFile,
                                contentDescription = item.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .background(Color(0xFFEAF6FD)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
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

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = item.title,
                            fontSize = 11.sp,
                            maxLines = 1,
                            color = Color.DarkGray
                        )
                    }
                }
            }
        }
    }
}

// 상세 화면 추가
@Composable
fun HistoryDetailScreen(
    item: HistoryItem,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "뒤로가기"
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "상세 보기",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = item.date,
            fontSize = 16.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "보정 전",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (item.originalPhotoFile != null) {
            AsyncImage(
                model = item.originalPhotoFile,
                contentDescription = "보정 전 사진",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Text("원본 이미지가 없습니다")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "보정 후",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (!item.adjustedImageUrl.isNullOrBlank()) {
            AsyncImage(
                model = item.adjustedImageUrl,
                contentDescription = "보정 후 사진",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Text("보정 후 이미지가 없습니다")
            }
        }


        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "보정 정보",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item.adjustmentInfo?.let {
                Text("노출: ${it.exposureBefore} → ${it.exposureAfter}")
                Text("ISO: ${it.isoBefore} → ${it.isoAfter}")
                Text("화이트밸런스: ${it.wbBefore} → ${it.wbAfter}")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}


@Composable
fun MainTabScreen(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    historyItems: List<HistoryItem>
) {
    var selectedTab by remember { mutableStateOf<BottomTab>(BottomTab.Start) }

    val tabs = listOf(
        BottomTab.Start,
        BottomTab.History
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White
            ) {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF5DADE2),
                            selectedTextColor = Color(0xFF5DADE2),
                            indicatorColor = Color(0xFFD6EFFF),
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
                is BottomTab.Start -> StartMenuScreen(
                    onCameraClick = onCameraClick,
                    onGalleryClick = onGalleryClick,
                    historyItems = historyItems
                )
                is BottomTab.History -> HistoryScreen(historyItems = historyItems)
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

    // 결과 화면 표시 여부
    var showResultScreen by remember { mutableStateOf(false) }

    // 업로드 / 결과 관련 상태값
    var isUploading by remember { mutableStateOf(false) }
    var guideText by remember { mutableStateOf("") }
    var uploadError by remember { mutableStateOf("") }
    var capturedPhotoFile by remember { mutableStateOf<File?>(null) }
    var adjustedImageUrl by remember { mutableStateOf<String?>(null) }

    // CameraX 카메라 객체 및 노출값 상태
    var camera by remember { mutableStateOf<Camera?>(null) }
    var exposureIndex by remember { mutableStateOf(0f) }
    var exposureRange by remember { mutableStateOf(0..0) }

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

    // 프리뷰 화면 밝기 분석
    LaunchedEffect(imageAnalysis) {
        imageAnalysis.setAnalyzer(
            ContextCompat.getMainExecutor(context)
        ) { image ->
            val brightness = calculateBrightness(image)
            isDark = brightness < 90
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
            onBackToCamera = {
                showResultScreen = false
                isUploading = false
                guideText = ""
                uploadError = ""
                adjustedImageUrl = null
            }
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // 위쪽: 카메라 프리뷰 영역
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { previewView }
            )

/*
            // 상태 텍스트
            Text(
                text = "업로드 결과: $resultText",
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 50.dp)
            )
*/

            // 가이드선
            GuideOverlay(isLevel = isLevel)

            // 뒤로가기 버튼
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = Color.White
                )
            }

            // 화이트밸런스 버튼
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .fillMaxWidth(0.9f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                WhiteBalanceMode.entries.forEach { mode ->
                    Button(
                        onClick = { wbMode = mode },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (wbMode == mode) Color.White else Color.DarkGray,
                            contentColor = if (wbMode == mode) Color.Black else Color.White
                        )
                    ) {
                        Text(
                            text = mode.label,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            // 밝기 경고
            if (isDark) {
                Text(
                    text = "너무 어두워요",
                    color = Color.Yellow,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 40.dp)
                )
            }
        }

        // 아래쪽: 검은 배경 컨트롤 영역
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color.Black)
        ) {
            // 노출값 조절 UI
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
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
                                    onSuccess = { responseText, imageUrl, scene, iso, shutter ->
                                        android.util.Log.d("UPLOAD", "responseText = $responseText")
                                        android.util.Log.d("UPLOAD", "imageUrl = $imageUrl")
                                        android.util.Log.d("UPLOAD", "scene = $scene")

                                        isUploading = false
                                        guideText = "Scene: $scene\n$responseText"
                                        adjustedImageUrl = imageUrl

                                        val category = if (!scene.isNullOrBlank()) scene else "기타"

                                        val newItem = HistoryItem(
                                            category = category,
                                            title = photoFile.name,
                                            date = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(Date()),
                                            originalPhotoFile = photoFile,
                                            adjustedImageUrl = imageUrl,
                                            adjustmentInfo = AdjustmentInfo(
                                                exposureBefore = 0,
                                                exposureAfter = 0, // 셔터는 나중에 변환
                                                isoBefore = 100,
                                                isoAfter = iso,
                                                wbBefore = "AUTO",
                                                wbAfter = "DAYLIGHT"
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
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 50.dp)
                    .size(72.dp)
                    .clip(CircleShape),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                )
            ) {
                Text("")
            }
        }
    }
}

@Composable
fun ResultScreen(
    isUploading: Boolean,
    guideText: String,
    uploadError: String,
    originalPhotoFile: File?,
    adjustedImageUrl: String?,
    onBackToCamera: () -> Unit
) {
    var showAnalysisOverlay by remember { mutableStateOf(false) }

    // 분석 결과 화면
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(36.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
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
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {

                            // 원본 이미지 Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFAFAFAFF))
                                    .padding(1.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("원본 이미지")

                                    // 실제 사진 - 서버 연결용
                                    originalPhotoFile?.let { file ->
                                        AsyncImage(
                                            model = file,
                                            contentDescription = "원본 이미지",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(300.dp)
                                        )
                                    }

// 임의 사진 - 화면 미리보기용
//                                Image(
//                                    painter = painterResource(id = R.drawable.testimage),
//                                    contentDescription = "원본 이미지",
//                                    modifier = Modifier
//                                        .fillMaxWidth()
//                                        .height(200.dp),
//                                    contentScale = ContentScale.Crop
//                                )
                                }
                            }

                            // 보정 이미지 Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFAFAFAFF))
                                    .padding(1.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("보정 이미지")

                                    // 실제 사진 - 서버 연결용
                                    if (!adjustedImageUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = adjustedImageUrl,
                                            contentDescription = "보정된 이미지",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(300.dp)
                                        )
                                    } else {
                                        Text("보정 이미지 없음")
                                    }

// 임의 사진 - 화면 미리보기용
//                                Image(
//                                    painter = painterResource(id = R.drawable.testimage),
//                                    contentDescription = "보정 이미지",
//                                    modifier = Modifier
//                                        .fillMaxWidth()
//                                        .height(200.dp),
//                                    contentScale = ContentScale.Crop
//                                )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 분석 결과 버튼
                        Button(
                            onClick = { showAnalysisOverlay = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF87CEEB),
                                contentColor = Color.White
                            )
                        ) {
                            Text("분석 결과")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onBackToCamera,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF87CEEB),
                    contentColor = Color.White
                )
            ) {
                Text("처음으로 돌아가기")
            }
        }

        if (showAnalysisOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .background(Color.White)
                        .padding(20.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "X",
                                modifier = Modifier.clickable {
                                    showAnalysisOverlay = false
                                },
                                fontSize = 20.sp
                            )
                        }

                        Text(
                            text = guideText,
                            color = Color.Black
                        )
                    }
                }
            }
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

@Composable
fun GuideOverlay(
    isLevel: Boolean
) {
    // 수평이면 초록, 아니면 빨강
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
    onSuccess: (String, String?, String?, Int, String) -> Unit,
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
                    var iso = 0
                    var shutter = ""

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
                        iso = recommendation.optInt("recommended_iso", 0)
                        shutter = recommendation.optString("recommended_shutter", "").trim()

                        android.util.Log.d("UPLOAD", "recommended iso = $iso")
                        android.util.Log.d("UPLOAD", "recommended shutter = $shutter")
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

                    onSuccess(messageText, imageUrl, sceneText, iso, shutter)

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

// 결과 화면 미리보기
@androidx.compose.ui.tooling.preview.Preview(showBackground = true, showSystemUi = true)
@Composable
fun ResultScreenPreview() {

    val context = LocalContext.current

    val originalImage =
        painterResource(id = R.drawable.testimage)

    val adjustedImage =
        painterResource(id = R.drawable.testimage)

    ResultScreen(
        isUploading = false,
        guideText = "Scene: 풍경\n밝기가 적절합니다.\n구도가 안정적입니다.",
        uploadError = "",
        originalPhotoFile = null,
        adjustedImageUrl = null,
        onBackToCamera = {}
    )
}