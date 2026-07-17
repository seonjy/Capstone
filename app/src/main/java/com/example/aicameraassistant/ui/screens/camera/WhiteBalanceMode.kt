package com.example.aicameraassistant.ui.screens.camera

import android.hardware.camera2.CaptureRequest

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
