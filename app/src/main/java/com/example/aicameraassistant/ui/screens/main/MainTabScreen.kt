package com.example.aicameraassistant.ui.screens.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.aicameraassistant.ui.screens.home.HomeScreen
import com.example.aicameraassistant.data.model.HistoryItem
import com.example.aicameraassistant.ui.screens.history.HistoryDetailScreen
import com.example.aicameraassistant.ui.screens.history.HistoryScreen

sealed class BottomTab(
    val label: String,
    val icon: ImageVector
) {
    object Home : BottomTab("홈", Icons.Default.Home)
    object Camera : BottomTab("카메라", Icons.Default.PhotoCamera)
    object History : BottomTab("히스토리", Icons.Default.CalendarMonth)
}

@Composable
fun MainTabScreen(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    historyItems: List<HistoryItem>,
    onDeleteHistoryItem: (HistoryItem) -> Unit
) {
    var selectedTab by remember {
        mutableStateOf<BottomTab>(BottomTab.Home)
    }
    var selectedHomeHistoryItem by remember {
        mutableStateOf<HistoryItem?>(null)
    }
    var isHistoryDetailVisible by remember { mutableStateOf(false) }

    val tabs = listOf(
        BottomTab.Home,
        BottomTab.Camera,
        BottomTab.History
    )

    val returnToHistory = {
        selectedHomeHistoryItem = null
        selectedTab = BottomTab.History
    }

    BackHandler(
        enabled = !isHistoryDetailVisible &&
            (selectedHomeHistoryItem != null || selectedTab != BottomTab.Home)
    ) {
        if (selectedHomeHistoryItem != null) {
            returnToHistory()
        } else {
            selectedTab = BottomTab.Home
        }
    }

    Scaffold(
        containerColor = Color(0xFFFFFFFF),

        bottomBar = {
            if (selectedHomeHistoryItem == null && !isHistoryDetailVisible) {
                NavigationBar(
                    containerColor = Color(0xFFFFFFFF)
                ) {
                    tabs.forEach { tab ->

                    NavigationBarItem(
                        selected = selectedTab == tab,

                        onClick = {
                            selectedHomeHistoryItem = null
                            isHistoryDetailVisible = false

                            when (tab) {
                                BottomTab.Camera -> {
                                    onCameraClick()
                                }

                                BottomTab.Home,
                                BottomTab.History -> {
                                    selectedTab = tab
                                }
                            }
                        },

                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label
                            )
                        },

                        label = {
                            Text(tab.label)
                        },

                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF5B35FF),
                            selectedTextColor = Color(0xFF5B35FF),
                            indicatorColor = Color(0xFFEDE9FE),
                            unselectedIconColor = Color(0xFF6B7280),
                            unselectedTextColor = Color(0xFF6B7280)
                        )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            val homeHistoryItem = selectedHomeHistoryItem

            if (homeHistoryItem != null) {
                HistoryDetailScreen(
                    item = homeHistoryItem,
                    onDelete = {
                        onDeleteHistoryItem(homeHistoryItem)
                        returnToHistory()
                    },
                    onBack = returnToHistory
                )
            } else when (selectedTab) {

                BottomTab.Home -> {
                    HomeScreen(
                        historyItems = historyItems,
                        onCameraClick = onCameraClick,
                        onGalleryClick = onGalleryClick,
                        onViewAllClick = {
                            selectedTab = BottomTab.History
                        },
                        onHistoryItemClick = { item ->
                            selectedHomeHistoryItem = item
                        }
                    )
                }

                BottomTab.History -> {
                    HistoryScreen(
                        historyItems = historyItems,
                        onDeleteHistoryItem = onDeleteHistoryItem,
                        onDetailVisibilityChange = { isVisible ->
                            isHistoryDetailVisible = isVisible
                        }
                    )
                }

                else -> Unit
            }
        }
    }
}
