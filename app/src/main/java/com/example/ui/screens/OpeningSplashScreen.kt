package com.example.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.language.AppLanguageProvider
import com.example.ui.language.Language
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Animated Opening Splash Screen for Kunjachaya Club.
 * Renders a smooth 70-frame JPEG sequence playback (splash_frame_001 to splash_frame_070)
 * at 30 FPS, followed by a seamless fade-out transition into the application.
 */
@SuppressLint("DiscouragedApi")
@Composable
fun OpeningSplashScreen(
    lang: Language = Language.BN,
    onSplashFinished: () -> Unit
) {
    val context = LocalContext.current
    var currentFrameIndex by remember { mutableIntStateOf(1) }
    var isPlaying by remember { mutableStateOf(true) }
    val fadeAlpha = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    // Pre-cache resource IDs for 70 frames so lookup is 0ms during the loop
    val frameDrawableIds = remember(context) {
        (1..70).map { index ->
            val frameName = String.format("splash_frame_%03d", index)
            context.resources.getIdentifier(frameName, "drawable", context.packageName)
        }
    }

    // Playback loop engine: 30 FPS (~33ms per frame) for ~2.3 seconds total
    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect

        fadeAlpha.snapTo(1f)
        currentFrameIndex = 1

        for (i in 1..70) {
            currentFrameIndex = i
            delay(33) // ~30 FPS
        }

        // Hold final frame briefly before fade out
        delay(400)
        fadeAlpha.animateTo(0f, animationSpec = tween(300))
        onSplashFinished()
    }

    val activeDrawableId = remember(currentFrameIndex, frameDrawableIds) {
        val idx = (currentFrameIndex - 1).coerceIn(0, 69)
        if (frameDrawableIds.isNotEmpty()) frameDrawableIds[idx] else 0
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .alpha(fadeAlpha.value)
            .testTag("opening_splash_screen")
    ) {
        // Frame-by-Frame Image View
        if (activeDrawableId != 0) {
            Image(
                painter = painterResource(id = activeDrawableId),
                contentDescription = "Splash Frame Animation $currentFrameIndex",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Action Controls (Replay & Skip)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Replay Button
            Surface(
                onClick = {
                    scope.launch {
                        isPlaying = false
                        delay(50)
                        isPlaying = true
                    }
                },
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.5f),
                contentColor = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Replay",
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Replay",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Skip Button
            Surface(
                onClick = {
                    scope.launch {
                        fadeAlpha.animateTo(0f, animationSpec = tween(150))
                        onSplashFinished()
                    }
                },
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.5f),
                contentColor = Color.White,
                modifier = Modifier.testTag("skip_splash_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (lang == Language.BN) "এড়িয়ে যান" else "Skip",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Skip",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
