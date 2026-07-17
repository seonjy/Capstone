package com.example.aicameraassistant.data.model

enum class WeatherType {
    SUNNY,
    CLOUDY,
    RAIN,
    SNOW
}

data class WeatherTip(
    val weatherText: String,
    val tipText: String
)
