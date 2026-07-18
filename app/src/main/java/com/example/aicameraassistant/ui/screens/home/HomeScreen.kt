package com.example.aicameraassistant.ui.screens.home

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.aicameraassistant.R
import com.example.aicameraassistant.data.model.HistoryItem
import com.example.aicameraassistant.data.model.WeatherTip
import com.example.aicameraassistant.data.model.WeatherType
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

private const val WEATHER_API_KEY =  "8767d3ec1b1f549c8f473c665dd5b2f2"

// 디버그 화면 테스트 시에만 WeatherType 값을 지정합니다.
// null이면 실제 위치 기반 날씨를 사용하며, release 빌드에는 적용되지 않습니다.
private val DEBUG_WEATHER_OVERRIDE: WeatherType? = null

// 홈 화면 UI
@Composable
fun HomeScreen(
    historyItems: List<HistoryItem>,
    locationPermissionGranted: Boolean,
    shouldRequestLocationPermission: Boolean,
    onRequestLocationPermission: () -> Unit,
    onCameraClick: () -> Unit,
    onViewAllClick: () -> Unit,
    onHistoryItemClick: (HistoryItem) -> Unit
) {
    val context = LocalContext.current
    val isDebugBuild = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    var weatherType by remember {
        mutableStateOf(
            if (isDebugBuild) DEBUG_WEATHER_OVERRIDE ?: WeatherType.SUNNY
            else WeatherType.SUNNY
        )
    }
    val weatherTip = getWeatherTip(weatherType)

    LaunchedEffect(locationPermissionGranted, shouldRequestLocationPermission) {
        if (!locationPermissionGranted) {
            if (shouldRequestLocationPermission) {
                onRequestLocationPermission()
            }
            return@LaunchedEffect
        }

        val fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(context)

        try {

            fun fetchWeather(latitude: Double, longitude: Double) {
                fetchCurrentWeatherType(
                    latitude = latitude,
                    longitude = longitude,
                    apiKey = WEATHER_API_KEY
                ) { result ->
                    weatherType = if (isDebugBuild) {
                        DEBUG_WEATHER_OVERRIDE ?: result
                    } else {
                        result
                    }
                }
            }

            fusedLocationClient.lastLocation
                .addOnSuccessListener { lastLocation ->
                    if (lastLocation != null) {
                        fetchWeather(lastLocation.latitude, lastLocation.longitude)
                    } else {
                        val cancellationTokenSource = CancellationTokenSource()

                        fusedLocationClient.getCurrentLocation(
                            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                            cancellationTokenSource.token
                        ).addOnSuccessListener { currentLocation ->
                            if (currentLocation != null) {
                                fetchWeather(
                                    currentLocation.latitude,
                                    currentLocation.longitude
                                )
                            }
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

        if (historyItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "최근 촬영 기록이 없습니다",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        } else {
            val recentItems = historyItems.take(3)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                repeat(3) { index ->
                    if (index < recentItems.size) {
                        RecentHistoryCard(
                            item = recentItems[index],
                            onClick = { onHistoryItemClick(recentItems[index]) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
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

@Composable
private fun RecentHistoryCard(
    item: HistoryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1F2937)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        val imageFile = item.originalPhotoFile

        if (imageFile != null && imageFile.exists()) {
            SubcomposeAsyncImage(
                model = imageFile,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = { RecentHistoryImageFallback("불러오는 중") },
                error = { RecentHistoryImageFallback("이미지 없음") }
            )
        } else {
            RecentHistoryImageFallback("이미지 없음")
        }
    }
}

@Composable
private fun RecentHistoryImageFallback(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1F2937)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp
        )
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
