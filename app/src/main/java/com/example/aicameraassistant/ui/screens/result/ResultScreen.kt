package com.example.aicameraassistant.ui.screens.result

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import com.example.aicameraassistant.data.local.saveResultImageToGallery
import com.example.aicameraassistant.data.local.readCapturedSettings
import com.example.aicameraassistant.data.model.CapturedSettings
import com.example.aicameraassistant.data.model.RecommendedSettings
import com.example.aicameraassistant.ui.util.sceneToKorean
import java.io.File

@Composable
fun ResultScreen(
    isUploading: Boolean,
    guideText: String,
    uploadError: String,
    originalPhotoFile: File?,
    adjustedPhotoFile: File?,
    adjustedImageUrl: String?,
    scene: String?,
    settings: RecommendedSettings,
    isHistoryDetail: Boolean = false,
    onDelete: (() -> Unit)? = null,
    onBackToCamera: () -> Unit
) {
    val context = LocalContext.current
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showHistoryMenu by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    val adjustedImage = adjustedPhotoFile
        ?: adjustedImageUrl?.takeIf { it.isNotBlank() }
    val hasSavableImage = adjustedImage != null || originalPhotoFile != null
    val sceneKor = sceneToKorean(scene)
    val sceneEng = sceneToEnglish(scene)
    val saveImage = {
        if (!isSaving) {
            isSaving = true
            saveResultImageToGallery(
                context = context,
                adjustedPhotoFile = adjustedPhotoFile,
                adjustedImageUrl = adjustedImageUrl,
                originalPhotoFile = originalPhotoFile
            ) { saved ->
                isSaving = false
                android.widget.Toast.makeText(
                    context,
                    if (saved) "갤러리에 저장했습니다."
                    else "이미지를 저장하지 못했습니다.",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

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
                    if (isHistoryDetail) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onBackToCamera) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "뒤로가기",
                                    tint = Color(0xFF1F2937)
                                )
                            }

                            Text(
                                text = "히스토리",
                                color = Color(0xFF1F2937),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Box {
                                IconButton(onClick = { showHistoryMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "더보기",
                                        tint = Color(0xFF1F2937)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showHistoryMenu,
                                    onDismissRequest = { showHistoryMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("사진 저장") },
                                        enabled = !isSaving && hasSavableImage,
                                        onClick = {
                                            showHistoryMenu = false
                                            saveImage()
                                        }
                                    )
                                    if (onDelete != null) {
                                        DropdownMenuItem(
                                            text = {
                                                Text("기록 삭제", color = Color(0xFFDC2626))
                                            },
                                            onClick = {
                                                showHistoryMenu = false
                                                showDeleteConfirmation = true
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    ResultImageCard(
                        adjustedImageModel = adjustedImage,
                        originalImageModel = originalPhotoFile
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = "CHALKAK 추천",
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
                        fontSize = 18.sp,
                        lineHeight = 27.sp,
                        fontWeight = FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    ResultSettingGrid(
                        settings = settings,
                        originalPhotoFile = originalPhotoFile
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = "촬영 가이드",
                        color = Color(0xFF1F2937),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    ResultGuideGroup()

                    Spacer(modifier = Modifier.height(24.dp))

                    if (!isHistoryDetail) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = onBackToCamera,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(60.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF5B35FF),
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    text = "카메라로 돌아가기",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedIconButton(
                                onClick = saveImage,
                                enabled = !isSaving && hasSavableImage,
                                modifier = Modifier
                                    .size(60.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = IconButtonDefaults.outlinedIconButtonColors(
                                    contentColor = Color(0xFF5B35FF)
                                )
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "사진 저장"
                                    )
                                }
                            }
                        }
                    }

                    if (onDelete != null && !isHistoryDetail) {
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { showDeleteConfirmation = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFDC2626)
                            )
                        ) {
                            Text("히스토리 삭제")
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        if (showDeleteConfirmation && onDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("히스토리 삭제") },
                text = { Text("이 기록과 저장된 이미지를 삭제할까요?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirmation = false
                            onDelete()
                        }
                    ) {
                        Text("삭제", color = Color(0xFFDC2626))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text("취소")
                    }
                }
            )
        }
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
    adjustedImageModel: Any?,
    originalImageModel: Any?
) {
    var showOverlay by remember { mutableStateOf(false) }
    var overlayShowsOriginal by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ComparisonThumbnail(
                model = originalImageModel,
                label = "보정 전",
                modifier = Modifier.weight(1f),
                onClick = {
                    overlayShowsOriginal = true
                    showOverlay = true
                }
            )
            ComparisonThumbnail(
                model = adjustedImageModel,
                label = "보정 후",
                modifier = Modifier.weight(1f),
                onClick = {
                    overlayShowsOriginal = false
                    showOverlay = true
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "사진을 눌러 크게 보기",
            color = Color(0xFF9CA3AF),
            fontSize = 11.sp
        )
    }

    if (showOverlay) {
        Dialog(
            onDismissRequest = { showOverlay = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                val displayedImage = if (overlayShowsOriginal) {
                    originalImageModel
                } else {
                    adjustedImageModel
                }

                if (displayedImage != null) {
                    SubcomposeAsyncImage(
                        model = displayedImage,
                        contentDescription = if (overlayShowsOriginal) "보정 전 이미지" else "보정 후 이미지",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        loading = { ResultImageFallback("이미지를 불러오고 있습니다") },
                        error = { ResultImageFallback("이미지를 불러올 수 없습니다") }
                    )
                } else {
                    ResultImageFallback("이미지가 없습니다")
                }

                IconButton(
                    onClick = { showOverlay = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, "닫기", tint = Color.White)
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 20.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ImageModeButton("보정 전", overlayShowsOriginal) {
                        overlayShowsOriginal = true
                    }
                    ImageModeButton("보정 후", !overlayShowsOriginal) {
                        overlayShowsOriginal = false
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonThumbnail(
    model: Any?,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.82f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFE5E7EB))
                .clickable(onClick = onClick)
        ) {
            if (model != null) {
                SubcomposeAsyncImage(
                    model = model,
                    contentDescription = "$label 이미지",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = { ResultImageFallback("불러오는 중") },
                    error = { ResultImageFallback("이미지 없음") }
                )
            } else {
                ResultImageFallback("이미지 없음")
            }
        }
        Spacer(modifier = Modifier.height(7.dp))
        Text(
            text = label,
            color = Color(0xFF6B7280),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ImageModeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = text,
        color = if (selected) Color.White else Color.White.copy(alpha = 0.65f),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) Color(0xFF5B35FF) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

@Composable
private fun ResultImageFallback(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE5E7EB)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color(0xFF6B7280),
            fontSize = 13.sp
        )
    }
}

@Composable
fun ResultSettingGrid(
    settings: RecommendedSettings,
    originalPhotoFile: File?
) {
    var showCapturedSettings by remember { mutableStateOf(false) }
    val capturedSettings = remember(originalPhotoFile?.absolutePath) {
        readCapturedSettings(originalPhotoFile)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(modifier = Modifier.padding(top = 14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingModeButton("추천 설정", !showCapturedSettings) {
                    showCapturedSettings = false
                }
                SettingModeButton("촬영 정보", showCapturedSettings) {
                    showCapturedSettings = true
                }
            }

            if (showCapturedSettings && !capturedSettings.hasAnyValue) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(185.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "이 사진에는 촬영 정보가 없습니다",
                        color = Color(0xFF6B7280),
                        fontSize = 14.sp
                    )
                }
                return@Column
            }

            val displayedSettings = if (showCapturedSettings) {
                capturedSettings
            } else {
                settings.asCapturedSettings()
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                ResultSettingCell(
                    "↔ ISO",
                    displayedSettings.iso.orDash(),
                    Modifier.weight(1f)
                )

                ResultSettingCell(
                    "◉ 셔터",
                    displayedSettings.shutter.orDash(),
                    Modifier.weight(1f)
                )

                ResultSettingCell(
                    "◉ 조리개",
                    displayedSettings.aperture.orDash(),
                    Modifier.weight(1f)
                )
            }

            Divider(color = Color(0xFFE5E7EB))

            Row(modifier = Modifier.fillMaxWidth()) {
                ResultSettingCell(
                    "☼ WB",
                    displayedSettings.whiteBalance.orDash(),
                    Modifier.weight(1f)
                )

                ResultSettingCell(
                    "↯ EV",
                    displayedSettings.ev.orDash(),
                    Modifier.weight(1f)
                )

                ResultSettingCell(
                    "✣ 초점거리",
                    displayedSettings.focalLength.orDash(),
                    Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SettingModeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = text,
        color = if (selected) Color.White else Color(0xFF6B7280),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) Color(0xFF5B35FF) else Color(0xFFEEF0F6))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp)
    )
}

private fun RecommendedSettings.asCapturedSettings() = CapturedSettings(
    iso = iso.takeIf { it > 0 }?.toString(),
    shutter = shutter.takeIf { it.isNotBlank() },
    aperture = aperture.takeIf { it.isNotBlank() },
    focalLength = focalLength.takeIf { it.isNotBlank() },
    ev = ev.takeIf { it.isNotBlank() },
    whiteBalance = whiteBalance.takeIf { it.isNotBlank() }
)

private fun String?.orDash(): String = this?.takeIf { it.isNotBlank() } ?: "-"

@Composable
fun ResultSettingCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(92.dp)
            .padding(horizontal = 10.dp, vertical = 14.dp),
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
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ResultGuideGroup() {
    val guides = listOf(
        Triple("01", "심도", "피사체에 초점을 맞추고 배경은 자연스럽게 분리"),
        Triple("02", "광원", "45° 측면광을 활용해 입체감 확보"),
        Triple("03", "구도", "주 피사체를 그리드 교차점 근처에 배치")
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            guides.forEachIndexed { index, (number, title, description) ->
                Row(
                    modifier = Modifier.padding(vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = number,
                        color = Color(0xFF6D28D9),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(30.dp)
                    )
                    Column {
                        Text(
                            text = title,
                            color = Color(0xFF1F2937),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = description,
                            color = Color(0xFF6B7280),
                            fontSize = 13.sp
                        )
                    }
                }

                if (index < guides.lastIndex) {
                    Divider(color = Color(0xFFE5E7EB))
                }
            }
        }
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
