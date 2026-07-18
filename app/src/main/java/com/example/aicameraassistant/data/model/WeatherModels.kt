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

sealed interface WeatherUiState {
    data object Loading : WeatherUiState

    data class Success(
        val weatherType: WeatherType
    ) : WeatherUiState

    data class Error(
        val message: String
    ) : WeatherUiState
}
