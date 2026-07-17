package com.example.aicameraassistant.ui.screens.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.aicameraassistant.data.model.RecommendedSettings
import com.example.aicameraassistant.ui.util.sceneToKorean
import java.io.File

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
