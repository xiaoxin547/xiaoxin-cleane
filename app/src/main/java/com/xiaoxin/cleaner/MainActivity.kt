package com.xiaoxin.cleaner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

// 全局状态
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
                onDisagree = { finish() }, // 拒绝免责声明直接退出App
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
    onDisagree: () -> Unit,
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
                // 赛博朋克霓虹流光背景
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
            AppScreen.SPLASH -> DisclaimDialog(onAgree = onAgree, onDisagree = onDisagree)
            AppScreen.HOME -> HomeContent(onGameClick = onGameClick)
            AppScreen.CONFIRM_CLEAN -> ConfirmDialog(
                gameName = selectedGame?.name ?: "",
                onConfirm = onConfirmClean,
                onCancel = onCancel
            )
            AppScreen.SHOW_LOGS -> LogDialog(logs = logs)
            AppScreen.REBOOT_OPTIONS -> RebootDialog(onSelect = onReboot)
        }
    }
}

// -------- 1. 免责声明弹窗 --------
@Composable
fun DisclaimDialog(onAgree: () -> Unit, onDisagree: () -> Unit) {
    Dialog(onDismissRequest = {}) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1A1A2E))
                .border(1.dp, neonCyan.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "免责声明",
                    color = neonCyan,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "本脚本仅供学习交流，使用后产生的一切后果（包括账号封禁、设备异常等）由使用者自行承担，作者不承担任何责任。",
                    color = Color.White,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                Text(
                    text = "🐧 965366268",
                    color = neonPink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 拒绝按钮
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x33FF0000))
                            .border(1.dp, Color.Red.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .clickable { onDisagree() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("拒绝并退出", color = Color.White, fontSize = 14.sp)
                    }
                    // 同意按钮
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x3300FF00))
                            .border(1.dp, neonGreen.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .clickable { onAgree() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("同意并进入", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

// -------- 2. 主界面 --------
@Composable
fun HomeContent(onGameClick: (GameItem) -> Unit) {
    val games = listOf(
        GameItem(1, "三角洲", "tmgp.dfm|delta|dfm", Color(0xFFFF3B30)),
        GameItem(2, "王者荣耀", "tmgp.sgame|sgame|wangzhe", Color(0xFF34C759)),
        GameItem(3, "暗区突围", "com.tencent.mf.uam|anqu", Color(0xFFFFCC00)),
        GameItem(4, "香肠派对", "xiangchang|sausage|meta.box", Color(0xFFFF2D55)),
        GameItem(5, "和平精英", "pubgmhd|hpjy|和平精英", Color(0xFF00C7FF)),
        GameItem(6, "失控进化", "rmcn|sikong|shikong|lost", Color(0xFFAF52DE)),
        GameItem(7, "迷你世界", "miniworld|迷你世界", Color(0xFFFF9500)),
        GameItem(8, "PUBG Mobile", "tencent.ig|pubgm|PUBG", Color(0xFF5856D6)),
        GameItem(9, "无畏契约", "tmgp.codev|codev", Color(0xFFFFFFFF)),
        GameItem(10, "三角洲台服", "com.garena.game.df", Color(0xFFFF85A2)),
        GameItem(11, "CF手游", "com.tencent.tmgp.cf", Color(0xFF30D158)),
        GameItem(12, "暗区国际服", "com.proximabeta.mf.uamo", Color(0xFFFFD60A))
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "小 鑫 清 理",
            color = neonCyan,
            fontSize = 34.sp,
            fontWeight = FontWeight.ExtraBold,
            style = androidx.compose.ui.text.TextStyle(
                shadow = Shadow(color = neonCyan, blurRadius = 20f)
            ),
            modifier = Modifier.padding(bottom = 24.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(games) { game ->
                GlassCard(game = game, onClick = { onGameClick(game) })
            }
        }
    }
}

// -------- 3. 确认清理弹窗 --------
@Composable
fun ConfirmDialog(gameName: String, onConfirm: () -> Unit, onCancel: () -> Unit) {
    Dialog(onDismissRequest = { onCancel() }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1A1A2E))
                .border(1.dp, neonCyan.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "提示",
                    color = neonCyan,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "确定要清理「$gameName」吗？\n该操作会重置登录状态和设备标识。",
                    color = Color.White,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x33FFFFFF))
                            .clickable { onCancel() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("否", color = Color.White, fontSize = 14.sp)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x3300FF00))
                            .border(1.dp, neonGreen.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .clickable { onConfirm() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("是", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

// -------- 4. 实时日志弹窗 --------
@Composable
fun LogDialog(logs: String) {
    val scrollState = rememberScrollState()
    LaunchedEffect(logs) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }
    Dialog(onDismissRequest = {}) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0A0A14).copy(alpha = 0.95f))
                .border(1.dp, neonGreen.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "清理进度",
                    color = neonGreen,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        text = logs,
                        color = neonGreen,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

// -------- 5. 结尾重启三选项弹窗 --------
@Composable
fun RebootDialog(onSelect: (RebootChoice) -> Unit) {
    Dialog(onDismissRequest = {}) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1A1A2E))
                .border(1.dp, neonPink.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "清理完成",
                    color = neonGreen,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "请选择重启方式以使设备标识生效",
                    color = Color.White,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                // 选项1：完全重启
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x33FF0000))
                        .border(1.dp, Color.Red.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .clickable { onSelect(RebootChoice.FULL) }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("完全重启（会丢失临时 Root）", color = Color.White, fontSize = 14.sp)
                }

                // 选项2：软重启
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x3300FF00))
                        .border(1.dp, neonGreen.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .clickable { onSelect(RebootChoice.SOFT) }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("软重启（保留临时 Root，推荐）", color = Color.White, fontSize = 14.sp)
                }

                // 选项3：不重启
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x33FFFFFF))
                        .clickable { onSelect(RebootChoice.NONE) }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("不重启，直接退出", color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}
