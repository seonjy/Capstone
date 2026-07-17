package com.example.aicameraassistant.ui.screens.history

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
