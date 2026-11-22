package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.ViceGold
import com.example.ui.theme.ViceVoid

@Composable
fun CityLoadingScreen(
    progress: Float,
    stageText: String,
    onCancel: () -> Unit,
) {
    BackHandler(onBack = onCancel)

    val clampedProgress = progress.coerceIn(0f, 1f)
    val percent = (clampedProgress * 100f).toInt()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ViceVoid)
            .testTag("city_loading_screen"),
    ) {
        Image(
            painter = painterResource(id = R.drawable.loading_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.width(230.dp),
                color = ViceVoid.copy(alpha = 0.24f),
                border = BorderStroke(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            NeonMagenta.copy(alpha = 0.55f),
                            ElectricCyan.copy(alpha = 0.55f),
                        ),
                    ),
                ),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(
                            color = ElectricCyan.copy(alpha = 0.90f),
                            trackColor = Color.White.copy(alpha = 0.16f),
                            strokeWidth = 2.4.dp,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "$percent%",
                            style = MaterialTheme.typography.labelLarge,
                            color = ViceGold,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    LinearProgressIndicator(
                        progress = { clampedProgress },
                        color = NeonMagenta.copy(alpha = 0.90f),
                        trackColor = Color.White.copy(alpha = 0.20f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .testTag("loading_progress_bar"),
                    )

                    Text(
                        text = stageText,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.92f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
