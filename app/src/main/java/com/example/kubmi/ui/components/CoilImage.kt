package com.example.kubmi.ui.components


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import coil.size.Size
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * A reusable composable for loading and displaying images with Coil.
 * Provides consistent image loading behavior with proper caching and error handling.
 *
 * This implementation follows the official Coil documentation for best practices:
 * https://coil-kt.github.io/coil/compose/
 *
 * @param imageUrl The URL of the image to load
 * @param contentDescription Text used by accessibility services to describe the image
 * @param modifier Modifier to be applied to the image
 * @param contentScale How the image should be scaled
 * @param placeholderSize Size of the placeholder icon when image is loading or failed to load
 */
@Composable
fun CoilImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholderSize: Dp = 24.dp,
    autoRetry: Boolean = true,
    maxAutoRetries: Int = 5
) {
    val fallbackPainter = rememberVectorPainter(Icons.Filled.Image)
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    var attempt by remember(imageUrl) { mutableStateOf(0) }
    var lastError: Throwable? by remember { mutableStateOf(null) }

    fun retry() {
        // Bump attempt to rebuild the request and restart loading.
        attempt++
        lastError = null
    }

    if (imageUrl.isNullOrEmpty()) {
        // Display placeholder when no image URL is provided
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Image,
                contentDescription = contentDescription ?: "Image placeholder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(placeholderSize)
            )
        }
        return
    }

    val targetSize = remember(configuration, density) {
        val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }.roundToInt()
        val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }.roundToInt()
        val width = screenWidthPx.coerceIn(720, 1280)
        val height = (screenHeightPx * 0.6f).roundToInt().coerceIn(480, 960)
        Size(width, height)
    }

    val model = remember(imageUrl, attempt, context, targetSize) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            // Request a capped size to balance clarity and GPU load.
            .size(targetSize)
            .precision(Precision.INEXACT)
            .scale(if (contentScale == ContentScale.Crop) Scale.FILL else Scale.FIT)
            .crossfade(true)
            .build()
    }

    val painter = rememberAsyncImagePainter(
        model = model,
        placeholder = fallbackPainter,
        error = fallbackPainter
    )

    // Auto-retry with exponential backoff until success or max attempts reached.
    LaunchedEffect(painter.state, attempt, autoRetry, maxAutoRetries) {
        if (!autoRetry) return@LaunchedEffect
        if (painter.state is AsyncImagePainter.State.Error && attempt < maxAutoRetries) {
            val backoffMs = (1 shl attempt).coerceAtMost(32) * 500L // 0.5s, 1s, 2s, 4s, 8s, 16s
            delay(backoffMs)
            retry()
        }
    }

    // Track last error to show in UI.
    LaunchedEffect(painter.state) {
        lastError = (painter.state as? AsyncImagePainter.State.Error)?.result?.throwable
    }

    Box(
        modifier = modifier.clip(RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Image(
            painter = painter,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier.matchParentSize()
        )

        when (painter.state) {
            is AsyncImagePainter.State.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp
                )
            }
            is AsyncImagePainter.State.Error -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        text = "Не удалось загрузить изображение",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { retry() },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = "Повторить")
                    }
                    lastError?.message?.takeIf { it.isNotBlank() }?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }
            }
            else -> Unit
        }
    }
}