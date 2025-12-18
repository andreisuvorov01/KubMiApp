package com.example.kubmi

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.example.kubmi.ui.components.CoilImage

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ScreensaverContent(imageUrls: List<String>) {
    var currentTextIndex by remember { mutableStateOf(0) }
    var currentImageIndex by remember { mutableStateOf(0) }

    val texts = listOf("Slide 1", "Slide 2", "Slide 3", "Idle Mode Active")
    val hasImages = imageUrls.isNotEmpty()

    LaunchedEffect(imageUrls) { currentImageIndex = 0 }

    LaunchedEffect(hasImages, imageUrls) {
        while (true) {
            delay(30_000) // Change slide every 30 seconds
            if (hasImages) {
                currentImageIndex = if (imageUrls.isNotEmpty()) {
                    (currentImageIndex + 1) % imageUrls.size
                } else 0
            } else {
                currentTextIndex = (currentTextIndex + 1) % texts.size
            }
        }
    }

    val targetState = if (hasImages && imageUrls.isNotEmpty()) {
        imageUrls[currentImageIndex % imageUrls.size]
    } else {
        texts[currentTextIndex]
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f)) // Solid dark overlay
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = targetState,
            transitionSpec = {
                // Fade in and out animation
                fadeIn(animationSpec = tween(1000, delayMillis = 500)) with
                        fadeOut(animationSpec = tween(1000, delayMillis = 0)) using
                        SizeTransform(clip = false)
            }
        ) { target ->
            if (hasImages && imageUrls.isNotEmpty()) {
                CoilImage(
                    imageUrl = target,
                    contentDescription = "Screensaver image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f))
                )
            } else {
                Text(
                    text = target,
                    color = Color.White,
                    fontSize = 48.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Hint in bottom-right corner
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Text(
                text = "нажмите на экран",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}