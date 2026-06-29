package com.example.kubmi.presentation.screens.news

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.kubmi.R
import com.example.kubmi.domain.model.NewsContentBlock
import com.example.kubmi.ui.components.CoilImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsDetailScreen(
    navController: NavController,
    newsId: String,
    viewModel: NewsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val newsFlow = remember(newsId) { viewModel.observeNewsById(newsId) }
    val newsItem by newsFlow.collectAsState(initial = null)
    val isLoadingDetail by viewModel.detailLoading.collectAsState()
    val detailError by viewModel.detailError.collectAsState()

    // Kick off fetching full article content (blocks + full text).
    LaunchedEffect(newsId) {
        Log.i("KubMI_NewsUI", "NewsDetailScreen: enter newsId=$newsId")
        viewModel.refreshNewsArticle(newsId)
    }

    LaunchedEffect(isLoadingDetail, detailError, newsItem?.contentBlocks?.size, newsItem?.fullText?.length) {
        Log.i(
            "KubMI_NewsUI",
            "NewsDetailScreen: state newsId=$newsId loading=$isLoadingDetail error=${detailError != null} blocks=${newsItem?.contentBlocks?.size ?: -1} fullTextLen=${newsItem?.fullText?.length ?: -1}"
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.news_details)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        newsItem?.let { news ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = news.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = news.date,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Display news cover image if available
                val coverUrl = news.imageUrl
                if (!coverUrl.isNullOrBlank()) {
                    item {
                        CoilImage(
                            imageUrl = coverUrl,
                            contentDescription = stringResource(R.string.news_image_description, news.title),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(MaterialTheme.shapes.medium),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                if (detailError != null) {
                    item {
                        Text(
                            text = detailError ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = { viewModel.refreshNewsArticle(newsId) }) {
                            Text(text = stringResource(R.string.refresh))
                        }
                    }
                } else if (isLoadingDetail && news.contentBlocks.isEmpty() && (news.fullText.isNullOrBlank())) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text(
                                text = stringResource(R.string.loading),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (news.contentBlocks.isNotEmpty()) {
                    items(news.contentBlocks) { block ->
                        when (block.type) {
                            NewsContentBlock.TYPE_TEXT -> {
                                val t = block.text.orEmpty()
                                if (t.isNotBlank()) {
                                    Text(
                                        text = t,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                            NewsContentBlock.TYPE_IMAGE -> {
                                val url = block.imageUrl
                                if (!url.isNullOrBlank()) {
                                    CoilImage(
                                        imageUrl = url,
                                        contentDescription = block.alt ?: news.title,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 180.dp)
                                            .clip(MaterialTheme.shapes.medium),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                            NewsContentBlock.TYPE_VIDEO -> {
                                val url = block.videoUrl
                                if (!url.isNullOrBlank()) {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.video),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = url,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            OutlinedButton(
                                                onClick = {
                                                    context.startActivity(
                                                        Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    )
                                                }
                                            ) {
                                                Text(text = stringResource(R.string.open_video))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    item {
                        // Fallback to plain content/fullText when blocks are not available.
                        val text = news.fullText?.takeIf { it.isNotBlank() } ?: news.content
                        if (text.isBlank() && !isLoadingDetail && detailError == null) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = stringResource(R.string.news_content_unavailable),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val url = news.id.takeIf { it.startsWith("http://") || it.startsWith("https://") }
                                if (!url.isNullOrBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            )
                                        }
                                    ) {
                                        Text(text = stringResource(R.string.open_site))
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        } ?: run {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                when {
                    detailError != null -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = detailError ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(onClick = { viewModel.refreshNewsArticle(newsId) }) {
                                Text(text = stringResource(R.string.refresh))
                            }
                        }
                    }
                    isLoadingDetail -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = stringResource(R.string.loading))
                        }
                    }
                    else -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.news_content_unavailable),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(onClick = { viewModel.refreshNewsArticle(newsId) }) {
                                Text(text = stringResource(R.string.refresh))
                            }
                            val url = newsId.takeIf { it.startsWith("http://") || it.startsWith("https://") }
                            if (!url.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    }
                                ) {
                                    Text(text = stringResource(R.string.open_site))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

