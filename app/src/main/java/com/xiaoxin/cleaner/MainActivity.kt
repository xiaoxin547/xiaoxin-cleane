package com.xiaoxin.cleaner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            XinCleanerApp()
        }
    }
}

data class GameItem(
    val id: Int,
    val name: String,
    val pattern: String,
    val color: Color
)

@Composable
fun XinCleanerApp() {
    // 定义12款游戏的颜色和匹配规则
    val games = listOf(
        GameItem(1, "三角洲", "tmgp.dfm|delta|dfm", Color(0xFFED1C24)),
        GameItem(2, "王者荣耀", "tmgp.sgame|sgame|wangzhe", Color(0xFF00FF66)),
        GameItem(3, "暗区突围", "com.tencent.mf.uam|anqu", Color(0xFFFFE600)),
        GameItem(4, "香肠派对", "xiangchang|sausage|meta.box", Color(0xFFFF33CC)),
        GameItem(5, "和平精英", "pubgmhd|hpjy|和平精英", Color(0xFF00CCFF)),
        GameItem(6, "失控进化", "rmcn|sikong|shikong|lost", Color(0xFF9933FF)),
        GameItem(7, "迷你世界", "miniworld|迷你世界", Color(0xFFFF9900)),
        GameItem(8, "PUBG Mobile", "tencent.ig|pubgm|PUBG", Color(0xFF3366FF)),
        GameItem(9, "无畏契约", "tmgp.codev|codev", Color(0xFFFFFFFF)),
        GameItem(10, "三角洲台服", "com.garena.game.df", Color(0xFFFF6699)),
        GameItem(11, "CF手游", "com.tencent.tmgp.cf", Color(0xFF00FFCC)),
        GameItem(12, "暗区国际服", "com.proximabeta.mf.uamo", Color(0xFFFFCC33))
    )

    var selectedGame by remember { mutableStateOf<GameItem?>(null) }
    var logs by remember { mutableStateOf("等待选择游戏...") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F0F1A), Color(0xFF1A1A2E))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "小 鑫 清 理",
                color = Color(0xFF00FFFF),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 24.dp)
            )

            // 游戏列表网格
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(games) { game ->
                    GlassCard(
                        game = game,
                        onClick = {
                            selectedGame = game
                            logs = "开始清理 ${game.name}..."
                        }
                    )
                }
            }

            // 底部日志区
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(top = 16.dp)
                    .background(Color(0x44000000), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = logs,
                    color = Color(0xFF00FFCC),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
