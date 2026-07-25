package com.example.aicameraassistant.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.aicameraassistant.R
import com.example.aicameraassistant.data.model.HistoryItem
import com.example.aicameraassistant.data.model.WeatherTip
import com.example.aicameraassistant.data.model.WeatherType

// 발표/시연용 날씨입니다. SUNNY, CLOUDY, RAIN, SNOW 중 하나로 변경할 수 있습니다.
private val DEMO_WEATHER_TYPE = WeatherType.SUNNY
private val HomePrimary = Color(0xFF5B35FF)
private val HomeBrandNavy = Color(0xFF1C1E53)
private val HomeBackground = Color(0xFFF7F8FC)
private val HomeTextPrimary = Color(0xFF1F2937)
private val HomeTextSecondary = Color(0xFF6B7280)

// 홈 화면 UI
@Composable
fun HomeScreen(
    historyItems: List<HistoryItem>,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onViewAllClick: () -> Unit,
    onHistoryItemClick: (HistoryItem) -> Unit
) {
    val weatherTip = getWeatherTip(DEMO_WEATHER_TYPE)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.chalkak_logo),
                contentDescription = "CHALKAK 앱 아이콘",
                modifier = Modifier.size(28.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "CHALKAK",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = HomeBrandNavy
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = HomeBrandNavy
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                Text(
                    text = "장면을 분석해\n알맞은 촬영 설정을 추천합니다",
                    color = Color.White,
                    fontSize = 23.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "음식·풍경·야경·명암·인물 지원",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onCameraClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HomePrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(10.dp),
                        modifier = Modifier
                            .weight(1.35f)
                            .fillMaxHeight()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(38.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "카메라 →",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onGalleryClick,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "갤러리 선택",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
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
                color = HomeTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "전체보기",
                color = HomePrimary,
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
                    text = "아직 분석한 사진이 없어요\n첫 사진을 촬영해보세요",
                    color = HomeTextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 21.sp
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

        SupportScenesSection()

        Spacer(modifier = Modifier.height(132.dp))
        }

        FloatingWeatherTipCard(
            weatherTip = weatherTip,
            weatherType = DEMO_WEATHER_TYPE,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp)
        )
    }
}

@Composable
private fun SupportScenesSection() {
    Text(
        text = "지원 장면",
        color = HomeTextPrimary,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold
    )

    Spacer(modifier = Modifier.height(16.dp))

    val supportScenes = listOf(
        "음식" to R.drawable.food,
        "풍경" to R.drawable.landscape,
        "야경" to R.drawable.night,
        "명암" to R.drawable.contrast,
        "인물" to R.drawable.portrait
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        supportScenes.forEach { (title, imageRes) ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(0.72f)
                    .clip(RoundedCornerShape(16.dp))
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

                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(7.dp)
                )
            }
        }
    }
}

@Composable
private fun FloatingWeatherTipCard(
    weatherTip: WeatherTip,
    weatherType: WeatherType,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .widthIn(max = 280.dp)
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        if (isExpanded) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFFFD36E), Color(0xFFFF7AD9))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = weatherIcon(weatherType),
                        color = Color.White,
                        fontSize = 21.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = weatherTip.weatherText,
                        color = HomeTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = weatherTip.tipText,
                        color = HomeTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .width(82.dp)
                    .padding(horizontal = 8.dp, vertical = 11.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "오늘의 촬영 팁 열기",
                    tint = HomePrimary,
                    modifier = Modifier.size(30.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "오늘의 촬영 팁",
                    color = HomeTextSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

private fun weatherIcon(weatherType: WeatherType): String = when (weatherType) {
    WeatherType.SUNNY -> "☀"
    WeatherType.CLOUDY -> "☁"
    WeatherType.RAIN -> "☂"
    WeatherType.SNOW -> "❄"
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
            containerColor = HomeBrandNavy
        ),
        shape = RoundedCornerShape(16.dp)
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
            .background(HomeBrandNavy),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp
        )
    }
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
            containerColor = HomeBrandNavy
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
                color = HomeTextSecondary,
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
