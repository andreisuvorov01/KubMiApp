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
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Precision
import coil.size.Scale
import coil.size.Size
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import java.io.File
import com.google.gson.JsonObject

// Функция логирования для режима отладки
private fun logDebug(hypothesisId: String, message: String, data: Map<String, Any?> = emptyMap()) {
    try {
        val logFile = File("d:\\AndroidProject\\.cursor\\debug.log")
        val entry = JsonObject().apply {
            addProperty("sessionId", "debug-session")
            addProperty("runId", "run-compilation-fix")
            addProperty("hypothesisId", hypothesisId)
            addProperty("location", "CoilImage.kt")
            addProperty("message", message)
            addProperty("timestamp", System.currentTimeMillis())
            val dataObj = JsonObject()
            data.forEach { (k, v) -> dataObj.addProperty(k, v?.toString()) }
            add("data", dataObj)
        }.toString()
        logFile.appendText(entry + "\n")
    } catch (e: Exception) {}
}

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
    var progress by remember(imageUrl, attempt) { mutableFloatStateOf(0f) }

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
        logDebug("H1/H2", "Creating ImageRequest", mapOf("imageUrl" to imageUrl, "attempt" to attempt))
        ImageRequest.Builder(context)
            .data(imageUrl)
            .size(targetSize)
            .precision(Precision.INEXACT)
            .scale(if (contentScale == ContentScale.Crop) Scale.FILL else Scale.FIT)
            .crossfade(true)
            .allowHardware(false)
            .listener(
                onStart = { request ->
                    logDebug("H2", "Loading started", mapOf("url" to request.data.toString()))
                    progress = 0f
                },
                onSuccess = { request, _ ->
                    logDebug("H2", "Loading success", mapOf("url" to request.data.toString()))
                    progress = 1f
                },
                onError = { request, result ->
                    logDebug("H2", "Loading error", mapOf("url" to request.data.toString(), "error" to result.throwable.message))
                }
            )
            .build()
    }

    val painter = rememberAsyncImagePainter(
        model = model
    )

    // Auto-retry with exponential backoff indefinitely.
    LaunchedEffect(painter.state, attempt) {
        if (painter.state is AsyncImagePainter.State.Error) {
            val backoffMs = (1L shl (attempt.coerceAtMost(6))).toInt() * 1000L
            logDebug("H4", "Retrying after error", mapOf("attempt" to attempt, "backoffMs" to backoffMs))
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
                if (progress > 0f) {
                    CircularProgressIndicator(
                        progress = progress,
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            is AsyncImagePainter.State.Error -> {
                // Keep showing progress during infinite retry cycle
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }
            else -> Unit
        }
    }
}
