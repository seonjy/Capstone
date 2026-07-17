package com.example.aicameraassistant.ui.util

fun sceneToKorean(scene: String?): String {
    return when (scene?.lowercase()) {
        "food", "음식" -> "음식"
        "landscape", "풍경" -> "풍경"
        "night", "야경" -> "야경"
        "contrast", "명암" -> "명암"
        "portrait", "person", "인물" -> "인물"
        else -> "기타"
    }
}

fun getAiGuideByScene(scene: String?): String {
    return when (scene) {
        "음식" -> "45° 측면, 접시는 그리드 교차점에 두기"
        "풍경" -> "수평선을 맞추고 하늘과 지면 비율 조절하기"
        "야경" -> "흔들림을 줄이고 밝은 광원을 피하기"
        "명암" -> "빛과 그림자의 경계를 살려 촬영하기"
        "인물" -> "얼굴에 빛이 고르게 닿도록 위치 조정하기"
        else -> "장면을 감지하면 촬영 가이드를 알려드려요"
    }
}
