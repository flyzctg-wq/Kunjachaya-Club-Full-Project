package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Animated Opening Splash Screen for Kunjachaya Club.
 * Recreates the logo reveal sequence:
 * 1. Leaves growing from dark canvas
 * 2. Roof silhouette forming over leaves
 * 3. Green "K" emblem springing into frame
 * 4. Bengali typography "কুঞ্জছায়া ক্লাব" with glowing light sweep
 */
@Composable
fun OpeningSplashScreen(
    onSplashFinished: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var animationState by remember { mutableStateOf(0) } // 0: Leaves, 1: Roof, 2: K emblem, 3: Text & Shimmer

    // Animatable values
    val leafScale = remember { Animatable(0f) }
    val leafAlpha = remember { Animatable(0f) }
    val roofProgress = remember { Animatable(0f) }
    val kScale = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val textOffsetY = remember { Animatable(30f) }

    // Continuous shimmer for text
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    LaunchedEffect(Unit) {
        // Phase 1: Leaves grow (0ms -> 700ms)
        animationState = 0
        leafAlpha.animateTo(1f, animationSpec = tween(400))
        leafScale.animateTo(
            1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )

        // Phase 2: Roof draws (700ms -> 1400ms)
        animationState = 1
        roofProgress.animateTo(1f, animationSpec = tween(700, easing = FastOutSlowInEasing))

        // Phase 3: "K" emblem springs in (1400ms -> 2000ms)
        animationState = 2
        kScale.animateTo(
            1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioHighBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )

        // Phase 4: Text reveals (2000ms -> 2800ms)
        animationState = 3
        textAlpha.animateTo(1f, animationSpec = tween(500))
        textOffsetY.animateTo(0f, animationSpec = tween(500, easing = FastOutSlowInEasing))

        // Wait to finish reveal and auto-advance
        delay(1200)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060B08)) // Deep rich black canvas
            .testTag("opening_splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Main Logo Graphics Canvas
            Box(
                modifier = Modifier.size(280.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val centerX = width / 2f
                    val centerY = height / 2f + 20f

                    // 1. Draw Twin Green Leaves (Bottom center)
                    if (leafAlpha.value > 0f) {
                        val leafPathLeft = Path().apply {
                            moveTo(centerX, centerY + 30f)
                            cubicTo(
                                centerX - 80f * leafScale.value, centerY - 10f,
                                centerX - 90f * leafScale.value, centerY - 80f,
                                centerX, centerY - 30f * leafScale.value
                            )
                            cubicTo(
                                centerX - 20f * leafScale.value, centerY,
                                centerX - 10f * leafScale.value, centerY + 20f,
                                centerX, centerY + 30f
                            )
                        }

                        val leafPathRight = Path().apply {
                            moveTo(centerX, centerY + 30f)
                            cubicTo(
                                centerX + 80f * leafScale.value, centerY - 10f,
                                centerX + 90f * leafScale.value, centerY - 80f,
                                centerX, centerY - 30f * leafScale.value
                            )
                            cubicTo(
                                centerX + 20f * leafScale.value, centerY,
                                centerX + 10f * leafScale.value, centerY + 20f,
                                centerX, centerY + 30f
                            )
                        }

                        val leafGradient = Brush.radialGradient(
                            colors = listOf(Color(0xFF76C843), Color(0xFF2E7D32), Color(0xFF1B5E20)),
                            center = Offset(centerX, centerY),
                            radius = 120f
                        )

                        drawPath(
                            path = leafPathLeft,
                            brush = leafGradient,
                            alpha = leafAlpha.value
                        )
                        drawPath(
                            path = leafPathRight,
                            brush = leafGradient,
                            alpha = leafAlpha.value
                        )

                        // Leaf vein highlights
                        drawPath(
                            path = Path().apply {
                                moveTo(centerX, centerY + 25f)
                                cubicTo(
                                    centerX - 40f * leafScale.value, centerY - 20f,
                                    centerX - 50f * leafScale.value, centerY - 50f,
                                    centerX, centerY - 25f * leafScale.value
                                )
                            },
                            color = Color(0xFFAED581),
                            style = Stroke(width = 3f, cap = StrokeCap.Round),
                            alpha = leafAlpha.value * 0.8f
                        )
                        drawPath(
                            path = Path().apply {
                                moveTo(centerX, centerY + 25f)
                                cubicTo(
                                    centerX + 40f * leafScale.value, centerY - 20f,
                                    centerX + 50f * leafScale.value, centerY - 50f,
                                    centerX, centerY - 25f * leafScale.value
                                )
                            },
                            color = Color(0xFFAED581),
                            style = Stroke(width = 3f, cap = StrokeCap.Round),
                            alpha = leafAlpha.value * 0.8f
                        )
                    }

                    // 2. Draw Orange House Roof Silhouette over leaves
                    if (roofProgress.value > 0f) {
                        val roofPath = Path().apply {
                            // Left eave -> Peak -> Right eave -> Chimney
                            val roofPeakY = centerY - 150f
                            val roofLeftX = centerX - 120f
                            val roofRightX = centerX + 120f
                            val roofBaseY = centerY - 30f

                            moveTo(roofLeftX, roofBaseY)
                            lineTo(
                                roofLeftX + (centerX - roofLeftX) * roofProgress.value,
                                roofBaseY + (roofPeakY - roofBaseY) * roofProgress.value
                            )
                            if (roofProgress.value > 0.5f) {
                                val secondHalfProgress = (roofProgress.value - 0.5f) * 2f
                                lineTo(
                                    centerX + (roofRightX - centerX) * secondHalfProgress,
                                    roofPeakY + (roofBaseY - roofPeakY) * secondHalfProgress
                                )
                            }
                        }

                        // Outer Orange Roof Frame
                        drawPath(
                            path = roofPath,
                            color = Color(0xFFE65100),
                            style = Stroke(width = 16f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        drawPath(
                            path = roofPath,
                            color = Color(0xFFFF9800),
                            style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        // Draw Chimney if roof drawing completed
                        if (roofProgress.value > 0.8f) {
                            val chimneyPath = Path().apply {
                                moveTo(centerX + 65f, centerY - 105f)
                                lineTo(centerX + 65f, centerY - 135f)
                                lineTo(centerX + 80f, centerY - 135f)
                                lineTo(centerX + 80f, centerY - 90f)
                            }
                            drawPath(
                                path = chimneyPath,
                                color = Color(0xFFE65100),
                                style = Stroke(width = 10f, cap = StrokeCap.Square)
                            )
                        }
                    }

                    // 3. Stylized Green "K" inside home peak
                    if (kScale.value > 0f) {
                        val kPath = Path().apply {
                            val kTopY = centerY - 120f
                            val kBottomY = centerY - 30f
                            val kLeftX = centerX - 35f

                            // Vertical bar of K
                            moveTo(kLeftX, kTopY)
                            lineTo(kLeftX, kBottomY)

                            // Top arm
                            moveTo(kLeftX, centerY - 75f)
                            lineTo(centerX + 25f, kTopY + 10f)

                            // Bottom arm with flourish
                            moveTo(kLeftX + 5f, centerY - 80f)
                            lineTo(centerX + 30f, kBottomY - 5f)
                        }

                        drawPath(
                            path = kPath,
                            color = Color(0xFF2E7D32),
                            style = Stroke(width = 20f * kScale.value, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        drawPath(
                            path = kPath,
                            color = Color(0xFF4CAF50),
                            style = Stroke(width = 10f * kScale.value, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Bengali Title "কুঞ্জছায়া ক্লাব" with Golden Light Sweep
            Box(
                modifier = Modifier
                    .offset(y = textOffsetY.value.dp)
                    .alpha(textAlpha.value),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "কুঞ্জছায়া ক্লাব",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = Color(0xFFE8F5E9),
                    letterSpacing = 2.sp
                )

                // Shimmer Overlay
                Text(
                    text = "কুঞ্জছায়া ক্লাব",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = 2.sp,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFFFFD54F).copy(alpha = 0.8f),
                                Color.White,
                                Color(0xFFFFD54F).copy(alpha = 0.8f),
                                Color.Transparent
                            ),
                            start = Offset(shimmerOffset, 0f),
                            end = Offset(shimmerOffset + 200f, 0f)
                        )
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Kunjachaya Club",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF81C784).copy(alpha = textAlpha.value),
                letterSpacing = 3.sp,
                modifier = Modifier.alpha(textAlpha.value)
            )
        }

        // Top-right Skip / Replay Action
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = {
                    // Replay animation
                    scope.launch {
                        leafScale.snapTo(0f)
                        roofProgress.snapTo(0f)
                        kScale.snapTo(0f)
                        textAlpha.snapTo(0f)
                        textOffsetY.snapTo(30f)
                    }
                },
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.12f), CircleShape)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Replay Intro Video",
                    tint = Color.White
                )
            }

            TextButton(
                onClick = onSplashFinished,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = Color.White.copy(alpha = 0.15f),
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("skip_intro_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Skip Intro",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Skip", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
