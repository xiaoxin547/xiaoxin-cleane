package com.xiaoxin.cleaner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction

// 全局状态定义
enum class AppScreen { SPLASH, HOME, CONFIRM_CLEAN, SHOW_LOGS, REBOOT_OPTIONS }
enum class RebootChoice { FULL, SOFT, NONE }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var screen by remember { mutableStateOf(AppScreen.SPLASH) }
            var selectedGame by remember { mutableStateOf<GameItem?>(null) }
            var logs by remember { mutableStateOf("") }
            val scope = rememberCoroutineScope()

            XinCleanerApp(
                screen = screen,
                selectedGame = selectedGame,
                logs = logs,
                onAgree = { screen = AppScreen.HOME },
                onGameClick = { game ->
                    selectedGame = game
                    screen = AppScreen.CONFIRM_CLEAN
                },
                onConfirmClean = {
                    screen = AppScreen.SHOW_LOGS
                    logs = "正在初始化...\n"
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            RootShell.execute(CleanScripts.buildScript(selectedGame!!))
                        }
                        logs += result
                        // 延迟一秒让用户看完最后的日志，然后弹出选项
                        kotlinx.coroutines.delay(1000)
                        screen = AppScreen.REBOOT_OPTIONS
                    }
                },
                onCancel = { screen = AppScreen.HOME },
                onReboot = { choice ->
                    when (choice) {
                        RebootChoice.FULL -> {
                            scope.launch(Dispatchers.IO) { RootShell.execute("reboot") }
                        }
                        RebootChoice.SOFT -> {
                            scope.launch(Dispatchers.IO) { 
                                RootShell.execute("setprop ctl.restart zygote || (stop && sleep 2 && start)") 
                            }
                            screen = AppScreen.HOME
                        }
                        RebootChoice.NONE -> screen = AppScreen.HOME
                    }
                }
            )
        }
    }
}

// 全局霓虹色
val neonCyan = Color(0xFF00FFFF)
val neonPink = Color(0xFFFF00FF)
val neonGreen = Color(0xFF00FF88)
val darkBg = Color(0xFF0A0A14)

@Composable
fun XinCleanerApp(
    screen: AppScreen,
    selectedGame: GameItem?,
    logs: String,
    onAgree: () -> Unit,
    onGameClick: (GameItem) -> Unit,
    onConfirmClean: () -> Unit,
    onCancel: () -> Unit,
    onReboot: (RebootChoice) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBg)
            .drawBehind {
                // 绘制赛博朋克背景流光（纯代码实现，无需图片）
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x3300FFFF), Color.Transparent),
                        center = Offset(size.width * 0.2f, size.height * 0.3f),
                        radius = size.width * 0.8f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x33FF00FF), Color.Transparent),
                        center = Offset(size.width * 0.8f, size.height * 0.7f),
                        radius = size.width * 0.8f
                    )
                )
            }
    ) {
        when (screen) {
            AppScreen.SPLASH -> {
                DisclaimDialog(
                    onAgree = onAgree,
                    onDisagree = { /* 退出程序，由调用方处理 */ }
                )
            }
            AppScreen.HOME -> {
                HomeContent(onGameClick = onGameClick)
            }
            AppScreen.CONFIRM_CLEAN -> {
                ConfirmDialog(
                    gameName = selectedGame?.name ?: "",
                    onConfirm = onConfirmClean,
                    onCancel = onCancel
                )
            }
            AppScreen.SHOW_LOGS -> {
                LogDialog(logs = logs)
            }
            AppScreen.REBOOT_OPTIONS -> {
                RebootDialog(onSelect = onReboot)
            }
        }
    }
}
