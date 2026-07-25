package com.example.aicameraassistant.ui.screens.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

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
            text = "CHALKAK",
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
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = scene,
            color = Color(0xFF1F2937),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        if (scene != "분석 실패" && accuracy.isNotBlank() && accuracy != "--") {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = accuracy,
                color = Color(0xFFE056FD),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AiGuideCard(
    guide: String,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(guide) {
        if (guide.isNotBlank()) {
            isExpanded = true
            delay(3000L)
            isExpanded = false
        }
    }

    if (isExpanded) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.94f))
                .clickable { isExpanded = false }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "✦", color = Color(0xFF6D28D9), fontSize = 19.sp)
            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = "AI 촬영 팁",
                    color = Color(0xFF6D28D9),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = guide,
                    color = Color(0xFF1F2937),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    } else {
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.92f))
                .clickable { isExpanded = true }
                .padding(horizontal = 13.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "✦", color = Color(0xFF6D28D9), fontSize = 15.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "AI 촬영 팁",
                color = Color(0xFF1F2937),
                fontSize = 12.sp,
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
    zoom: String,
    selectedSetting: String?,
    modifier: Modifier = Modifier,
//    onIsoClick: () -> Unit,
//    onShutterClick: () -> Unit,
//    onApertureClick: () -> Unit,
    onWbClick: () -> Unit,
    onEvClick: () -> Unit,
    onZoomClick: () -> Unit
) {
    Row(
        modifier = modifier
            .wrapContentWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
//        CameraSettingItem("ISO", iso, onIsoClick)
//        CameraSettingItem("SS", shutter, onShutterClick)
//        CameraSettingItem("F", aperture, onApertureClick)
        CameraSettingItem("WB", wb, selectedSetting == "WB", onWbClick)
        CameraSettingItem("EV", ev, selectedSetting == "EV", onEvClick)
        CameraSettingItem("ZOOM", zoom, selectedSetting == "ZOOM", onZoomClick)
    }
}

@Composable
fun CameraSettingItem(
    label: String,
    value: String,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isSelected) Color(0xFF6D28D9)
                else Color(0xFF263238).copy(alpha = 0.82f)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = value,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ZoomInlineBar(
    zoomRatio: Float,
    zoomRange: ClosedFloatingPointRange<Float>,
    onZoomChange: (Float) -> Unit,
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
                .clickable(onClick = onClose)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = String.format("%.1fx", zoomRatio),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(10.dp))
        Slider(
            value = zoomRatio.coerceIn(zoomRange.start, zoomRange.endInclusive),
            onValueChange = onZoomChange,
            valueRange = zoomRange,
            modifier = Modifier.weight(1f)
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
fun GuideOverlay(
    isLevel: Boolean
) {
    // 수평이면 초록, 아니면 빨강
    val lineColor = if (isLevel) Color.Green else Color.Red

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {

        val centerY = size.height / 2f
        val lineLength = size.width * 0.36f
        val startX = (size.width - lineLength) / 2f
        val endX = startX + lineLength

        // 항상 가로 수평선
        drawLine(
            color = lineColor,
            start = Offset(startX, centerY),
            end = Offset(endX, centerY),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
    }
}
