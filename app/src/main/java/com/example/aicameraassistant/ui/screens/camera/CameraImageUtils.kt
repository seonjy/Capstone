package com.example.aicameraassistant.ui.screens.camera

import androidx.camera.core.ImageProxy

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
