package com.example.aicameraassistant.ui.screens.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.aicameraassistant.R
import com.example.aicameraassistant.ui.screens.result.ResultScreen
import com.example.aicameraassistant.data.model.HistoryItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    historyItems: List<HistoryItem>,
    onDeleteHistoryItem: (HistoryItem) -> Unit,
    onDetailVisibilityChange: (Boolean) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("전체") }
    var selectedItem by remember { mutableStateOf<HistoryItem?>(null) }

    val filters = listOf("전체", "음식", "풍경", "야경", "명암", "인물")

    val filteredItems = historyItems
        .filter { selectedFilter == "전체" || it.category == selectedFilter }
        .sortedByDescending { it.createdAt }

    val groupedItems = filteredItems.groupBy { it.date }

    if (selectedItem != null) {
        BackHandler {
            selectedItem = null
            onDetailVisibilityChange(false)
        }

        HistoryDetailScreen(
            item = selectedItem!!,
            onDelete = {
                selectedItem?.let(onDeleteHistoryItem)
                selectedItem = null
                onDetailVisibilityChange(false)
            },
            onBack = {
                selectedItem = null
                onDetailVisibilityChange(false)
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FC))
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.chalkak_logo),
                contentDescription = "CHALKAK 앱 아이콘",
                modifier = Modifier.size(24.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.width(7.dp))

            Text(
                text = "CHALKAK",
                color = Color(0xFF1C1E53),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFEEF0F6))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            filters.forEach { filter ->
                HistoryFilterChip(
                    text = filter,
                    selected = selectedFilter == filter,
                    count = if (filter == "전체") historyItems.size else null,
                    onClick = { selectedFilter = filter },
                    modifier = Modifier.weight(if (filter == "전체") 1.35f else 1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (selectedFilter == "전체") {
                        "아직 촬영 기록이 없어요\n사진을 분석하면 이곳에 기록이 쌓여요"
                    } else {
                        "아직 $selectedFilter 촬영 기록이 없어요"
                    },
                    color = Color(0xFF6B7280),
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(28.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = groupedItems.entries.toList(),
                    key = { it.key }
                ) { (date, itemsForDate) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatHistoryDateLabel(date),
                            fontSize = 16.sp,
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

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        itemsForDate.chunked(3).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowItems.forEach { item ->
                                    HistoryPhotoCard(
                                        item = item,
                                        onClick = {
                                            selectedItem = item
                                            onDetailVisibilityChange(true)
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                repeat(3 - rowItems.size) {
                                    Spacer(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
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

@Composable
fun HistoryFilterChip(
    text: String,
    selected: Boolean,
    count: Int? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.height(38.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (count != null) "$text $count" else text,
                color = if (selected) Color(0xFF1F2937) else Color(0xFF6B7280),
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth(0.62f)
                .height(2.dp)
                .background(if (selected) Color(0xFF1C1E53) else Color.Transparent)
        )
    }
}

@Composable
fun HistoryPhotoCard(
    item: HistoryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE5E7EB))
            .clickable { onClick() }
    ) {
        val originalFile = item.originalPhotoFile

        if (originalFile != null && originalFile.exists()) {
            SubcomposeAsyncImage(
                model = originalFile,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = { HistoryImageFallback("불러오는 중") },
                error = { HistoryImageFallback("이미지 없음") }
            )
        } else {
            HistoryImageFallback("이미지 없음")
        }

    }
}

@Composable
private fun HistoryImageFallback(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE5E7EB)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color(0xFF6B7280),
            fontSize = 11.sp
        )
    }
}

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
                                        SubcomposeAsyncImage(
                                            model = historyItem.originalPhotoFile,
                                            contentDescription = historyItem.title,
                                            modifier = Modifier
                                                .size(120.dp)
                                                .background(Color(0xFFEAF6FD)),
                                            contentScale = ContentScale.Crop,
                                            error = { HistoryImageFallback("이미지 없음") }
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

@Composable
fun HistoryDetailScreen(
    item: HistoryItem,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    ResultScreen(
        isUploading = false,
        guideText = item.guideText,
        uploadError = "",
        originalPhotoFile = item.originalPhotoFile,
        adjustedPhotoFile = item.adjustedPhotoFile,
        adjustedImageUrl = item.adjustedImageUrl,
        scene = item.category,
        settings = item.settings,
        isHistoryDetail = true,
        onDelete = onDelete,
        onBackToCamera = onBack
    )
}
