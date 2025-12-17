package com.example.kubmi.ui.components


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale

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
    placeholderSize: Dp = 24.dp
) {
    val fallbackPainter = rememberVectorPainter(Icons.Filled.Image)

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

    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            // Decode smaller bitmaps for faster load + lower memory.
            // Thumbnails across the app shouldn't decode huge 1500px+ images.
            .size(640)
            .precision(Precision.INEXACT)
            .scale(if (contentScale == ContentScale.Crop) Scale.FILL else Scale.FIT)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier.clip(RoundedCornerShape(8.dp)),
        error = fallbackPainter,
        placeholder = fallbackPainter
    )
}